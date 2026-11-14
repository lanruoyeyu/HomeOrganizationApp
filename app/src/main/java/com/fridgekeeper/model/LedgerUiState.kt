package com.fridgekeeper.model

import com.fridgekeeper.data.local.LedgerEntryEntity

/**
 * 解析出的记账条目（尚未入账）
 */
data class ParsedLedgerEntry(
    val type: String,
    val amount: Double,
    val category: String,
    val paymentMethod: String,
    val note: String,
    val occurredAt: Long
)

data class LedgerItemUiState(
    val id: Long,
    val type: String,
    val amount: Double,
    val category: String,
    val paymentMethod: String,
    val note: String,
    val occurredAt: Long,
    val rawInput: String
) {
    val isExpense: Boolean get() = type == LedgerEntryEntity.TYPE_EXPENSE

    companion object {
        fun fromEntity(entity: LedgerEntryEntity): LedgerItemUiState = LedgerItemUiState(
            id = entity.id,
            type = entity.type,
            amount = entity.amount,
            category = entity.category,
            paymentMethod = entity.paymentMethod,
            note = entity.note,
            occurredAt = entity.occurredAt,
            rawInput = entity.rawInput
        )
    }
}

data class LedgerUiState(
    val items: List<LedgerItemUiState> = emptyList(),
    /** 本月支出合计 */
    val monthExpense: Double = 0.0,
    /** 本月收入合计 */
    val monthIncome: Double = 0.0,
    val isParsing: Boolean = false,
    val parsedItems: List<ParsedLedgerEntry> = emptyList(),
    val parseError: String? = null,
    /** 解析来源提示，如未配置 Key 时走了本地规则 */
    val parseHint: String? = null,
    val isLoading: Boolean = true
) {
    val monthBalance: Double get() = monthIncome - monthExpense
}

/** 大模型接入配置快照 */
data class LedgerConfig(
    val apiKey: String,
    val baseUrl: String,
    val model: String
) {
    val isConfigured: Boolean get() = apiKey.isNotBlank()
}
