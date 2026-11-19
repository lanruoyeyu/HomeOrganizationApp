package com.fridgekeeper.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 记账条目，字段口径与 iCost 记账模板一致
 */
@Entity(tableName = "ledger_entries")
data class LedgerEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 统计类型：EXPENSE 支出 / INCOME 收入 / TRANSFER 转账等不计收支 */
    val type: String,
    /** 金额，正数，单位元 */
    val amount: Double,
    /** 一级分类，如 餐饮、日用、工资 */
    val category: String,
    /** 二级分类，如 午餐、猫粮 */
    @ColumnInfo(defaultValue = "''")
    val subcategory: String = "",
    /** 账户1（模板列名），如 微信、广发信用卡 */
    val paymentMethod: String,
    /** 账户2，转账、还款等场景使用 */
    @ColumnInfo(defaultValue = "''")
    val account2: String = "",
    /** 标签，如 #淘宝 */
    @ColumnInfo(defaultValue = "''")
    val tags: String = "",
    /** 账本，如 日常账本 */
    @ColumnInfo(defaultValue = "'日常账本'")
    val book: String = "日常账本",
    /** 货币 */
    @ColumnInfo(defaultValue = "'CNY'")
    val currency: String = "CNY",
    /** 模板「类型」原始名称，如 支出、收入、转账、分期本金 */
    @ColumnInfo(defaultValue = "''")
    val typeLabel: String = "",
    /** 模板「退款」金额 */
    @ColumnInfo(defaultValue = "0")
    val refund: Double = 0.0,
    /** 模板「优惠」金额 */
    @ColumnInfo(defaultValue = "0")
    val discount: Double = 0.0,
    /** 模板「手续费」金额 */
    @ColumnInfo(defaultValue = "0")
    val fee: Double = 0.0,
    /** 备注 */
    val note: String,
    /** 发生时间 */
    val occurredAt: Long,
    /** 原始输入（智能解析时保存） */
    val rawInput: String = "",
    /** 记账时是否同步加入收纳物品 */
    @ColumnInfo(defaultValue = "0")
    val storedToInventory: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_EXPENSE = "EXPENSE"
        const val TYPE_INCOME = "INCOME"
        const val TYPE_TRANSFER = "TRANSFER"
    }
}
