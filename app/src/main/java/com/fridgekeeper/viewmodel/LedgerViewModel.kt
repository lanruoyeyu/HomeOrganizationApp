package com.fridgekeeper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fridgekeeper.data.local.LedgerEntryEntity
import com.fridgekeeper.data.remote.LedgerAiParser
import com.fridgekeeper.data.remote.LedgerLocalParser
import com.fridgekeeper.data.repository.LedgerRepository
import com.fridgekeeper.data.settings.LedgerSettings
import com.fridgekeeper.model.LedgerConfig
import com.fridgekeeper.model.LedgerItemUiState
import com.fridgekeeper.model.LedgerUiState
import com.fridgekeeper.model.ParsedLedgerEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class LedgerViewModel(
    private val repository: LedgerRepository,
    private val aiParser: LedgerAiParser,
    private val settings: LedgerSettings
) : ViewModel() {

    private data class ParseState(
        val isParsing: Boolean = false,
        val items: List<ParsedLedgerEntry> = emptyList(),
        val error: String? = null,
        val hint: String? = null
    )

    private val parseState = MutableStateFlow(ParseState())

    val uiState: StateFlow<LedgerUiState> = combine(
        repository.observeAll(),
        parseState
    ) { entities, parse ->
        val (monthStart, monthEnd) = currentMonthRange()
        val monthItems = entities.filter { it.occurredAt in monthStart until monthEnd }
        LedgerUiState(
            items = entities.map { LedgerItemUiState.fromEntity(it) },
            monthExpense = monthItems
                .filter { it.type == LedgerEntryEntity.TYPE_EXPENSE }
                .sumOf { it.amount },
            monthIncome = monthItems
                .filter { it.type == LedgerEntryEntity.TYPE_INCOME }
                .sumOf { it.amount },
            isParsing = parse.isParsing,
            parsedItems = parse.items,
            parseError = parse.error,
            parseHint = parse.hint,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LedgerUiState()
    )

    /** 智能解析一段自然语言 */
    fun parseInput(text: String) {
        val input = text.trim()
        if (input.isBlank()) return
        viewModelScope.launch {
            parseState.value = ParseState(isParsing = true)
            if (!settings.isConfigured) {
                val local = LedgerLocalParser.parse(input)
                parseState.value = ParseState(
                    items = local,
                    hint = if (local.isEmpty()) {
                        "未识别到金额，请补充金额后重试"
                    } else {
                        "未配置 API Key，已用本地规则解析"
                    }
                )
                return@launch
            }
            runCatching { aiParser.parse(input) }.fold(
                onSuccess = { parsed ->
                    if (parsed.isEmpty()) {
                        val local = LedgerLocalParser.parse(input)
                        parseState.value = ParseState(
                            items = local,
                            hint = if (local.isEmpty()) {
                                "没有识别到记账内容，请换个说法"
                            } else {
                                "AI 未返回结果，已用本地规则解析"
                            }
                        )
                    } else {
                        parseState.value = ParseState(
                            items = parsed,
                            hint = "AI 解析完成，请确认后入账"
                        )
                    }
                },
                onFailure = { error ->
                    val local = LedgerLocalParser.parse(input)
                    parseState.value = ParseState(
                        items = local,
                        error = "AI 解析失败：${error.message?.take(80) ?: "网络异常"}",
                        hint = if (local.isEmpty()) null else "已自动改用本地规则解析"
                    )
                }
            )
        }
    }

    /** 把解析结果批量入账 */
    fun confirmParsed(rawInput: String) {
        val parsed = parseState.value.items
        if (parsed.isEmpty()) return
        viewModelScope.launch {
            parsed.forEach { item ->
                repository.add(item.toEntity(rawInput))
            }
            parseState.value = ParseState()
        }
    }

    fun removeParsedItem(index: Int) {
        val current = parseState.value.items
        if (index !in current.indices) return
        parseState.value = parseState.value.copy(
            items = current.filterIndexed { i, _ -> i != index }
        )
    }

    fun clearParsed() {
        parseState.value = ParseState()
    }

    fun addEntry(
        type: String,
        amount: Double,
        category: String,
        paymentMethod: String,
        note: String,
        occurredAt: Long
    ) {
        viewModelScope.launch {
            repository.add(
                LedgerEntryEntity(
                    type = type,
                    amount = amount,
                    category = category,
                    paymentMethod = paymentMethod,
                    note = note,
                    occurredAt = occurredAt
                )
            )
        }
    }

    fun updateEntry(
        id: Long,
        type: String,
        amount: Double,
        category: String,
        paymentMethod: String,
        note: String,
        occurredAt: Long
    ) {
        viewModelScope.launch {
            val existing = repository.getById(id) ?: return@launch
            repository.update(
                existing.copy(
                    type = type,
                    amount = amount,
                    category = category,
                    paymentMethod = paymentMethod,
                    note = note,
                    occurredAt = occurredAt
                )
            )
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun loadConfig(): LedgerConfig = LedgerConfig(
        apiKey = settings.apiKey,
        baseUrl = settings.baseUrl,
        model = settings.model
    )

    fun saveConfig(apiKey: String, baseUrl: String, model: String) {
        settings.apiKey = apiKey
        settings.baseUrl = baseUrl
        settings.model = model
    }

    private fun ParsedLedgerEntry.toEntity(rawInput: String) = LedgerEntryEntity(
        type = type,
        amount = amount,
        category = category,
        paymentMethod = paymentMethod,
        note = note,
        occurredAt = occurredAt,
        rawInput = rawInput
    )

    private fun currentMonthRange(now: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        return start to calendar.timeInMillis
    }
}
