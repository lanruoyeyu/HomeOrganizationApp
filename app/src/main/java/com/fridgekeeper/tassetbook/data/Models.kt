package com.fridgekeeper.tassetbook.data

/**
 * 资产（股票）。字段与微信小程序 app.js globalData.assets 对齐。
 * 注意：小程序 addAsset 漏存 market（编辑时才补），Android 版在新增时即保存 market。
 */
data class Asset(
    var id: String = "",
    var name: String = "",
    var market: String? = null,        // A股 / 港股 / ETF
    var exchange: String? = null,      // 沪市 / 深市 / 港交所
    // 底仓
    var baseTotalCost: Double = 0.0,
    var baseShares: Int = 0,
    var baseUnit: Double = 0.0,        // 底仓单价
    // 当前持仓（由交易回放维护）
    var totalCost: Double = 0.0,
    var shares: Int = 0,
    var costPerShare: Double = 0.0,    // 成本价（内部数值，显示 3 位小数）
    // 利润统计
    var totalProfit: Double = 0.0,
    var monthProfit: Double = 0.0,
    var todayProfit: Double = 0.0,
    var currentPrice: Double = 0.0,
    var remark: String? = null,
    var isTop: Boolean = false,
    var hidden: Boolean = false,          // 隐藏后不在股票管理主界面显示
    var order: Long = 0L,                 // 主界面手动排序（越小越靠前；0 为未排序，按存储顺序）
    var createTime: String = "",
    var updateTime: String = ""
)

/**
 * 交易记录。字段与小程序 Trade 对齐；buyXxx、sellXxx 为两侧明细，
 * price、shares、total、commission、transferFee、stampDuty 为兼容旧字段。
 * tradeStatus: buyNoSell(正T买入未卖) / sellNoBuy(反T卖出未补) / buyToSell(买后补卖) / sellToBuy(卖后补买)
 */
data class Trade(
    var id: String = "",
    var assetId: String = "",
    var assetName: String = "",
    var tradeStatus: String = "buyNoSell",
    // 买入侧
    var buyPrice: Double = 0.0,
    var buyShares: Int = 0,
    var buyCommission: Double = 0.0,
    var buyTransferFee: Double = 0.0,
    var buyTotalFee: Double = 0.0,
    var buyTotal: Double = 0.0,
    var buyTradeDate: String = "",
    // 卖出侧
    var sellPrice: Double = 0.0,
    var sellShares: Int = 0,
    var sellCommission: Double = 0.0,
    var sellTransferFee: Double = 0.0,
    var sellStampDuty: Double = 0.0,
    var sellTotalFee: Double = 0.0,
    var sellTotal: Double = 0.0,        // 卖出净额（扣费后）
    var sellTradeDate: String = "",
    // 兼容旧字段
    var price: Double = 0.0,
    var shares: Int = 0,
    var total: Double = 0.0,
    var commission: Double = 0.0,
    var transferFee: Double = 0.0,
    var stampDuty: Double = 0.0,
    // 通用
    var profit: Double = 0.0,
    var remark: String = "",
    var tradeDate: String = "",
    var tradeTime: String = "",
    var relatedBuyTradeId: String? = null,
    var relatedSellTradeId: String? = null,
    // 融资标记：买入侧 isMarginBuy=true 表示融资买入；卖出侧 isMarginRepay=true 表示卖券还款
    var isMarginBuy: Boolean = false,
    var isMarginRepay: Boolean = false,
    var updateTime: String = ""
)

/**
 * 资产页账户（现金/储蓄卡/信用卡/公交卡/基金等）。
 * balance：资产/充值/理财账户为余额（计正）；信用账户为欠款金额（计负）。
 */
data class Account(
    var id: String = "",
    var category: String = "",       // AccountTypes.CAT_*
    var type: String = "",           // 现金 / 储蓄卡 / 信用卡 / ...
    var name: String = "",           // 用户自定义名称，默认与 type 相同
    var balance: Double = 0.0,
    var remark: String? = null,
    var ledgerSelectable: Boolean? = null,   // 记账时是否可被选择；null 视为默认开启
    var billingDay: Int? = null,             // 信用账户账单日（1-31）
    var repaymentDay: Int? = null,           // 信用账户还款日（1-31）
    var order: Long = 0L,
    var createTime: String = "",
    var updateTime: String = ""
)

/** 账户类别与预设类型。股票账户为系统项（自动生成，金额=持仓市值），不在手动添加列表中。 */
object AccountTypes {
    const val CAT_ASSET = "asset"       // 资产账户
    const val CAT_CREDIT = "credit"     // 信用账户
    const val CAT_PREPAID = "prepaid"   // 充值账户
    const val CAT_INVEST = "invest"     // 理财账户

    const val TYPE_STOCK = "股票"
    const val TYPE_LOAN = "贷款"

    data class Category(val key: String, val label: String, val types: List<String>)

    val categories = listOf(
        Category(CAT_ASSET, "资产账户", listOf(
            "现金钱包", "储蓄卡", "支付宝", "余额宝", "余利宝", "小荷包", "蚂蚁财富",
            "微信钱包", "微信零钱通", "腾讯理财通", "QQ钱包", "抖音钱包", "多多钱包",
            "美团钱包", "饿了么钱包", "飞猪钱包", "澳门钱包", "数字人民币", "京东金融",
            "京东小金库", "云闪付", "LINE Pay", "PayPay", "PayPal", "PayMe", "Paga",
            "Chipper Cash", "YouTrip", "TNG eWallet", "UseTimon", "公积金", "医保卡",
            "养老金", "存折", "其他资产"
        )),
        Category(CAT_CREDIT, "信用账户", listOf(
            "信用卡", "贷款", "蚂蚁花呗", "京东白条", "京东金条", "借呗", "微粒贷",
            "美团借钱", "美团月付", "抖音月付", "微信分付", "备用金", "亲情账户", "其他信用"
        )),
        Category(CAT_PREPAID, "充值账户", listOf(
            "公交卡", "饭卡", "话费", "会员卡", "加油卡", "押金", "Apple ID", "八达通",
            "澳门通", "Suica", "ICOCA", "PASMO", "WOWPASS", "Tmoney", "天猫超市卡",
            "京东超市卡", "京东E卡", "其他充值卡"
        )),
        Category(CAT_INVEST, "理财账户", listOf(
            "基金", "黄金", "期货", "保险", "数字货币", "定期存款", "其他理财"
        ))
    )

    fun categoryLabel(key: String): String =
        categories.find { it.key == key }?.label ?: key

    fun typesOf(category: String): List<String> =
        categories.find { it.key == category }?.types ?: emptyList()

    /** 类别金额是否计正（信用账户计负，其余计正） */
    fun isPositive(category: String): Boolean = category != CAT_CREDIT
}

/**
 * 设置。费率单位为万分之一（‱）：tradeRateA=0.85 表示 0.85‱=0.0085%。
 */
data class Settings(
    var theme: String = "light",
    var defaultMarket: String = "A股",
    var defaultExchange: String = "沪市",
    var tradeRateTransfer: String = "0.1",   // 过户费 ‱
    var tradeRateStampDuty: String = "5",    // 印花税 ‱（仅卖出）
    var tradeRateA: Double? = 0.85,          // A股佣金 ‱；null 时按 0.000085
    var tradeRateHK: Double? = 1.00,
    var tradeRateETF: Double? = 0.20,
    // 融资年利率（%），默认 3.98%
    var marginRate: Double = 3.98
)
