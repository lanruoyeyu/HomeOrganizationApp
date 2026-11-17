package com.fridgekeeper.tassetbook.data

import com.fridgekeeper.tassetbook.domain.Backup
import com.fridgekeeper.tassetbook.domain.Fees
import com.fridgekeeper.tassetbook.domain.RealizedProfit
import com.fridgekeeper.tassetbook.util.Dates
import com.fridgekeeper.tassetbook.util.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.time.LocalDate

/**
 * 全局数据仓库，对齐小程序 app.js 的 globalData 与全部业务方法。
 */
class AppRepository(private val store: LocalStore) {

    val assets = mutableListOf<Asset>()
    val trades = mutableListOf<Trade>()
    val accounts = mutableListOf<Account>()
    var settings: Settings = Settings()
        private set

    private val _assetsFlow = MutableStateFlow<List<Asset>>(emptyList())
    val assetsFlow: StateFlow<List<Asset>> = _assetsFlow.asStateFlow()
    private val _tradesFlow = MutableStateFlow<List<Trade>>(emptyList())
    val tradesFlow: StateFlow<List<Trade>> = _tradesFlow.asStateFlow()
    private val _settingsFlow = MutableStateFlow(settings)
    val settingsFlow: StateFlow<Settings> = _settingsFlow.asStateFlow()
    private val _accountsFlow = MutableStateFlow<List<Account>>(emptyList())
    val accountsFlow: StateFlow<List<Account>> = _accountsFlow.asStateFlow()

    data class OpResult(val success: Boolean, val message: String = "", val trade: Trade? = null, val profit: Double? = null)

    data class Position(val totalCost: Double, val shares: Int, val costPerShare: Double, val oversold: Boolean = false, val minShares: Int = 0)

    data class Stats(
        val totalAssets: Int,
        val totalProfit: Double,
        val totalCost: Double,
        val totalShares: Int,
        val aStock: Int,
        val hkStock: Int,
        val etf: Int,
        val totalTrades: Int,
        val buyCount: Int,
        val sellCount: Int
    )

    fun load() {
        assets.clear()
        assets.addAll(store.loadAssets().filter { it.name.isNotBlank() })
        trades.clear()
        trades.addAll(store.loadTrades().filter { it.assetId.isNotBlank() })
        accounts.clear()
        accounts.addAll(store.loadAccounts().filter { it.type.isNotBlank() })
        settings = store.loadSettings()
        migrateLegacyBase()
        recomputeAll()
        save()   // 用当前口径校正历史版本写入的派生字段（持仓/利润）
        publish()
    }

    /**
     * 历史数据迁移：底仓股数为0时底仓单价无意义，清零避免脏数据。
     */
    private fun migrateLegacyBase() {
        for (a in assets) {
            if (a.baseShares == 0 && a.baseTotalCost > 0) {
                a.baseUnit = 0.0
            }
        }
    }

    fun save() {
        store.saveAssets(assets)
        store.saveTrades(trades)
        store.saveSettings(settings)
        store.saveAccounts(accounts)
    }

    private fun publish() {
        // 必须发射元素拷贝：assets/trades 内的对象常被原地修改（如 hidden），
        // 若只发 toList()，StateFlow 逐元素 equals 比较会认为值未变化而跳过发射，
        // 导致当前页面状态（隐藏/显示徽标等）不能即时刷新。
        // 按用户拖动顺序（order）发射；未排序资产 order=0，稳定排序保持原有存储顺序
        _assetsFlow.value = assets.sortedWith(compareBy { it.order }).map { it.copy() }
        _tradesFlow.value = trades.map { it.copy() }
        _settingsFlow.value = settings.copy()
        _accountsFlow.value = accounts.sortedWith(compareBy { it.order }).map { it.copy() }
    }

    // ---------------- 资产 ----------------

    fun addAsset(name: String, market: String, exchange: String, baseTotalCost: Double, baseShares: Int): Asset {
        val now = Dates.nowString()
        val asset = Asset(
            id = System.currentTimeMillis().toString(),
            name = name,
            market = market,
            exchange = exchange,
            baseTotalCost = baseTotalCost,
            baseShares = baseShares,
            baseUnit = if (baseShares > 0 && baseTotalCost > 0) baseTotalCost / baseShares else 0.0,
            totalCost = baseTotalCost,
            shares = baseShares,
            createTime = now,
            updateTime = now,
            costPerShare = if (baseShares > 0) Money.round6(baseTotalCost / baseShares) else 0.0,
            order = (assets.minOfOrNull { it.order } ?: 0L) - 1L
        )
        assets.add(0, asset)
        recomputeAll()
        save()
        publish()
        return asset
    }

    /** 添加/编辑资产页保存（编辑模式） */
    fun saveAsset(id: String?, name: String, market: String, exchange: String, baseTotalCost: Double, baseShares: Int, baseUnit: Double) {
        if (id == null) {
            addAsset(name, market, exchange, baseTotalCost, baseShares)
            return
        }
        val index = assets.indexOfFirst { it.id == id }
        if (index != -1) {
            val a = assets[index]
            a.name = name
            a.market = market
            a.exchange = exchange
            a.baseTotalCost = baseTotalCost
            a.baseShares = baseShares
            a.baseUnit = if (baseShares > 0 && baseTotalCost > 0) baseTotalCost / baseShares else 0.0
            a.updateTime = Dates.nowString()
            recomputeAll()
            save()
            publish()
        }
    }

    fun deleteAsset(id: String) {
        assets.removeAll { it.id == id }
        trades.removeAll { it.assetId == id }
        recomputeAll()
        save()
        publish()
    }

    fun toggleTop(id: String) {
        val a = assets.find { it.id == id } ?: return
        a.isTop = !a.isTop
        save()
        publish()
    }

