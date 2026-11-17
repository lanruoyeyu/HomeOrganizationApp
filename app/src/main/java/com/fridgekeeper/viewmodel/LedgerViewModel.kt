package com.fridgekeeper.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fridgekeeper.data.backup.DataBackupManager
import com.fridgekeeper.data.ledger.LedgerExcelManager
import com.fridgekeeper.data.ledger.LedgerTaxonomy
import com.fridgekeeper.data.local.LedgerEntryEntity
import com.fridgekeeper.data.remote.LedgerAiParser
import com.fridgekeeper.data.remote.LedgerLocalParser
import com.fridgekeeper.data.repository.LedgerRepository
import com.fridgekeeper.data.settings.LedgerSettings
import com.fridgekeeper.model.LedgerConfig
import com.fridgekeeper.model.LedgerDraft
import com.fridgekeeper.model.LedgerItemUiState
import com.fridgekeeper.model.LedgerUiState
import com.fridgekeeper.model.ParsedLedgerEntry
import com.fridgekeeper.tassetbook.data.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class BackupUiState(
    val isRunning: Boolean = false,
    val message: String? = null
)

class LedgerViewModel(
    private val repository: LedgerRepository,
    private val aiParser: LedgerAiParser,
    private val settings: LedgerSettings,
    private val backupManager: DataBackupManager,
    private val excelManager: LedgerExcelManager
) : ViewModel() {

    private data class ParseState(
        val isParsing: Boolean = false,
        val items: List<ParsedLedgerEntry> = emptyList(),
        val error: String? = null,
        val hint: String? = null
    )

    private val parseState = MutableStateFlow(ParseState())
    private val _backupState = MutableStateFlow(BackupUiState())
    val backupState: StateFlow<BackupUiState> = _backupState.asStateFlow()

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

    fun addEntry(draft: LedgerDraft) {
        viewModelScope.launch {
            repository.add(
                LedgerEntryEntity(
                    type = draft.type,
                    amount = draft.amount,
                    category = draft.category,
                    subcategory = draft.subcategory,
                    paymentMethod = draft.account,
                    account2 = draft.account2,
                    tags = draft.tags,
                    book = draft.book,
                    typeLabel = draft.typeLabel.ifBlank {
                        LedgerTaxonomy.templateTypeOf(draft.type)
                    },
                    note = draft.note,
                    occurredAt = draft.occurredAt
                )
            )
        }
    }

    fun updateEntry(id: Long, draft: LedgerDraft) {
        viewModelScope.launch {
            val existing = repository.getById(id) ?: return@launch
            repository.update(
                existing.copy(
                    type = draft.type,
                    amount = draft.amount,
                    category = draft.category,
                    subcategory = draft.subcategory,
                    paymentMethod = draft.account,
                    account2 = draft.account2,
                    tags = draft.tags,
                    book = draft.book,
                    typeLabel = draft.typeLabel.ifBlank {
                        LedgerTaxonomy.templateTypeOf(draft.type)
                    },
                    note = draft.note,
                    occurredAt = draft.occurredAt
                )
            )
        }
    }

    /** 从 iCost 模板表格导入记账，保留现有记录并追加 */
    fun importLedgerExcel(source: Uri) {
        if (_backupState.value.isRunning) return
        viewModelScope.launch {
            _backupState.value = BackupUiState(isRunning = true)
            var createdAccounts = 0
            runCatching {
                val result = excelManager.importFrom(source)
                repository.addAll(result.entries)
                // 表格中的账户若在资产中不存在，则自动创建，后续可手动调整
                createdAccounts = AppRepository.get().ensureLedgerAccounts(
                    result.entries.flatMap { listOf(it.paymentMethod, it.account2) }
                )
                result
            }.fold(
                onSuccess = { result ->
                    val skipped = if (result.skippedRows > 0) {
                        "，跳过 ${result.skippedRows} 行"
                    } else {
                        ""
                    }
                    val assetDetail = if (createdAccounts > 0) {
                        "，自动添加 $createdAccounts 个资产"
                    } else {
                        ""
                    }
                    val detail = result.hint?.let { "（$it）" }.orEmpty()
                    _backupState.value = BackupUiState(
                        message = "表格导入完成：新增 ${result.entries.size} 条$skipped$assetDetail$detail"
                    )
                },
                onFailure = { error ->
                    _backupState.value = BackupUiState(
                        message = "表格导入失败：${error.message ?: "未知错误"}"
                    )
                }
            )
        }
    }

    /** 导出空白记账模板 */
    fun exportLedgerTemplate(destination: Uri) {
        if (_backupState.value.isRunning) return
        viewModelScope.launch {
            _backupState.value = BackupUiState(isRunning = true)
            runCatching { excelManager.writeTemplateTo(destination) }.fold(
                onSuccess = {
                    _backupState.value = BackupUiState(message = "记账模板已保存")
                },
                onFailure = { error ->
                    _backupState.value = BackupUiState(
                        message = "模板下载失败：${error.message ?: "未知错误"}"
                    )
                }
            )
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun exportData(destination: Uri) {
        runBackup("导出", destination, backupManager::exportData)
    }

    fun importData(source: Uri) {
        runBackup("导入", source, backupManager::importData)
    }

    fun consumeBackupMessage() {
        _backupState.value = _backupState.value.copy(message = null)
    }

    private fun runBackup(
        action: String,
        uri: Uri,
        operation: suspend (Uri) -> Int
    ) {
        if (_backupState.value.isRunning) return
        viewModelScope.launch {
            _backupState.value = BackupUiState(isRunning = true)
            runCatching { operation(uri) }.fold(
                onSuccess = { count ->
                    _backupState.value = BackupUiState(
                        message = "${action}完成，共 $count 条记录"
                    )
                },
                onFailure = { error ->
                    _backupState.value = BackupUiState(
                        message = "${action}失败：${error.message ?: "未知错误"}"
                    )
                }
            )
        }
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
        subcategory = subcategory,
        paymentMethod = paymentMethod,
        book = LedgerTaxonomy.DEFAULT_BOOK,
        typeLabel = LedgerTaxonomy.templateTypeOf(type),
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
