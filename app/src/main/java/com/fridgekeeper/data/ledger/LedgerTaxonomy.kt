package com.fridgekeeper.data.ledger

import com.fridgekeeper.data.local.LedgerEntryEntity

/**
 * 记账分类、账户、账本等字典，字段口径与 iCost 记账模板保持一致。
 */
object LedgerTaxonomy {

    const val DEFAULT_BOOK = "日常账本"
    const val DEFAULT_CURRENCY = "CNY"
    const val EMPTY_OPTION = "无"

    val EXPENSE_CATEGORIES = listOf(
        "餐饮", "日用", "宠物", "交通", "住房", "服饰", "保险", "汽车", "数码",
        "投资", "社交", "医疗", "通讯", "学习", "卫浴", "家具", "装饰", "家装建材",
        "亏损", "分期本金", "分期利息", "还款手续费", "还贷", "其他支出"
    )

    val INCOME_CATEGORIES = listOf("工资", "投资", "其他收入")

    val TRANSFER_CATEGORIES = listOf("转账", "还款", "充值", "分期创建")

    val SUBCATEGORIES = mapOf(
        "餐饮" to listOf(
            "早餐", "午餐", "晚餐", "生鲜食品", "饮料酒水", "粮油调味",
            "休闲零食", "水果", "蔬菜"
        ),
        "日用" to listOf("日常家居", "美妆", "护肤", "虚拟充值", "娱乐", "罚款赔偿"),
        "宠物" to listOf("猫粮", "猫砂", "猫零食", "猫玩具", "猫用具", "医疗档案"),
        "交通" to listOf("打车", "公交", "地铁", "火车", "飞机", "共享单车", "电动车"),
        "住房" to listOf("房租", "水费", "电费", "燃气费", "物业费", "生活电器", "维修"),
        "服饰" to listOf("服饰运动", "首饰"),
        "保险" to listOf("重疾险", "医疗险", "意外险", "寿险", "养老险"),
        "汽车" to listOf("停车", "加油", "保养", "ETC", "维修"),
        "数码" to listOf("手机数码", "配饰腕表", "家用电器"),
        "投资" to listOf("股市", "红包", "返利", "亏损"),
        "社交" to listOf("请客吃饭", "礼物", "家人"),
        "医疗" to listOf("买药", "滋补保健"),
        "通讯" to listOf("话费充值"),
        "学习" to listOf("书报杂志", "学费"),
        "工资" to listOf("薪资"),
        "家具" to listOf("家具"),
        "卫浴" to listOf("卫浴"),
        "装饰" to listOf("装饰"),
        "家装建材" to listOf("家装建材"),
        "其他支出" to listOf("其他")
    )

    val ACCOUNTS = listOf(
        "现金", "微信", "支付宝", "招商信用卡", "广发信用卡", "中信信用卡",
        "交通信用卡", "招商银行", "建设银行", "网商银行", "美团月付",
        "银河证券", "京东超市卡", "天猫", "其他"
    )

    val BOOKS = listOf("日常账本", "装修账本")

    val CURRENCIES = listOf("CNY", "USD", "HKD")

    val TAG_SUGGESTIONS = listOf("#淘宝", "#京东", "#拼多多", "#小红书", "#抖音")

    fun categoriesFor(type: String): List<String> = when (type) {
        LedgerEntryEntity.TYPE_INCOME -> INCOME_CATEGORIES
        LedgerEntryEntity.TYPE_TRANSFER -> TRANSFER_CATEGORIES
        else -> EXPENSE_CATEGORIES
    }

    fun subcategoriesFor(category: String): List<String> = SUBCATEGORIES[category].orEmpty()

    /** 手动记账时写入模板类型名称 */
    fun templateTypeOf(type: String): String = when (type) {
        LedgerEntryEntity.TYPE_INCOME -> "收入"
        LedgerEntryEntity.TYPE_TRANSFER -> "转账"
        else -> "支出"
    }

    /** 模板「类型」列映射到统计类型，未知类型按金额正负判断 */
    fun resolveType(typeLabel: String, signedAmount: Double): String = when (typeLabel.trim()) {
        "收入", "退款入账", "还款优惠", "转账优惠", "充值优惠" ->
            LedgerEntryEntity.TYPE_INCOME
        "转账", "还款", "充值", "创建消费分期", "创建账单分期" ->
            LedgerEntryEntity.TYPE_TRANSFER
        "支出", "分期本金", "分期利息", "还款手续费", "还贷", "亏损" ->
            LedgerEntryEntity.TYPE_EXPENSE
        else -> if (signedAmount < 0) {
            LedgerEntryEntity.TYPE_EXPENSE
        } else {
            LedgerEntryEntity.TYPE_INCOME
        }
    }

    fun fallbackCategory(type: String): String = when (type) {
        LedgerEntryEntity.TYPE_INCOME -> "其他收入"
        LedgerEntryEntity.TYPE_TRANSFER -> "转账"
        else -> "其他支出"
    }

    /** 兼容历史数据中的旧分类名称 */
    fun normalizeCategory(category: String, type: String): String {
        val name = category.trim()
        if (name.isEmpty()) return fallbackCategory(type)
        return when (name) {
            "购物" -> "日用"
            "居住" -> "住房"
            "娱乐" -> "日用"
            "教育" -> "学习"
            "人情" -> "社交"
            "奖金", "理财", "报销", "退款", "兼职" -> "其他收入"
            else -> name
        }
    }
}