    /**
     * 拖动排序：orderedVisibleIds 为当前可见列表的目标顺序（可能是筛选后的子集）。
     * 与全部资产做稳定归并——不可见资产保持其原有相对位置，可见资产占据它们原来的槽位，
     * 最后全局重排 order。
     */
    fun reorderAssets(orderedVisibleIds: List<String>) {
        val visibleSet = orderedVisibleIds.toSet()
        val byId = assets.associateBy { it.id }
        val visible = orderedVisibleIds.mapNotNull { byId[it] }
        if (visible.size < 2) return
        var vi = 0
        val merged = assets.sortedWith(compareBy { it.order }).map { a ->
            if (a.id in visibleSet && vi < visible.size) visible[vi++] else a
        }
        merged.forEachIndexed { i, a -> a.order = (i + 1).toLong() }
        save()
        publish()
    }

    /** 设置资产隐藏/显示（隐藏后不在股票管理主界面显示） */
    fun setAssetHidden(id: String, hidden: Boolean) {
        val a = assets.find { it.id == id } ?: return
        a.hidden = hidden
        a.updateTime = Dates.nowString()
        save()
        publish()
    }

    fun getAsset(id: String): Asset? = assets.find { it.id == id }

    // ---------------- 资产页账户 ----------------

    fun getAccount(id: String): Account? = accounts.find { it.id == id }

    fun addAccount(
        category: String,
        type: String,
        name: String,
        balance: Double,
        remark: String,
        ledgerSelectable: Boolean? = null,
        billingDay: Int? = null,
        repaymentDay: Int? = null
    ): Account {
        val now = Dates.nowString()
        val account = Account(
            id = System.currentTimeMillis().toString(),
            category = category,
            type = type,
            name = name.ifBlank { type },
            balance = Money.round2(balance),
            remark = remark.ifBlank { null },
            ledgerSelectable = ledgerSelectable,
            billingDay = billingDay,
            repaymentDay = repaymentDay,
            order = (accounts.minOfOrNull { it.order } ?: 0L) - 1L,
            createTime = now,
            updateTime = now
        )
        accounts.add(0, account)
        save()
        publish()
        return account
    }

    fun updateAccount(
        id: String,
        category: String,
        type: String,
        name: String,
        balance: Double,
        remark: String,
        ledgerSelectable: Boolean? = null,
        billingDay: Int? = null,
        repaymentDay: Int? = null
    ) {
        val a = accounts.find { it.id == id } ?: return
        a.category = category
        a.type = type
        a.name = name.ifBlank { type }
        a.balance = Money.round2(balance)
        a.remark = remark.ifBlank { null }
        a.ledgerSelectable = ledgerSelectable
        a.billingDay = billingDay
        a.repaymentDay = repaymentDay
        a.updateTime = Dates.nowString()
        save()
        publish()
    }

    /** 记账时可选择的账户名称（贷款、股票系统项及手动关闭的账户除外） */
    fun ledgerSelectableAccountNames(): List<String> =
        accounts
            .filter { it.type != AccountTypes.TYPE_LOAN && it.ledgerSelectable != false }
            .map { it.name }
            .distinct()

    /** 导入记账时按表格中的账户名称自动补齐资产账户，返回新增数量 */
    fun ensureLedgerAccounts(names: Collection<String>): Int {
        val existing = accounts.map { it.name.trim() }.toMutableSet()
        var created = 0
        names.map { it.trim() }
            .filter { it.isNotEmpty() && it !in existing }
            .distinct()
            .forEach { name ->
                val (category, type) = guessAccountType(name)
                addAccount(
                    category = category,
                    type = type,
                    name = name,
                    balance = 0.0,
                    remark = "记账导入自动创建",
                    ledgerSelectable = true
                )
                existing.add(name)
                created++
            }
        return created
    }

    /** 根据账户名称推测资产类别与类型 */
    private fun guessAccountType(name: String): Pair<String, String> = when {
        name.contains("信用卡") -> AccountTypes.CAT_CREDIT to "信用卡"
        name.contains("贷款") -> AccountTypes.CAT_CREDIT to AccountTypes.TYPE_LOAN
        name.contains("花呗") -> AccountTypes.CAT_CREDIT to "蚂蚁花呗"
        name.contains("白条") -> AccountTypes.CAT_CREDIT to "京东白条"
        name.contains("金条") -> AccountTypes.CAT_CREDIT to "京东金条"
        name.contains("借呗") -> AccountTypes.CAT_CREDIT to "借呗"
        name.contains("微粒贷") -> AccountTypes.CAT_CREDIT to "微粒贷"
        name.contains("月付") || name.contains("分付") ->
            AccountTypes.CAT_CREDIT to "其他信用"
        name.contains("微信") -> AccountTypes.CAT_ASSET to "微信钱包"
        name.contains("支付宝") -> AccountTypes.CAT_ASSET to "支付宝"
        name.contains("余额宝") -> AccountTypes.CAT_ASSET to "余额宝"
        name.contains("余利宝") -> AccountTypes.CAT_ASSET to "余利宝"
        name.contains("零钱通") -> AccountTypes.CAT_ASSET to "微信零钱通"
        name.contains("现金") -> AccountTypes.CAT_ASSET to "现金钱包"
        name.contains("银行") || name.contains("储蓄") || name.contains("借记") ->
            AccountTypes.CAT_ASSET to "储蓄卡"
        name.contains("公交") || name.contains("交通") ->
            AccountTypes.CAT_PREPAID to "公交卡"
        name.contains("基金") -> AccountTypes.CAT_INVEST to "基金"
        name.contains("黄金") -> AccountTypes.CAT_INVEST to "黄金"
        name.contains("理财") -> AccountTypes.CAT_INVEST to "其他理财"
        else -> AccountTypes.CAT_ASSET to "其他资产"
    }

    fun deleteAccount(id: String) {
        accounts.removeAll { it.id == id }
        save()
        publish()
    }

    /** 各类别（不含股票系统项）的金额合计：信用账户返回正数欠款合计，由调用方决定加减 */
    fun sumAccountsByCategory(category: String): Double =
        Money.round2(accounts.filter { it.category == category }.sumOf { it.balance })

    // ---------------- 交易：新建正T买入 / 反T卖出 ----------------

