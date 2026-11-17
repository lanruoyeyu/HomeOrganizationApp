package com.fridgekeeper.model

import com.fridgekeeper.data.ledger.LedgerTaxonomy
import com.fridgekeeper.data.local.LedgerEntryEntity

/**
 * 解析出的记账条目（尚未入账）
 */
data class ParsedLedgerEntry(
    val type: String,
    val amount: Double,
    val category: String,
    val subcategory: String = "",
    val paymentMethod: String,
    val note: String,
    val occurredAt: Long
)

/** 手动记账 / 编辑记账的表单数据 */
data class LedgerDraft(
    val type: String,
    val amount: Double,
    val category: String,
    val subcategory: String = "",
    val account: String = "",
    val account2: String = "",
    val tags: String = "",
    val book: String = LedgerTaxonomy.DEFAULT_BOOK,
    val typeLabel: String = "",
    val note: String = "",
    val occurredAt: Long
)

data class LedgerItemUiState(
    val id: Long,
    val type: String,
    val amount: Double,
    val category: String,
    val subcategory: String,
    val typeLabel: String,
    val paymentMethod: String,
    val account2: String,
    val tags: String,
    val book: String,
    val note: String,
    val occurredAt: Long,
    val rawInput: String
) {
    val isExpense: Boolean get() = type == LedgerEntryEntity.TYPE_EXPENSE
    val isIncome: Boolean get() = type == LedgerEntryEntity.TYPE_INCOME
    val isTransfer: Boolean get() = type == LedgerEntryEntity.TYPE_TRANSFER

    /** 列表右侧展示的收支类型名称 */
    val typeName: String
        get() = typeLabel.ifBlank {
            when (type) {
                LedgerEntryEntity.TYPE_INCOME -> "收入"
                LedgerEntryEntity.TYPE_TRANSFER -> "转账"
                else -> "支出"
            }
        }

    val categoryLabel: String
        get() = if (subcategory.isBlank()) category else "$category · $subcategory"

    companion object {
        fun fromEntity(entity: LedgerEntryEntity): LedgerItemUiState = LedgerItemUiState(
            id = entity.id,
            type = entity.type,
            amount = entity.amount,
            category = entity.category,
            subcategory = entity.subcategory,
            typeLabel = entity.typeLabel,
            paymentMethod = entity.paymentMethod,
            account2 = entity.account2,
            tags = entity.tags,
            book = entity.book,
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
