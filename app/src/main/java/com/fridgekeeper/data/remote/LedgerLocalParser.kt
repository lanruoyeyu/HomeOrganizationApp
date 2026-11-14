package com.fridgekeeper.data.remote

import com.fridgekeeper.data.local.LedgerEntryEntity
import com.fridgekeeper.model.ParsedLedgerEntry
import java.util.Calendar
import java.util.Locale

/**
 * 本地规则解析：不依赖网络，作为未配置 API Key 或接口失败时的兜底
 */
object LedgerLocalParser {

    private val amountWithUnit =
        Regex("""(\d+(?:\.\d{1,2})?)\s*(?:元|块钱|块|RMB|rmb|￥|¥)""")
    private val anyNumber = Regex("""\d+(?:\.\d{1,2})?""")
    private val fullDate = Regex("""\d{4}\s*[-/年]\s*\d{1,2}\s*[-/月]\s*\d{1,2}\s*[日号]?""")
    private val monthDay = Regex("""(\d{1,2})\s*月\s*(\d{1,2})\s*[日号]?""")
    private val clockTime = Regex("""\d{1,2}\s*(?:点|时|:|：)\s*\d{0,2}\s*分?""")
    private val dayOfMonth = Regex("""\d{1,2}\s*号""")

    private val incomeKeywords = listOf(
        "工资", "薪水", "发薪", "奖金", "年终奖", "提成", "报销", "退款", "退货",
        "收到", "到账", "收入", "利息", "分红", "收益", "赚了"
    )

    private val expenseCategories = listOf(
        "餐饮" to listOf(
            "吃", "饭", "早餐", "午餐", "晚餐", "夜宵", "外卖", "咖啡", "奶茶",
            "餐", "食堂", "火锅", "烧烤", "零食", "水果", "买菜"
        ),
        "交通" to listOf(
            "打车", "出租车", "滴滴", "地铁", "公交", "车费", "加油", "油费",
            "停车", "高铁", "火车", "机票", "车票"
        ),
        "购物" to listOf("买", "购物", "超市", "淘宝", "京东", "拼多多", "衣服", "鞋", "日用品"),
        "居住" to listOf("房租", "水电", "物业", "燃气", "宽带", "话费", "电费", "水费"),
        "娱乐" to listOf("电影", "游戏", "唱歌", "KTV", "旅游", "健身", "会员"),
        "医疗" to listOf("医院", "看病", "药", "体检", "挂号"),
        "教育" to listOf("学费", "书", "课程", "培训", "考试"),
        "人情" to listOf("红包", "礼金", "请客", "礼物", "份子")
    )

    private val incomeCategories = listOf(
        "工资" to listOf("工资", "薪水", "发薪"),
        "奖金" to listOf("奖金", "年终奖", "提成"),
        "理财" to listOf("理财", "利息", "分红", "收益"),
        "报销" to listOf("报销"),
        "退款" to listOf("退款", "退货"),
        "兼职" to listOf("兼职", "外快")
    )

    private val paymentMethods = listOf(
        "微信" to listOf("微信", "wx", "WeChat"),
        "支付宝" to listOf("支付宝", "花呗"),
        "银行卡" to listOf("银行卡", "信用卡", "储蓄卡", "刷卡"),
        "现金" to listOf("现金")
    )

    fun parse(input: String, now: Long = System.currentTimeMillis()): List<ParsedLedgerEntry> {
        val segments = input
            .split(Regex("""[，,。；;\n]"""))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val results = segments.mapNotNull { parseSegment(it, now) }
        if (results.isNotEmpty()) return results

        // 整句兜底（可能没有标点分隔）
        return parseSegment(input, now)?.let { listOf(it) } ?: emptyList()
    }

    private fun parseSegment(segment: String, now: Long): ParsedLedgerEntry? {
        val amount = findAmount(segment) ?: return null
        if (amount <= 0) return null

        val income = incomeKeywords.any { segment.contains(it, ignoreCase = true) }
        val type = if (income) LedgerEntryEntity.TYPE_INCOME else LedgerEntryEntity.TYPE_EXPENSE
        val category = findCategory(segment, income)
        val payment = paymentMethods.firstOrNull { (_, words) ->
            words.any { segment.contains(it, ignoreCase = true) }
        }?.first ?: "其他"

        return ParsedLedgerEntry(
            type = type,
            amount = amount,
            category = category,
            paymentMethod = payment,
            note = buildNote(segment, category),
            occurredAt = parseDate(segment, now)
        )
    }

    private fun findAmount(text: String): Double? {
        amountWithUnit.find(text)?.let { match ->
            match.groupValues[1].toDoubleOrNull()?.let { return it }
        }
        val cleaned = text
            .replace(fullDate, " ")
            .replace(monthDay, " ")
            .replace(clockTime, " ")
            .replace(dayOfMonth, " ")
        return anyNumber.find(cleaned)?.value?.toDoubleOrNull()
    }

    private fun findCategory(text: String, income: Boolean): String {
        val table = if (income) incomeCategories else expenseCategories
        val strongMatches = mutableListOf<Pair<String, Int>>()
        val weakMatches = mutableListOf<Pair<String, Int>>()
        table.forEach { (category, words) ->
            words.forEach { word ->
                val index = text.indexOf(word)
                if (index >= 0) {
                    // 多字词是强信号（如"咖啡"），单字词是弱信号（如"买"）
                    if (word.length >= 2) {
                        strongMatches += category to index
                    } else {
                        weakMatches += category to index
                    }
                }
            }
        }
        val pool = if (strongMatches.isNotEmpty()) strongMatches else weakMatches
        return pool.minByOrNull { it.second }?.first ?: "其他"
    }

    private fun buildNote(text: String, category: String): String {
        val cleaned = text
            .replace(amountWithUnit, " ")
            .replace(Regex("""花了|花费|付了|付款|消费|支出|买了|收到|入账|到账"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .trim('，', ',', '。', '的')
        return if (cleaned.isBlank()) category else cleaned.take(30)
    }

    private fun parseDate(text: String, now: Long): Long {
        val calendar = Calendar.getInstance(Locale.getDefault()).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        when {
            text.contains("大前天") -> calendar.add(Calendar.DAY_OF_YEAR, -3)
            text.contains("前天") -> calendar.add(Calendar.DAY_OF_YEAR, -2)
            text.contains("昨天") -> calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        monthDay.find(text)?.let { match ->
            val month = match.groupValues[1].toIntOrNull()
            val day = match.groupValues[2].toIntOrNull()
            if (month != null && day != null && month in 1..12 && day in 1..31) {
                calendar.set(Calendar.MONTH, month - 1)
                calendar.set(Calendar.DAY_OF_MONTH, day)
            }
        }
        return calendar.timeInMillis
    }
}