    fun addTBuy(assetId: String, price: Double, shares: Int, remark: String, tradeDate: String, isMarginBuy: Boolean = false): OpResult {
        val asset = assets.find { it.id == assetId } ?: return OpResult(false, "资产不存在")
        val (f, _) = Fees.buyTotal(price, shares, settings)
        val buyTotal = Money.round2(price * shares + f.commission + f.transferFee)
        val date = tradeDate.ifBlank { Dates.todayLocale() }
        val now = Dates.nowString()

        val t = Trade(
            id = System.currentTimeMillis().toString(),
            assetId = assetId,
            assetName = asset.name,
            buyPrice = price,
            buyShares = shares,
            buyCommission = f.commission,
            buyTransferFee = f.transferFee,
            buyTotalFee = Money.round2(f.commission + f.transferFee),
            buyTotal = buyTotal,
            buyTradeDate = date,
            sellPrice = 0.0, sellShares = 0, sellCommission = 0.0, sellTransferFee = 0.0,
            sellStampDuty = 0.0, sellTotalFee = 0.0, sellTotal = 0.0, sellTradeDate = "",
            price = price,
            shares = shares,
            total = buyTotal,
            commission = f.commission,
            transferFee = f.transferFee,
            stampDuty = 0.0,
            profit = 0.0,
            remark = remark,
            tradeDate = date,
            tradeTime = now,
            tradeStatus = "buyNoSell",
            isMarginBuy = isMarginBuy
        )

        asset.totalCost = Money.round6(asset.totalCost + buyTotal)
        asset.shares = asset.shares + shares
        asset.costPerShare = if (asset.shares > 0) Money.round6(asset.totalCost / asset.shares) else asset.costPerShare

        trades.add(0, t)
        recomputeProfits()
        save()
        publish()
        return OpResult(true, trade = t)
    }

    fun addTSell(assetId: String, price: Double, shares: Int, remark: String, tradeDate: String, isMarginRepay: Boolean = false): OpResult {
        val asset = assets.find { it.id == assetId } ?: return OpResult(false, "资产不存在")

        val (f, net) = Fees.sellNet(price, shares, settings)
        val grossProfit = (price - asset.costPerShare) * shares
        val netProfit = Money.round2(grossProfit - f.totalFee)
        val date = tradeDate.ifBlank { Dates.todayLocale() }
        val now = Dates.nowString()

        // 按日期回放校验：补录历史卖单时，当前持仓足够不代表该历史时点足够
        val probe = Trade(
            id = "__probe__", assetId = assetId, assetName = asset.name,
            sellPrice = price, sellShares = shares, sellTotal = net,
            sellTradeDate = date, tradeDate = date, tradeStatus = "sellNoBuy"
        )
        if (!validateNoOversell(asset, extra = probe)) return OpResult(false, "卖出股数超过该时点持仓")

        val t = Trade(
            id = System.currentTimeMillis().toString(),
            assetId = assetId,
            assetName = asset.name,
            buyPrice = 0.0, buyShares = 0, buyCommission = 0.0, buyTransferFee = 0.0,
            buyTotalFee = 0.0, buyTotal = 0.0, buyTradeDate = "",
            sellPrice = price,
            sellShares = shares,
            sellCommission = f.commission,
            sellTransferFee = f.transferFee,
            sellStampDuty = f.stampDuty,
            sellTotalFee = f.totalFee,
            sellTotal = net,
            sellTradeDate = date,
            price = price,
            shares = shares,
            total = net,
            commission = f.commission,
            transferFee = f.transferFee,
            stampDuty = f.stampDuty,
            profit = netProfit,
            remark = remark,
            tradeDate = date,
            tradeTime = now,
            tradeStatus = "sellNoBuy",
            isMarginRepay = isMarginRepay
        )

        val sharesAfter = asset.shares - shares
        // 独立卖出（减持）：按净额冲减成本，卖出费用留在剩余持仓成本
        val costAfter = Money.round6(asset.totalCost - net)
        asset.totalCost = costAfter
        asset.shares = sharesAfter
        asset.costPerShare = if (sharesAfter > 0)
            Money.round6(costAfter / sharesAfter) else 0.0

        trades.add(0, t)
        recomputeProfits()
        save()
        publish()
        return OpResult(true, trade = t, profit = netProfit)
    }

    // ---------------- 交易：补单 / 编辑（对齐 trade-operation.js） ----------------

    /** 补充卖出：buyNoSell → buyToSell */
    fun supplementSell(tradeId: String, price: Double, shares: Int, sellDate: String, remark: String, isMarginRepay: Boolean = false): OpResult {
        val idx = trades.indexOfFirst { it.id == tradeId }
        if (idx == -1) return OpResult(false, "交易记录不存在")
        val t = trades[idx]
        val (f, net) = Fees.sellNet(price, shares, settings)
        val date = sellDate.ifBlank { Dates.todayLocale() }
        // tradeDate 取买入/卖出较晚者，保证利润归属日正确
        val tradeDate = Dates.profitDate(null, t.buyTradeDate, date)?.let { Dates.formatCn(it) } ?: date

        val asset0 = assets.find { it.id == t.assetId }
        if (asset0 != null) {
            val candidate = t.copy(
                sellPrice = price, sellShares = shares,
                sellCommission = f.commission, sellTransferFee = f.transferFee,
                sellStampDuty = f.stampDuty, sellTotalFee = f.totalFee, sellTotal = net,
                sellTradeDate = date, tradeDate = tradeDate, tradeStatus = "buyToSell"
            )
            if (!validateNoOversell(asset0, t.id, candidate))
                return OpResult(false, "卖出股数超过该时点持仓")
        }

        t.sellPrice = price
        t.sellShares = shares
        t.sellCommission = f.commission
        t.sellTransferFee = f.transferFee
        t.sellStampDuty = f.stampDuty
        t.sellTotalFee = f.totalFee
        t.sellTotal = net
        t.sellTradeDate = date
        t.tradeDate = tradeDate
        t.tradeStatus = "buyToSell"
        t.profit = Money.round2(net - t.buyTotal)
        t.remark = remark
        t.isMarginRepay = isMarginRepay
        t.updateTime = Dates.nowString()

        val asset = assets.find { it.id == t.assetId }
        if (asset != null) {
            // 卖出一律按净额冲减成本（卖出费用留在剩余持仓），T利润另算
            val cost = Money.round6(asset.totalCost - net)
            val sh = asset.shares - shares
            asset.totalCost = cost
            asset.shares = sh
            asset.costPerShare = if (sh > 0) Money.round6(cost / sh) else 0.0
        }
        afterTradeChange()
        return OpResult(true, trade = t)
    }

