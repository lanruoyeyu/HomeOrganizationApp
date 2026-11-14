package com.fridgekeeper.data.remote

import com.fridgekeeper.data.local.LedgerEntryEntity
import com.fridgekeeper.data.settings.LedgerSettings
import com.fridgekeeper.model.DateUtils
import com.fridgekeeper.model.ParsedLedgerEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 调用 OpenAI 兼容接口（默认 DeepSeek）把自然语言解析为记账条目
 */
class LedgerAiParser(private val settings: LedgerSettings) {

    suspend fun parse(
        input: String,
        now: Long = System.currentTimeMillis()
    ): List<ParsedLedgerEntry> = withContext(Dispatchers.IO) {
        val today = DateUtils.formatDate(now)
        val payload = JSONObject().apply {
            put("model", settings.model)
            put("temperature", 0)
            put("response_format", JSONObject().put("type", "json_object"))
            put(
                "messages",
                JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", systemPrompt(today)))
                    put(JSONObject().put("role", "user").put("content", input))
                }
            )
        }
        val responseText = post(payload.toString())
        parseResponse(responseText, now)
    }

    private fun systemPrompt(today: String): String = """
        你是记账助手。把用户的一段中文描述解析为记账条目，只输出 JSON，不要输出任何解释。
        输出格式：{"entries":[{"type":"EXPENSE","amount":35.5,"category":"餐饮","paymentMethod":"微信","note":"午饭","date":"$today"}]}
        规则：
        1. type 只能是 EXPENSE（支出）或 INCOME（收入）。
        2. amount 为数字，单位元，必须是正数。
        3. category：支出从 餐饮/交通/购物/居住/娱乐/医疗/教育/人情/其他 中选；收入从 工资/奖金/理财/报销/退款/兼职/其他 中选。
        4. paymentMethod：从 微信/支付宝/银行卡/现金/其他 中选，未提及填 其他。
        5. date：格式 yyyy-MM-dd。今天是 $today，"昨天"就减一天，未提及日期用今天。
        6. 一句话可能包含多笔，请逐条输出；没有金额时输出空数组。
        7. note 用简短中文，不超过 20 字。
    """.trimIndent()

    private fun post(payload: String): String {
        val connection = (URL(buildUrl()).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 40_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer ${settings.apiKey}")
        }
        try {
            connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                throw IllegalStateException("接口返回 $code：${body.take(120)}")
            }
            return body
        } finally {
            connection.disconnect()
        }
    }

    private fun buildUrl(): String {
        val base = settings.baseUrl.trimEnd('/')
        return if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
    }

    private fun parseResponse(response: String, now: Long): List<ParsedLedgerEntry> {
        val content = JSONObject(response)
            .optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content")
            .orEmpty()
        if (content.isBlank()) return emptyList()

        val json = stripCodeFence(content)
        val array = runCatching {
            val obj = JSONObject(json)
            obj.optJSONArray("entries") ?: obj.optJSONArray("items") ?: JSONArray()
        }.getOrElse {
            runCatching { JSONArray(json) }.getOrDefault(JSONArray())
        }

        val result = mutableListOf<ParsedLedgerEntry>()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val amount = item.optDouble("amount", 0.0)
            if (amount <= 0) continue
            val type = if (item.optString("type").uppercase() == LedgerEntryEntity.TYPE_INCOME) {
                LedgerEntryEntity.TYPE_INCOME
            } else {
                LedgerEntryEntity.TYPE_EXPENSE
            }
            val category = item.optString("category").ifBlank { "其他" }
            val note = item.optString("note").ifBlank { category }
            result += ParsedLedgerEntry(
                type = type,
                amount = amount,
                category = category,
                paymentMethod = item.optString("paymentMethod").ifBlank { "其他" },
                note = note.take(30),
                occurredAt = parseDateOrToday(item.optString("date"), now)
            )
        }
        return result
    }

    private fun stripCodeFence(content: String): String {
        var text = content.trim()
        if (text.startsWith("```")) {
            text = text.removePrefix("```json").removePrefix("```")
            if (text.endsWith("```")) text = text.removeSuffix("```")
        }
        return text.trim()
    }

    private fun parseDateOrToday(date: String, now: Long): Long {
        if (date.isBlank()) return startOfDay(now)
        return runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)?.time ?: startOfDay(now)
        }.getOrDefault(startOfDay(now))
    }

    private fun startOfDay(now: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}
