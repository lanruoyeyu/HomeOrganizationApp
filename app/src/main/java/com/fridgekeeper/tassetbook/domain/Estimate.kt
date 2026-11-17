package com.fridgekeeper.tassetbook.domain

import com.fridgekeeper.tassetbook.util.Money
import kotlin.math.min

/**
 * 预估交易阶梯测算，对齐 estimate-trade.js calculateEstimates。
 */
object Estimate {
    data class Row(
        val index: Int,
        val price: String,
        val sellPrice: String,      // 本次卖出额
        val remainingShares: String,
        val totalPrice: String,     // 单价×剩余股数
        val totalRevenue: String,   // 累计卖出收入
        val profit: String,
        val profitNum: Double
    )

    /**
     * @param estimatedPrice 预估股价
     * @param costPrice 成本价
     * @param shares 总股数
     * @param tradeShares 单次卖出股数
     * @param ratio 阶梯比例（%，如 1.017）
     */
    fun calculate(
        estimatedPrice: Double,
        costPrice: Double,
        shares: Int,
        tradeShares: Int,
        ratio: Double
    ): List<Row> {
        if (estimatedPrice <= 0 || costPrice <= 0 || shares <= 0 || tradeShares <= 0 || ratio <= 0) {
            return emptyList()
        }
        val rows = mutableListOf<Row>()
        val priceStep = estimatedPrice * (ratio / 100.0)
        val totalCost = costPrice * shares
        var totalRevenue = 0.0
        var cumulativeSoldShares = 0

        for (i in 1..100) {
            val price = estimatedPrice + ((i - 1) * priceStep)
            val remainingBefore = shares - cumulativeSoldShares
            if (remainingBefore <= 0) break

            val currentSellShares = min(tradeShares, remainingBefore)
            val sellAmount = price * currentSellShares
            totalRevenue += sellAmount
            cumulativeSoldShares += currentSellShares

            // 市值按本次卖出后的剩余股数计算，避免卖出股数在市值和累计收入中重复计一次
            val remaining = shares - cumulativeSoldShares
            val marketValue = price * remaining
            val profit = marketValue + totalRevenue - totalCost

            rows.add(
                Row(
                    index = i,
                    price = Money.fixed(price, 2),
                    sellPrice = Money.fixed(sellAmount, 2),
                    remainingShares = remaining.toString(),
                    totalPrice = Money.fixed(marketValue, 2),
                    totalRevenue = Money.fixed(totalRevenue, 2),
                    profit = Money.fixed(profit, 2),
                    profitNum = Money.round2(profit)
                )
            )
        }
        return rows
    }
}