    /** 补充买入：sellNoBuy → sellToBuy */
    fun supplementBuy(tradeId: String, price: Double, shares: Int, buyDate: String, remark: String, isMarginBuy: Boolean = false): OpResult {
        val idx = trades.indexOfFirst { it.id == tradeId }
        if (idx == -1) return OpResult(false, "交易记录不存在")
        val t = trades[idx]
        val (f, _) = Fees.buyTotal(price, shares, settings)
        val buyTotal = Money.round2(price * shares + f.commission + f.transferFee)
        val date = buyDate.ifBlank { Dates.todayLocale() }
        // tradeDate 取买入/卖出较晚者，保证利润归属日正确
        val tradeDate = Dates.profitDate(null, date, t.sellTradeDate)?.let { Dates.formatCn(it) } ?: date

        t.buyPrice = price
        t.buyShares = shares
        t.buyCommission = f.commission
        t.buyTransferFee = f.transferFee
        t.buyTotalFee = Money.round2(f.commission + f.transferFee)
        t.buyTotal = buyTotal
        t.buyTradeDate = date
        t.tradeDate = tradeDate
        t.tradeStatus = "sellToBuy"
        t.profit = Money.round2(t.sellTotal - buyTotal)
        t.remark = remark
        t.isMarginBuy = isMarginBuy
        t.updateTime = Dates.nowString()

        val asset = assets.find { it.id == t.assetId }
        if (asset != null) {
            // 补买：只加买入总额，卖出净额已在原独立卖出时冲减过，无需调整
            val cost = Money.round6(asset.totalCost + buyTotal)
            val sh = asset.shares + shares
            asset.totalCost = cost
            asset.shares = sh
            asset.costPerShare = Money.round6(cost / sh)
        }
        afterTradeChange()
        return OpResult(true, trade = t)
    }

    /** 编辑正T买入记录（buyNoSell） */
    fun editBuyTrade(tradeId: String, price: Double, shares: Int, buyDate: String, remark: String, isMarginBuy: Boolean = false): OpResult {
        val idx = trades.indexOfFirst { it.id == tradeId }
        if (idx == -1) return OpResult(false, "交易记录不存在")
        val t = trades[idx]
        val oldShares = if (t.buyShares != 0) t.buyShares else t.shares
        val oldTotal = if (t.buyTotal != 0.0) t.buyTotal else t.total

        val (f, _) = Fees.buyTotal(price, shares, settings)
        val buyTotalFee = Money.round2(f.commission + f.transferFee)
        val buyTotal = Money.round2(price * shares + buyTotalFee)
        val date = buyDate.ifBlank { Dates.todayLocale() }

        val asset0 = assets.find { it.id == t.assetId }
        if (asset0 != null) {
            val candidate = t.copy(
                buyPrice = price, buyShares = shares,
                buyCommission = f.commission, buyTransferFee = f.transferFee,
                buyTotalFee = buyTotalFee, buyTotal = buyTotal, buyTradeDate = date,
                price = price, shares = shares, total = buyTotal,
                commission = f.commission, transferFee = f.transferFee, tradeDate = date
            )
            if (!validateNoOversell(asset0, t.id, candidate))
                return OpResult(false, "修改后卖出股数将超过该时点持仓")
        }

        t.buyPrice = price; t.buyShares = shares
        t.buyCommission = f.commission; t.buyTransferFee = f.transferFee
        t.buyTotalFee = buyTotalFee; t.buyTotal = buyTotal; t.buyTradeDate = date
        t.price = price; t.shares = shares; t.total = buyTotal
        t.commission = f.commission; t.transferFee = f.transferFee
        t.remark = remark; t.tradeDate = date; t.updateTime = Dates.nowString()
        t.isMarginBuy = isMarginBuy

        val asset = assets.find { it.id == t.assetId }
        if (asset != null) {
            val cost = Money.round6(asset.totalCost - oldTotal + buyTotal)
            val sh = asset.shares - oldShares + shares
            asset.totalCost = cost
            asset.shares = sh
            asset.costPerShare = if (sh > 0) Money.round6(cost / sh) else 0.0
        }
        afterTradeChange()
        return OpResult(true, trade = t)
    }

    /** 编辑反T卖出记录（sellNoBuy） */
    fun editSellTrade(tradeId: String, price: Double, shares: Int, sellDate: String, remark: String, isMarginRepay: Boolean = false): OpResult {
        val idx = trades.indexOfFirst { it.id == tradeId }
        if (idx == -1) return OpResult(false, "交易记录不存在")
        val t = trades[idx]
        val oldShares = if (t.sellShares != 0) t.sellShares else t.shares
        val oldNet = Money.round2(t.sellPrice * oldShares - t.sellTotalFee)

        val (f, net) = Fees.sellNet(price, shares, settings)
        val date = sellDate.ifBlank { Dates.todayLocale() }

        val asset0 = assets.find { it.id == t.assetId }
        if (asset0 != null) {
            val candidate = t.copy(
                sellPrice = price, sellShares = shares,
                sellCommission = f.commission, sellTransferFee = f.transferFee,
                sellStampDuty = f.stampDuty, sellTotalFee = f.totalFee, sellTotal = net,
                sellTradeDate = date, price = price, shares = shares, total = net,
                commission = f.commission, transferFee = f.transferFee, stampDuty = f.stampDuty,
                tradeDate = date
            )
            if (!validateNoOversell(asset0, t.id, candidate))
                return OpResult(false, "卖出股数超过该时点持仓")
        }

        t.sellPrice = price; t.sellShares = shares
        t.sellCommission = f.commission; t.sellTransferFee = f.transferFee; t.sellStampDuty = f.stampDuty
        t.sellTotalFee = f.totalFee; t.sellTotal = net; t.sellTradeDate = date
        t.price = price; t.shares = shares; t.total = net
        t.commission = f.commission; t.transferFee = f.transferFee; t.stampDuty = f.stampDuty
        t.remark = remark; t.tradeDate = date; t.updateTime = Dates.nowString()
        t.isMarginRepay = isMarginRepay

        val asset = assets.find { it.id == t.assetId }
        if (asset != null) {
            // 独立卖出：按新旧卖出净额差额调整成本（卖出费用留在剩余持仓）
            val cost = Money.round6(asset.totalCost + oldNet - net)
            val sh = asset.shares + oldShares - shares
            asset.totalCost = cost
            asset.shares = sh
            asset.costPerShare = if (sh > 0) Money.round6(cost / sh) else 0.0
        }
        afterTradeChange()
        return OpResult(true, trade = t)
    }

