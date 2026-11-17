package com.fridgekeeper.data.ledger

import com.fridgekeeper.data.local.LedgerEntryEntity
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/**
 * 把 iCost 模板表格解析为记账条目。
 */
object LedgerXlsxImporter {

    data class Result(
        val entries: List<LedgerEntryEntity>,
        val skippedRows: Int,
        val hint: String?
    )

    private val DATE_PATTERNS = listOf(
        "yyyy/M/d H:mm:ss",
        "yyyy/M/d H:mm",
        "yyyy-M-d H:mm:ss",
        "yyyy-M-d H:mm",
        "yyyy.M.d H:mm:ss",
        "yyyy.M.d H:mm",
        "yyyy年M月d日 H:mm:ss",
        "yyyy年M月d日 H:mm",
        "yyyy/M/d",
        "yyyy-M-d",
        "yyyy.M.d",
        "yyyy年M月d日"
    )

    fun parse(rows: List<List<String>>): Result {
        if (rows.isEmpty()) throw IllegalArgumentException("表格内容为空")

        val headerIndex = rows.take(20).indexOfFirst { row ->
            row.any { it.trim() == "日期" } && row.any { it.trim() == "金额" }
        }
        if (headerIndex < 0) {
            throw IllegalArgumentException("没有找到「日期 / 金额」表头，请使用记账模板")
        }

        val headers = rows[headerIndex].map { it.trim() }
        fun columnOf(name: String): Int = headers.indexOf(name)

        val dateColumn = columnOf("日期")
        val typeColumn = columnOf("类型")
        val amountColumn = columnOf("金额")
        val categoryColumn = columnOf("一级分类")
        val subcategoryColumn = columnOf("二级分类")
        val accountColumn = columnOf("账户1")
        val account2Column = columnOf("账户2")
        val noteColumn = columnOf("备注")
        val currencyColumn = columnOf("货币")
        val tagsColumn = columnOf("标签")
        val bookColumn = columnOf("账本")
        val refundColumn = columnOf("退款")
        val discountColumn = columnOf("优惠")
        val feeColumn = columnOf("手续费")

        val entries = ArrayList<LedgerEntryEntity>()
        var skippedRows = 0
        val reasons = ArrayList<String>()

        for (rowIndex in (headerIndex + 1) until rows.size) {
            val row = rows[rowIndex]
            if (row.all { it.isBlank() }) continue

            val signedAmount = parseAmount(cell(row, amountColumn))
            val occurredAt = parseDate(cell(row, dateColumn))
            if (signedAmount == null || occurredAt == null) {
                skippedRows++
                if (reasons.size < 3) {
                    reasons += "第 ${rowIndex + 1} 行" +
                        if (signedAmount == null) "金额无法识别" else "日期无法识别"
                }
                continue
            }

            val typeLabel = cell(row, typeColumn)
            val type = LedgerTaxonomy.resolveType(typeLabel, signedAmount)
            val category = LedgerTaxonomy.normalizeCategory(
                cell(row, categoryColumn),
                type
            )

            entries += LedgerEntryEntity(
                type = type,
                amount = abs(signedAmount),
                category = category,
                subcategory = cell(row, subcategoryColumn),
                paymentMethod = cell(row, accountColumn),
                account2 = cell(row, account2Column),
                tags = cell(row, tagsColumn),
                book = cell(row, bookColumn).ifBlank { LedgerTaxonomy.DEFAULT_BOOK },
                currency = cell(row, currencyColumn).ifBlank { LedgerTaxonomy.DEFAULT_CURRENCY },
                typeLabel = typeLabel.ifBlank { LedgerTaxonomy.templateTypeOf(type) },
                refund = parseAmount(cell(row, refundColumn))?.let { abs(it) } ?: 0.0,
                discount = parseAmount(cell(row, discountColumn))?.let { abs(it) } ?: 0.0,
                fee = parseAmount(cell(row, feeColumn))?.let { abs(it) } ?: 0.0,
                note = cell(row, noteColumn),
                occurredAt = occurredAt,
                rawInput = "iCost表格导入"
            )
        }

        if (entries.isEmpty()) {
            throw IllegalArgumentException(
                if (reasons.isEmpty()) "表格中没有可导入的记账数据"
                else "没有可导入的数据：${reasons.joinToString("；")}"
            )
        }

        return Result(
            entries = entries,
            skippedRows = skippedRows,
            hint = reasons.takeIf { it.isNotEmpty() }?.joinToString("；")
        )
    }

    private fun cell(row: List<String>, index: Int): String =
        if (index in row.indices) row[index].trim() else ""

    private fun parseAmount(text: String): Double? {
        var value = text.trim()
        if (value.isEmpty()) return null
        val negative = value.startsWith("-") || (value.startsWith("(") && value.endsWith(")"))
        value = value
            .replace(",", "")
            .replace("，", "")
            .replace("¥", "")
            .replace("￥", "")
            .replace("元", "")
            .replace(" ", "")
            .replace("(", "")
            .replace(")", "")
        val number = value.toDoubleOrNull() ?: return null
        return if (negative && number > 0) -number else number
    }

    private fun parseDate(text: String): Long? {
        val value = text.trim()
        if (value.isEmpty()) return null

        DATE_PATTERNS.forEach { pattern ->
            val format = SimpleDateFormat(pattern, Locale.CHINA).apply { isLenient = false }
            val position = ParsePosition(0)
            val date = format.parse(value, position)
            if (date != null && position.index == value.length) return date.time
        }

        // Excel 日期序列号
        val serial = value.toDoubleOrNull()
        if (serial != null && serial > 0) {
            val utcMillis = ((serial - 25569.0) * 86_400_000.0).toLong()
            return utcMillis - TimeZone.getDefault().getOffset(utcMillis)
        }
        return null
    }
}
