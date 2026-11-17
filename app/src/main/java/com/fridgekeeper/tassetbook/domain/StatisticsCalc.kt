package com.fridgekeeper.tassetbook.domain

import com.fridgekeeper.tassetbook.data.Asset
import com.fridgekeeper.tassetbook.data.Trade
import com.fridgekeeper.tassetbook.util.Dates
import com.fridgekeeper.tassetbook.util.Money
import java.time.LocalDate

/**
 * 统计页计算，对齐 statistics.js：
 * - 月内每日成本价历史（从底仓起按日回放，图表用 tradeDate 优先日期）
 * - 利润日历（仅 T完成 buyToSell/sellToBuy 交易，按买卖较晚日期归属）
 *   未T完的单独买入/卖出记录不进日历；顶部总/本月/今日利润另走 RealizedProfit 记录口径
 */
object StatisticsCalc {

    data class DayPoint(
        val day: Int,
        val date: LocalDate,
        val costPerShare: Double,
        val shares: Int,
        val totalCost: Double,
        val hasPosition: Boolean = true   // false=当日空仓，折线在此处断开
    )

    data class AssetDayProfit(
        val assetId: String,
        val assetName: String,
        val profit: Double,
        val tradeCount: Int
    )

    data class CalendarDay(
        val day: Int,
        val date: LocalDate?,           // null 表示占位格
        val profit: Double,
        val isToday: Boolean,
        val isFuture: Boolean,
        val hasProfit: Boolean,
        val assetProfits: List<AssetDayProfit>
    )

    /** 图表日期：tradeDate 优先，其次买/卖日期（对齐 statistics.js loadStatistics） */
    private fun chartDate(t: Trade): LocalDate? =
        Dates.parseDate(t.tradeDate) ?: Dates.parseDate(t.buyTradeDate) ?: Dates.parseDate(t.sellTradeDate)

    /**
     * 利润日历事件：仅 T完成（buyToSell/sellToBuy）交易，
     * 对齐 statistics.js generateCalendarData：
     * - 利润 = 卖出净额 - 买入总额，归属日取 tradeDate/买日/卖日最晚者
     */
    private fun calendarEvents(assets: List<Asset>, allTrades: List<Trade>): List<RealizedProfit.Event> =
        assets.flatMap { a ->
            allTrades.asSequence()
                .filter { it.assetId == a.id }
                .filter { it.tradeStatus == "buyToSell" || it.tradeStatus == "sellToBuy" }
                .map { t ->
                    RealizedProfit.Event(
                        a.id, a.name,
                        Money.round2(t.sellTotal - t.buyTotal),
                        Dates.profitDate(t.tradeDate, t.buyTradeDate, t.sellTradeDate)
                    )
                }
                .filter { it.profit != 0.0 }
                .toList()
        }

    fun monthCostHistory(asset: Asset, allTrades: List<Trade>, year: Int, month: Int): List<DayPoint> {
        val first = LocalDate.of(year, month, 1)
        val daysInMonth = first.lengthOfMonth()
        val today = LocalDate.now()

        // 与 replayPosition 一致：用当前底仓 + 全部交易回放，保证当前日成本价与股票列表一致
        val assetTrades = allTrades.filter { it.assetId == asset.id }

        // 将交易拆分为买入/卖出操作，各自按实际日期参与每日回放
        data class Op(val date: LocalDate, val isBuy: Boolean, val shares: Int, val total: Double)
        val ops = mutableListOf<Op>()
        for (t in assetTrades) {
            if (t.buyShares > 0 && t.buyTotal > 0) {
                val d = Dates.parseDate(t.buyTradeDate.ifBlank { t.tradeDate }) ?: continue
                ops.add(Op(d, true, t.buyShares, t.buyTotal))
            }
            if (t.sellShares > 0 && t.sellTotal > 0) {
                val d = Dates.parseDate(t.sellTradeDate.ifBlank { t.tradeDate }) ?: continue
                ops.add(Op(d, false, t.sellShares, t.sellTotal))
            }
        }
        val sorted = ops.sortedWith(
            compareBy<Op> { it.date }
                .thenBy { if (it.isBuy) 0 else 1 }
        )

        val result = mutableListOf<DayPoint>()
        for (day in 1..daysInMonth) {
            val current = LocalDate.of(year, month, day)
            var cost = asset.baseTotalCost
            var shares = asset.baseShares
            for (op in sorted) {
                if (!op.date.isAfter(current)) {
                    if (op.isBuy) {
                        cost += op.total
                        shares += op.shares
                    } else {
                        // 与 replayPosition 一致：不钳制中间负值，历史超卖由后续买入补回
                        cost -= op.total
                        shares -= op.shares
                    }
                }
            }
            val hasPosition = shares > 0
            val displayCost = if (hasPosition) cost else 0.0
            val cps = if (hasPosition) cost / shares else 0.0
            if (hasPosition || !current.isAfter(today)) {
                result.add(DayPoint(day, current, cps, if (hasPosition) shares else 0, displayCost, hasPosition))
            }
        }
        return result
    }