    /** 编辑 T完成记录（buyToSell / sellToBuy） */
    fun editCompletedTrade(
        tradeId: String, buyPrice: Double, buyShares: Int,
        sellPrice: Double, sellShares: Int, buyDate: String, sellDate: String, remark: String,
        isMarginBuy: Boolean = false, isMarginRepay: Boolean = false
    ): OpResult {
        val idx = trades.indexOfFirst { it.id == tradeId }
        if (idx == -1) return OpResult(false, "交易记录不存在")
        val t = trades[idx]
        val oldBuyTotal = t.buyTotal
        val oldBuyShares = t.buyShares
        val oldSellNet = t.sellTotal
        val oldSellShares = t.sellShares

        val (bf, _) = Fees.buyTotal(buyPrice, buyShares, settings)
        val buyTotalFee = Money.round2(bf.commission + bf.transferFee)
        val buyTotal = Money.round2(buyPrice * buyShares + buyTotalFee)
        val (sf, sellNet) = Fees.sellNet(sellPrice, sellShares, settings)
        val bDate = buyDate.ifBlank { Dates.todayLocale() }
        val sDate = sellDate.ifBlank { Dates.todayLocale() }
        // tradeDate 取买入/卖出较晚者，保证利润归属日正确
        val tradeDate = Dates.profitDate(null, bDate, sDate)?.let { Dates.formatCn(it) } ?: bDate

        val asset0 = assets.find { it.id == t.assetId }
        if (asset0 != null) {
            val candidate = t.copy(
                buyPrice = buyPrice, buyShares = buyShares,
                buyCommission = bf.commission, buyTransferFee = bf.transferFee,
                buyTotalFee = buyTotalFee, buyTotal = buyTotal, buyTradeDate = bDate,
                sellPrice = sellPrice, sellShares = sellShares,
                sellCommission = sf.commission, sellTransferFee = sf.transferFee,
                sellStampDuty = sf.stampDuty, sellTotalFee = sf.totalFee, sellTotal = sellNet,
                sellTradeDate = sDate, tradeDate = tradeDate
            )
            if (!validateNoOversell(asset0, t.id, candidate))
                return OpResult(false, "修改后卖出股数将超过该时点持仓")
        }

        t.buyPrice = buyPrice; t.buyShares = buyShares
        t.buyCommission = bf.commission; t.buyTransferFee = bf.transferFee
        t.buyTotalFee = buyTotalFee; t.buyTotal = buyTotal; t.buyTradeDate = bDate
        t.sellPrice = sellPrice; t.sellShares = sellShares
        t.sellCommission = sf.commission; t.sellTransferFee = sf.transferFee; t.sellStampDuty = sf.stampDuty
        t.sellTotalFee = sf.totalFee; t.sellTotal = sellNet; t.sellTradeDate = sDate
        t.tradeDate = tradeDate
        t.profit = Money.round2(sellNet - buyTotal)
        t.remark = remark
        t.updateTime = Dates.nowString()
        t.isMarginBuy = isMarginBuy
        t.isMarginRepay = isMarginRepay

        val asset = assets.find { it.id == t.assetId }
        if (asset != null) {
            // 卖出一律按净额冲减成本（卖出费用留在剩余持仓）
            var cost = asset.totalCost - oldBuyTotal + oldSellNet + buyTotal - sellNet
            var sh = asset.shares - oldBuyShares + oldSellShares + buyShares - sellShares
            if (sh < 0) sh = 0
            if (cost < 0) cost = 0.0
            cost = Money.round6(cost)
            asset.totalCost = cost
            asset.shares = sh
            asset.costPerShare = if (sh > 0) Money.round6(cost / sh) else 0.0
        }
        afterTradeChange()
        return OpResult(true, trade = t)
    }

    private fun afterTradeChange() {
        // 交易变更后：按交易回放重算持仓（与小程序页面 loadAssets/loadTrades 一致），再算利润
        recomputePositions()
        recomputeProfits()
        save()
        publish()
    }

    // ---------------- 删除 / 清空 ----------------

    fun deleteTrade(tradeId: String): Boolean {
        val removed = trades.removeAll { it.id == tradeId }
        if (removed) {
            recomputeAll()
            save()
            publish()
        }
        return removed
    }

    fun clearTradesForAsset(assetId: String) {
        trades.removeAll { it.assetId == assetId }
        val asset = assets.find { it.id == assetId }
        if (asset != null) {
            asset.totalCost = 0.0
            asset.shares = 0
            asset.costPerShare = 0.0
        }
        recomputeProfits()
        save()
        publish()
    }

    fun clearAllData() {
        assets.clear()
        trades.clear()
        accounts.clear()
        save()
        publish()
    }

    // ---------------- 查询 ----------------

    fun getAssetTrades(assetId: String): List<Trade> =
        trades.filter { it.assetId == assetId }

    // ---------------- 持仓回放 / 利润 / 统计 ----------------

