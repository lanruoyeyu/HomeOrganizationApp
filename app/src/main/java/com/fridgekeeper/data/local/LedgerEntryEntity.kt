package com.fridgekeeper.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 记账条目
 */
@Entity(tableName = "ledger_entries")
data class LedgerEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 类型：EXPENSE 支出 / INCOME 收入 */
    val type: String,
    /** 金额，正数，单位元 */
    val amount: Double,
    /** 分类，如 餐饮、交通、工资 */
    val category: String,
    /** 支付方式：微信/支付宝/银行卡/现金/其他 */
    val paymentMethod: String,
    /** 备注 */
    val note: String,
    /** 发生时间 */
    val occurredAt: Long,
    /** 原始输入（智能解析时保存） */
    val rawInput: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_EXPENSE = "EXPENSE"
        const val TYPE_INCOME = "INCOME"

        val EXPENSE_CATEGORIES = listOf(
            "餐饮", "交通", "购物", "居住", "娱乐", "医疗", "教育", "人情", "其他"
        )
        val INCOME_CATEGORIES = listOf(
            "工资", "奖金", "理财", "报销", "退款", "兼职", "其他"
        )
        val PAYMENT_METHODS = listOf("微信", "支付宝", "银行卡", "现金", "其他")
    }
}