    /**
     * 指定交易日序列上的成本价走势，用于统计页走势图。
     * tradingDays 只含交易日（已排除周末与法定节假日），按日期升序。
     */
    fun recentCostHistory(asset: Asset, allTrades: List<Trade>, tradingDays: List<LocalDate>): List<DayPoint> {
        // 与 replayPosition 一致：用当前底仓 + 全部交易回放，保证当前日成本价与股票列表一致
        val assetTrades = allTrades.filter { it.assetId == asset.id }

        // 将交易拆分为买入/卖出操作，各自按实际日期参与回放
        data class Op(val date: LocalDate, val isBuy: Boolean, val shares: Int, val amount: Double)
        val ops = mutableListOf<Op>()
        for (t in assetTrades) {
            if (t.buyShares > 0 && t.buyTotal > 0) {
                val d = Dates.parseDate(t.buyTradeDate.ifBlank { t.tradeDate }) ?: continue
                ops.add(Op(d, true, t.buyShares, t.buyTotal))
            }
            if (t.sellShares > 0 && t.sellTotal > 0) {
                val d = Dates.parseDate(t.sellTradeDate.ifBlank { t.tradeDate }) ?: continue
                // 卖出统一按净额冲减成本，与 replayPosition 口径一致
                ops.add(Op(d, false, t.sellShares, t.sellTotal))
            }
        }
        val sorted = ops.sortedWith(
            compareBy<Op> { it.date }
                .thenBy { if (it.isBuy) 0 else 1 }
        )

        val result = mutableListOf<DayPoint>()
        for (current in tradingDays) {
            var cost = asset.baseTotalCost
            var shares = asset.baseShares
            for (op in sorted) {
                if (!op.date.isAfter(current)) {
                    if (op.isBuy) {
                        cost += op.amount
                        shares += op.shares
                    } else {
                        // 不钳制中间负值，与 replayPosition 一致
                        cost -= op.amount
                        shares -= op.shares
                    }
                }
            }
            val hasPosition = shares > 0
            val displayCost = if (hasPosition) cost else 0.0
            val cps = if (hasPosition) cost / shares else 0.0
            result.add(DayPoint(current.dayOfMonth, current, cps, if (hasPosition) shares else 0, displayCost, hasPosition))
        }
        return result
    }

    /**
     * 利润日历：按 T完成事件的利润归属日（买卖较晚日）聚合。
     */
    fun profitCalendar(assets: List<Asset>, allTrades: List<Trade>, year: Int, month: Int): List<CalendarDay> {
        val first = LocalDate.of(year, month, 1)
        val daysInMonth = first.lengthOfMonth()
        val today = LocalDate.now()

        data class DayAgg(var profit: Double, val assets: MutableMap<String, AssetDayProfit>)

        val byDay = HashMap<Int, DayAgg>()
        for (e in calendarEvents(assets, allTrades)) {
            val d = e.date ?: continue
            if (d.year != year || d.monthValue != month) continue
            val p = Money.round2(e.profit)
            val agg = byDay.getOrPut(d.dayOfMonth) { DayAgg(0.0, HashMap()) }
            agg.profit += p
            val ap = agg.assets.getOrPut(e.assetId) { AssetDayProfit(e.assetId, e.assetName, 0.0, 0) }
            agg.assets[e.assetId] = ap.copy(profit = Money.round2(ap.profit + p), tradeCount = ap.tradeCount + 1)
        }

        val calendar = mutableListOf<CalendarDay>()
        // 月初占位（仅周一至周五，5列日历）
        val lead = if (first.dayOfWeek.value <= 5) first.dayOfWeek.value - 1 else 5
        repeat(lead) { calendar.add(CalendarDay(0, null, 0.0, false, false, false, emptyList())) }
        for (day in 1..daysInMonth) {
            val date = LocalDate.of(year, month, day)
            // 周六日不显示
            if (date.dayOfWeek.value >= 6) continue
            val agg = byDay[day]
            val profit = agg?.profit?.let { Money.round2(it) } ?: 0.0
            val assetList = agg?.assets?.values?.sortedByDescending { it.profit } ?: emptyList()
            calendar.add(
                CalendarDay(
                    day = day,
                    date = date,
                    profit = profit,
                    isToday = date == today,
                    isFuture = date.isAfter(today),
                    hasProfit = agg != null && profit != 0.0,
                    assetProfits = assetList
                )
            )
        }
        // 补齐 5 的倍数
        while (calendar.size % 5 != 0) {
            calendar.add(CalendarDay(0, null, 0.0, false, false, false, emptyList()))
        }
        return calendar
    }

    /** 某月利润日历合计（仅 T完成，按归属日） */
    fun monthlyProfit(assets: List<Asset>, allTrades: List<Trade>, year: Int, month: Int): Double {
        var sum = 0.0
        for (e in calendarEvents(assets, allTrades)) {
            val d = e.date ?: continue
            if (d.year == year && d.monthValue == month) sum += e.profit
        }
        return Money.round2(sum)
    }

    /** 统计页"总做T利润"：全部 T完成事件利润合计（与交易记录页头部同口径） */
    fun totalRealizedProfit(assets: List<Asset>, allTrades: List<Trade>): Double =
        Money.round2(calendarEvents(assets, allTrades).sumOf { it.profit })

    /** 统计页"今日利润"：指定日期的 T完成事件利润合计 */
    fun dailyRealizedProfit(assets: List<Asset>, allTrades: List<Trade>, day: LocalDate): Double {
        var sum = 0.0
        for (e in calendarEvents(assets, allTrades)) {
            if (e.date == day) sum += e.profit
        }
        return Money.round2(sum)
    }

    /** 单资产指定日期的做T利润（T完成，按利润归属日），走势图卡片"今日T利润"用 */
    fun assetDailyProfit(asset: Asset, allTrades: List<Trade>, day: LocalDate): Double {
        var sum = 0.0
        for (t in allTrades) {
            if (t.assetId != asset.id) continue
            if (t.tradeStatus != "buyToSell" && t.tradeStatus != "sellToBuy") continue
            val d = Dates.profitDate(t.tradeDate, t.buyTradeDate, t.sellTradeDate)
            if (d == day) {
                sum += Money.round2(t.sellTotal - t.buyTotal)
            }
        }
        return Money.round2(sum)
    }
}