    /**
     * 持仓回放：底仓起，按日期升序（同日先买后卖）重放全部交易。
     * 对齐 assets.js / trade-record.js loadXxx 中的回放逻辑。
     * 底仓成本价只来自用户设置的底仓，交易回放不会改写底仓。
     */
    fun replayPosition(asset: Asset, assetTrades: List<Trade>): Position {
        var cost = asset.baseTotalCost
        var sh = asset.baseShares
        var oversold = false
        var minSh = sh

        // 将每笔交易拆分为买入/卖出操作，各自按实际日期参与回放，
        // 保证买入日期和卖出日期独立的交易（如反T sellToBuy）在时间线上正确排列
        data class Op(val date: LocalDate, val isBuy: Boolean, val shares: Int, val total: Double)
        val ops = mutableListOf<Op>()
        for (t in assetTrades) {
            if (t.buyShares > 0 && t.buyTotal > 0) {
                val d = Dates.parseDate(t.buyTradeDate.ifBlank { t.tradeDate }) ?: LocalDate.MAX
                ops.add(Op(d, true, t.buyShares, t.buyTotal))
            }
            if (t.sellShares > 0 && t.sellTotal > 0) {
                val d = Dates.parseDate(t.sellTradeDate.ifBlank { t.tradeDate }) ?: LocalDate.MAX
                ops.add(Op(d, false, t.sellShares, t.sellTotal))
            }
        }
        // 按日期升序，同日先买后卖
        val sorted = ops.sortedWith(
            compareBy<Op> { it.date }
                .thenBy { if (it.isBuy) 0 else 1 }
        )

        for (op in sorted) {
            if (op.isBuy) {
                cost += op.total
                sh += op.shares
            } else {
                // 卖出按净额冲减成本，不钳制中间负值——
                // 历史超卖产生的负仓位由后续买入补回，保证最终持仓正确
                cost -= op.total
                sh -= op.shares
                if (sh < 0) oversold = true
            }
            if (sh < minSh) minSh = sh
        }
        sh = maxOf(0, sh)
        // 空仓时 totalCost 归零
        if (sh == 0) cost = 0.0
        cost = Money.round6(cost)
        val cps = if (sh > 0) Money.round3(cost / sh) else 0.0
        return Position(cost, sh, cps, oversold, minSh)
    }

    /**
     * 提交前校验：把目标交易替换为候选版本后回放，
     * 若变更引入了新的超卖（比历史更严重），返回 false。
     * 历史已有超卖（如反T先卖后买）不阻止新操作，只要新操作不使最低持仓更低。
     */
    private fun validateNoOversell(
        asset: Asset,
        targetTradeId: String? = null,
        candidate: Trade? = null,
        extra: Trade? = null
    ): Boolean {
        val original = getAssetTrades(asset.id)
        val probe = original.mapNotNull { t ->
            if (t.id == targetTradeId) candidate else t   // candidate=null 表示删除该交易
        }
        val full = if (extra != null) probe + extra else probe

        val beforePos = replayPosition(asset, original)
        val afterPos = replayPosition(asset, full)

        // 历史无超卖：变更后也不允许出现超卖
        if (!beforePos.oversold) return !afterPos.oversold
        // 历史已有超卖：只拒绝使最低持仓更低的变更
        return afterPos.minShares >= beforePos.minShares
    }

    fun recomputePositions() {
        for (a in assets) {
            val p = replayPosition(a, getAssetTrades(a.id))
            a.totalCost = p.totalCost
            a.shares = p.shares
            a.costPerShare = p.costPerShare
        }
    }

    /**
     * app.js calculateAssetProfit 的记录利润口径：
     * 总利润 = 该资产全部交易 t.profit 之和；月/今日按 tradeDate→买日→卖日归属。
     * 与每笔交易卡片显示的盈亏逐笔可对账；利润日历见 StatisticsCalc。
     */
    fun calculateAssetProfit(assetId: String) {
        val asset = assets.find { it.id == assetId } ?: return
        val now = LocalDate.now()
        var total = 0.0
        var month = 0.0
        var today = 0.0
        for (e in RealizedProfit.events(asset, getAssetTrades(assetId))) {
            total += e.profit
            val d = e.date
            if (d != null) {
                if (d.year == now.year && d.monthValue == now.monthValue) month += e.profit
                if (d == now) today += e.profit
            }
        }
        asset.totalProfit = Money.round2(total)
        asset.monthProfit = Money.round2(month)
        asset.todayProfit = Money.round2(today)
    }

    fun recomputeProfits() {
        for (a in assets) calculateAssetProfit(a.id)
    }

    fun recomputeAll() {
        recomputePositions()
        recomputeProfits()
    }

    fun getStatistics(): Stats {
        var totalProfit = 0.0
        var totalCost = 0.0
        var totalShares = 0
        var aStock = 0
        var hkStock = 0
        var etf = 0
        for (a in assets) {
            totalProfit += a.totalProfit
            totalCost += a.totalCost
            totalShares += a.shares
            when (a.market) {
                "A股" -> aStock++
                "港股" -> hkStock++
                "ETF" -> etf++
            }
        }
        return Stats(
            totalAssets = assets.size,
            totalProfit = Money.round2(totalProfit),
            totalCost = Money.round2(totalCost),
            totalShares = totalShares,
            aStock = aStock,
            hkStock = hkStock,
            etf = etf,
            totalTrades = trades.size,
            buyCount = trades.count { it.tradeStatus.startsWith("buy") },
            sellCount = trades.count { it.tradeStatus.startsWith("sell") }
        )
    }

    // ---------------- 设置 ----------------

    fun updateSettings(transform: (Settings) -> Settings) {
        settings = transform(settings)
        store.saveSettings(settings)
        publish()
    }

    // ---------------- 备份 ----------------

    fun exportCsv(): String = Backup.jsonToCsv(assets, trades)

    fun exportJson(): String {
        val gson = com.google.gson.Gson()
        val payload = mapOf(
            "version" to "2.0.0",
            "exportTime" to Dates.nowString(),
            "exportSettings" to mapOf("format" to "json"),
            "assets" to assets,
            "trades" to trades,
            "settings" to settings
        )
        return gson.toJson(payload)
    }

    /** CSV 导入（覆盖式），返回 (资产数, 交易数) */
    fun importCsv(content: String): Pair<Int, Int> {
        val result = Backup.csvToJson(content)
        assets.clear()
        assets.addAll(result.assets)
        trades.clear()
        trades.addAll(result.trades)
        recomputeAll()
        save()
        publish()
        return assets.size to trades.size
    }

