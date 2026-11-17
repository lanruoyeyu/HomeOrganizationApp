package com.fridgekeeper.tassetbook.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fridgekeeper.tassetbook.data.AccountTypes
import com.fridgekeeper.tassetbook.data.AppRepository
import com.fridgekeeper.tassetbook.ui.BgGray
import com.fridgekeeper.tassetbook.ui.CardWhite
import com.fridgekeeper.tassetbook.ui.DividerGray
import com.fridgekeeper.tassetbook.ui.PrimaryBlue
import com.fridgekeeper.tassetbook.ui.ProfitRed
import com.fridgekeeper.tassetbook.ui.TextHint
import com.fridgekeeper.tassetbook.ui.TextPrimary
import com.fridgekeeper.tassetbook.ui.TextSecondary
import com.fridgekeeper.tassetbook.ui.fmt2
import com.fridgekeeper.tassetbook.util.StockApi
import kotlinx.coroutines.delay

/**
 * 资产主页：账户总览。
 * 左上角 + 添加账户；四类账户分组展示；股票账户为系统项（金额=股票账户总资产），
 * 点击进入股票详情页（原融资/持仓界面）。
 */
@Composable
fun AccountsScreen(
    onAddAccount: () -> Unit,
    onEditAccount: (String) -> Unit,
    onStockDetail: () -> Unit
) {
    val repo = AppRepository.get()
    val accounts by repo.accountsFlow.collectAsState()
    val assets by repo.assetsFlow.collectAsState()
    val trades by repo.tradesFlow.collectAsState()

    // 联网获取现价，用于计算股票账户金额
    var prices by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    val holdingAssets = remember(assets) { assets.filter { it.shares > 0 } }
    LaunchedEffect(holdingAssets.map { it.id }) {
        var retry = 0
        while (true) {
            for (a in holdingAssets) {
                if (StockApi.inTradingHours() || prices[a.id] == null) {
                    StockApi.fetchPrice(a.name)?.let { p -> prices = prices + (a.id to p) }
                }
            }
            if (!StockApi.inTradingHours() && holdingAssets.all { prices[it.id] != null }) break
            if (!StockApi.inTradingHours()) {
                retry++
                if (retry >= 20) break
            }
            delay(5_000)
        }
    }

    val stockSummary = remember(assets, trades, prices) {
        repo.calcFinanceSummary(prices)
    }

    val assetSum = repo.sumAccountsByCategory(AccountTypes.CAT_ASSET)
    val creditSum = repo.sumAccountsByCategory(AccountTypes.CAT_CREDIT)
    val prepaidSum = repo.sumAccountsByCategory(AccountTypes.CAT_PREPAID)
    val investManualSum = repo.sumAccountsByCategory(AccountTypes.CAT_INVEST)
    val investSum = investManualSum + stockSummary.totalAssets
    val totalPositive = assetSum + prepaidSum + investSum
    val netAssets = totalPositive - creditSum

    Column(
        Modifier
            .fillMaxSize()
            .background(BgGray)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // 顶栏：左上角 + 号
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onAddAccount, modifier = Modifier.padding(start = 4.dp, top = 8.dp)) {
                Icon(Icons.Filled.Add, contentDescription = "添加账户", tint = PrimaryBlue)
            }
            Text(
                "资产",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // 汇总卡
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            colors = CardDefaults.cardColors(containerColor = PrimaryBlue),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("净资产", fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
                Text(
                    fmt2(netAssets) + " 元",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("总资产 ${fmt2(totalPositive)} 元", fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f))
                    Text("总负债 ${fmt2(creditSum)} 元", fontSize = 13.sp, color = Color(0xFFFFD54F))
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // 四类账户分组
        AccountTypes.categories.forEach { cat ->
            val list = accounts.filter { it.category == cat.key }
            val subtotal = when (cat.key) {
                AccountTypes.CAT_INVEST -> investSum
                else -> list.sumOf { it.balance }
            }
            AccountCategoryCard(
                label = cat.label,
                count = list.size + if (cat.key == AccountTypes.CAT_INVEST) 1 else 0,
                subtotal = subtotal,
                isDebt = cat.key == AccountTypes.CAT_CREDIT
            ) {
                if (cat.key == AccountTypes.CAT_INVEST) {
                    // 股票系统账户：金额=股票账户总资产，点击看详情
                    AccountRow(
                        name = AccountTypes.TYPE_STOCK,
                        sub = if (stockSummary.marginDebt > 0)
                            "含融资负债 ${fmt2(stockSummary.marginDebt)} 元" else null,
                        amount = stockSummary.totalAssets,
                        amountColor = TextPrimary,
                        showArrow = true,
                        onClick = onStockDetail
                    )
                }
                list.forEach { acc ->
                    AccountRow(
                        name = acc.name,
                        sub = buildList {
                            if (acc.name != acc.type) add(acc.type)
                            if (cat.key == AccountTypes.CAT_CREDIT) {
                                val days = listOfNotNull(
                                    acc.billingDay?.let { "账单日 $it 日" },
                                    acc.repaymentDay?.let { "还款日 $it 日" }
                                )
                                if (days.isNotEmpty()) add(days.joinToString(" / "))
                            }
                            if (acc.type == AccountTypes.TYPE_LOAN || acc.ledgerSelectable == false) {
                                add("记账不可选")
                            }
                            acc.remark?.takeIf { it.isNotBlank() }?.let { add(it) }
                        }.joinToString(" · ").ifBlank { null },
                        amount = acc.balance,
                        amountColor = if (cat.key == AccountTypes.CAT_CREDIT) ProfitRed else TextPrimary,
                        showArrow = false,
                        onClick = { onEditAccount(acc.id) }
                    )
                }
                if (list.isEmpty() && cat.key != AccountTypes.CAT_INVEST) {
                    Text(
                        "暂无账户，点击左上角 + 添加",
                        fontSize = 12.sp,
                        color = TextHint,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountCategoryCard(
    label: String,
    count: Int,
    subtotal: Double,
    isDebt: Boolean,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(6.dp))
                Text("${count}个", fontSize = 11.sp, color = TextHint)
                Spacer(Modifier.weight(1f))
                Text(
                    (if (isDebt) "-" else "") + fmt2(subtotal) + " 元",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDebt) ProfitRed else TextPrimary
                )
            }
            Spacer(
                Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DividerGray)
            )
            content()
        }
    }
}

@Composable
private fun AccountRow(
    name: String,
    sub: String?,
    amount: Double,
    amountColor: Color,
    showArrow: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, fontSize = 14.sp, color = TextPrimary)
            if (sub != null) {
                Text(sub, fontSize = 11.sp, color = TextHint)
            }
        }
        Text(fmt2(amount), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = amountColor)
        if (showArrow) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = TextHint,
                modifier = Modifier.padding(start = 2.dp)
            )
        }
    }
}