    /** JSON 备份导入（覆盖式） */
    fun importJson(content: String): Pair<Int, Int> {
        val gson = com.google.gson.Gson()
        val obj = gson.fromJson(content, com.google.gson.JsonObject::class.java)
            ?: throw Backup.BackupException("文件格式错误：不是有效的 JSON 备份")
        val importedAssets: MutableList<Asset> = if (obj.has("assets") && !obj.get("assets").isJsonNull)
            gson.fromJson(obj.get("assets"), object : com.google.gson.reflect.TypeToken<MutableList<Asset>>() {}.type)
        else mutableListOf()
        val importedTrades: MutableList<Trade> = if (obj.has("trades") && !obj.get("trades").isJsonNull)
            gson.fromJson(obj.get("trades"), object : com.google.gson.reflect.TypeToken<MutableList<Trade>>() {}.type)
        else mutableListOf()
        if (obj.has("settings") && !obj.get("settings").isJsonNull) {
            runCatching {
                val s = gson.fromJson(obj.get("settings"), Settings::class.java)
                if (s != null) settings = s
            }
        }
        assets.clear()
        assets.addAll(importedAssets.filter { it.name.isNotBlank() })
        trades.clear()
        trades.addAll(importedTrades.filter { it.assetId.isNotBlank() })
        recomputeAll()
        save()
        publish()
        return assets.size to trades.size
    }

    // ---------------- 融资资产 / 负债计算 ----------------

    /**
     * 融资资产概览。
     *
     * - netAssets（净资产）：所有未标记为融资买入的持仓股票的当前市值（currentPrice × shares）。
     * - marginPrincipal（融资本金）：所有标记为融资买入的交易买入金额总和 − 所有标记为卖券还款的交易卖出金额总和。
     * - marginInterest（融资利息）：按融资本金、融资年利率、天数（自买入日起算的自然日）逐笔计息。
     * - marginDebt（融资负债）：本金 + 利息。
     * - totalAssets（总资产）：净资产 + 融资负债。
     * - maintenanceRatio（维持担保比例）：（净资产 + 融资负债）/ 融资负债 × 100%。
     *   证券公司口径：维持担保比例 = 信用账户总资产 / 融资负债。
     */
    data class FinanceSummary(
        val netAssets: Double,
        val marginPrincipal: Double,
        val marginInterest: Double,
        val marginDebt: Double,
        val totalAssets: Double,
        val maintenanceRatio: Double
    )

    /**
     * 逐笔融资明细：用于资产页融资负债展开查看。
     * 每笔融资买入产生一条记录，卖券还款按时间顺序冲减最早的本金。
     */
    data class MarginEntry(
        val assetName: String,
        val date: String,
        val principal: Double,
        val interest: Double,
        val total: Double,
        val days: Long
    )

    /**
     * @param currentPrices assetId → 当前股价（由 UI 联网获取后传入；缺失则按成本价计）
     */
    fun calcFinanceSummary(currentPrices: Map<String, Double>): FinanceSummary {
        // 净资产：未标记融资买入的持仓股票当前市值
        var netAssets = 0.0
        for (a in assets) {
            if (a.shares <= 0) continue
            // 检查该资产是否有任何融资买入的交易
            val hasMarginBuy = trades.any {
                it.assetId == a.id && it.isMarginBuy &&
                    (it.buyShares > 0 || it.buyTotal > 0)
            }
            if (!hasMarginBuy) {
                val price = currentPrices[a.id] ?: a.costPerShare
                netAssets += price * a.shares
            }
        }

        // 融资本金：融资买入金额总和 - 卖券还款卖出金额总和
        var marginPrincipal = 0.0
        for (t in trades) {
            if (t.isMarginBuy && t.buyTotal > 0) {
                marginPrincipal += t.buyTotal
            }
            if (t.isMarginRepay && t.sellTotal > 0) {
                marginPrincipal -= t.sellTotal
            }
        }
        marginPrincipal = Money.round2(marginPrincipal)

        // 融资利息：按每笔融资买入的金额，自买入日起按自然日计息
        val annualRate = settings.marginRate / 100.0
        val today = LocalDate.now()
        var marginInterest = 0.0
        for (t in trades) {
            if (t.isMarginBuy && t.buyTotal > 0) {
                val buyDate = Dates.parseDate(t.buyTradeDate.ifBlank { t.tradeDate })
                val days = if (buyDate != null) {
                    java.time.temporal.ChronoUnit.DAYS.between(buyDate, today).coerceAtLeast(0)
                } else 0L
                // 日利率 = 年利率 / 360（银行惯例按 360 天）
                val interest = t.buyTotal * annualRate / 360.0 * days
                marginInterest += interest
            }
            if (t.isMarginRepay && t.sellTotal > 0) {
                // 卖券还款减少本金，相应减少已产生的利息（按还款日的剩余天数冲减）
                val sellDate = Dates.parseDate(t.sellTradeDate.ifBlank { t.tradeDate })
                val days = if (sellDate != null) {
                    java.time.temporal.ChronoUnit.DAYS.between(sellDate, today).coerceAtLeast(0)
                } else 0L
                val interestReduction = t.sellTotal * annualRate / 360.0 * days
                marginInterest -= interestReduction
            }
        }
        marginInterest = Money.round2(marginInterest)

        val marginDebt = Money.round2(marginPrincipal + marginInterest)
        val totalAssets = Money.round2(netAssets + marginDebt)
        val ratio = if (marginDebt > 0) (totalAssets / marginDebt) * 100.0 else Double.POSITIVE_INFINITY

        return FinanceSummary(
            netAssets = Money.round2(netAssets),
            marginPrincipal = marginPrincipal,
            marginInterest = marginInterest,
            marginDebt = marginDebt,
            totalAssets = totalAssets,
            maintenanceRatio = ratio
        )
    }

    /**
     * 逐笔融资明细列表（用于展开查看本金和利息）。
     * 列出每笔融资买入及其当前利息，以及每笔卖券还款。
     */
    fun calcMarginEntries(): List<MarginEntry> {
        val annualRate = settings.marginRate / 100.0
        val today = LocalDate.now()
        val entries = mutableListOf<MarginEntry>()

        for (t in trades) {
            if (t.isMarginBuy && t.buyTotal > 0) {
                val buyDate = Dates.parseDate(t.buyTradeDate.ifBlank { t.tradeDate })
                val days = if (buyDate != null) {
                    java.time.temporal.ChronoUnit.DAYS.between(buyDate, today).coerceAtLeast(0)
                } else 0L
                val interest = Money.round2(t.buyTotal * annualRate / 360.0 * days)
                entries.add(MarginEntry(
                    assetName = t.assetName,
                    date = t.buyTradeDate.ifBlank { t.tradeDate },
                    principal = t.buyTotal,
                    interest = interest,
                    total = Money.round2(t.buyTotal + interest),
                    days = days
                ))
            }
            if (t.isMarginRepay && t.sellTotal > 0) {
                val sellDate = Dates.parseDate(t.sellTradeDate.ifBlank { t.tradeDate })
                val days = if (sellDate != null) {
                    java.time.temporal.ChronoUnit.DAYS.between(sellDate, today).coerceAtLeast(0)
                } else 0L
                val interestReduction = Money.round2(t.sellTotal * annualRate / 360.0 * days)
                entries.add(MarginEntry(
                    assetName = t.assetName,
                    date = t.sellTradeDate.ifBlank { t.tradeDate },
                    principal = -t.sellTotal,
                    interest = -interestReduction,
                    total = Money.round2(-(t.sellTotal + interestReduction)),
                    days = days
                ))
            }
        }
        return entries
    }

    /**
     * 预估取款计算：当维持担保比例 > threshold（默认300%）时可取款。
     * 给定当前市值和想取出的金额，计算需要卖出多少股票。
     *
     * 逻辑：卖出股票后，卖出所得资金直接取出（减少总资产），同时不改变融资负债。
     * 需要卖出的股票数量 = ceil(取款金额 / 当前股价)
     * 卖出后重新计算担保比例是否仍 > threshold。
     *
     * @param currentPrices assetId → 当前股价
     * @param withdrawAmount 想要取出的金额
     * @param threshold 担保比例阈值（%），默认 300
     * @return 需要卖出的总股数、卖出后担保比例，或 null（无法取出）
     */
    data class WithdrawEstimate(
        val canWithdraw: Boolean,
        val sellShares: Int,
        val sellAmount: Double,
        val ratioAfter: Double,
        val message: String
    )

    fun estimateWithdraw(
        currentPrices: Map<String, Double>,
        withdrawAmount: Double,
        threshold: Double = 300.0
    ): WithdrawEstimate {
        val summary = calcFinanceSummary(currentPrices)
        if (summary.marginDebt <= 0) {
            return WithdrawEstimate(false, 0, 0.0, summary.maintenanceRatio,
                "当前无融资负债，无需担保比例限制，可直接取款（需卖出股票获取现金）")
        }
        if (summary.maintenanceRatio <= threshold) {
            return WithdrawEstimate(false, 0, 0.0, summary.maintenanceRatio,
                "当前维持担保比例 %.2f%% ≤ %.0f%%，不满足取款条件".format(summary.maintenanceRatio, threshold))
        }

        // 计算在保持担保比例 > threshold 的前提下，最多可取出的金额
        // (totalAssets - withdraw) / marginDebt > threshold / 100
        // withdraw < totalAssets - marginDebt * threshold / 100
        val maxWithdraw = summary.totalAssets - summary.marginDebt * threshold / 100.0
        if (withdrawAmount > maxWithdraw) {
            return WithdrawEstimate(false, 0, 0.0, summary.maintenanceRatio,
                "取款金额 %.2f 超过最大可取额度 %.2f（担保比例需维持 > %.0f%%）".format(
                    withdrawAmount, maxWithdraw, threshold))
        }

        // 需要卖出股票来获取取款现金：按当前股价计算需要卖出多少股
        // 选择有持仓且未标记融资买入的股票（自有资金持仓），优先卖股价高的以减少卖出股数
        val candidates = assets.filter { it.shares > 0 && currentPrices[it.id] != null }
            .sortedByDescending { currentPrices[it.id]!! }

        var remaining = withdrawAmount
        var totalSellShares = 0
        var totalSellAmount = 0.0

        for (a in candidates) {
            if (remaining <= 0) break
            val price = currentPrices[a.id]!!
            val sharesNeeded = kotlin.math.ceil(remaining / price).toInt()
            val sharesToSell = minOf(sharesNeeded, a.shares)
            val sellAmount = sharesToSell * price
            totalSellShares += sharesToSell
            totalSellAmount += sellAmount
            remaining -= sellAmount
        }

        if (remaining > 0.01) {
            return WithdrawEstimate(false, 0, 0.0, summary.maintenanceRatio,
                "持仓不足，无法卖出足够股票取出 %.2f 元".format(withdrawAmount))
        }

        // 卖出后的担保比例：总资产减少 withdrawAmount（现金被取出），融资负债不变
        val totalAssetsAfter = summary.totalAssets - withdrawAmount
        val ratioAfter = (totalAssetsAfter / summary.marginDebt) * 100.0

        return WithdrawEstimate(
            canWithdraw = true,
            sellShares = totalSellShares,
            sellAmount = Money.round2(totalSellAmount),
            ratioAfter = Money.round2(ratioAfter),
            message = "需卖出 %d 股（约 %.2f 元），取出后担保比例 %.2f%%".format(
                totalSellShares, totalSellAmount, ratioAfter)
        )
    }

    companion object {
        @Volatile
        private var instance: AppRepository? = null

        fun init(dataDir: File): AppRepository {
            instance?.let { return it }
            val dir = File(dataDir, "tassetbook")
            val repo = AppRepository(LocalStore(dir))
            repo.load()
            instance = repo
            return repo
        }

        fun get(): AppRepository =
            instance ?: error("AppRepository not initialized")
    }
}
