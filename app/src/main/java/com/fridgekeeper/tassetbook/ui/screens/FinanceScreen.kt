package com.fridgekeeper.tassetbook.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fridgekeeper.tassetbook.data.AppRepository
import com.fridgekeeper.tassetbook.ui.BgGray
import com.fridgekeeper.tassetbook.ui.CardWhite
import com.fridgekeeper.tassetbook.ui.CompactTextField
import com.fridgekeeper.tassetbook.ui.LossGreen
import com.fridgekeeper.tassetbook.ui.PriceOrange
import com.fridgekeeper.tassetbook.ui.PrimaryBlue
import com.fridgekeeper.tassetbook.ui.ProfitRed
import com.fridgekeeper.tassetbook.ui.TextHint
import com.fridgekeeper.tassetbook.ui.TextPrimary
import com.fridgekeeper.tassetbook.ui.TextSecondary
import com.fridgekeeper.tassetbook.ui.fmt2
import com.fridgekeeper.tassetbook.util.StockApi
import kotlinx.coroutines.delay
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(onBack: () -> Unit = {}) {
    val repo = AppRepository.get()
    val assets by repo.assetsFlow.collectAsState()
    val trades by repo.tradesFlow.collectAsState()
    val settings by repo.settingsFlow.collectAsState()

    // 联网获取现价
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

    val summary = remember(assets, trades, prices, settings) {
        repo.calcFinanceSummary(prices)
    }
    val marginEntries = remember(trades, settings) {
        repo.calcMarginEntries()
    }

    var debtExpanded by remember { mutableStateOf(false) }
    var withdrawInput by remember { mutableStateOf("") }
    var withdrawResult by remember { mutableStateOf<AppRepository.WithdrawEstimate?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("股票账户") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", modifier = Modifier.size(48.dp))
                    }
                }
            )
        }
    ) { scaffoldPadding ->
    Column(
        Modifier
            .fillMaxSize()
            .padding(scaffoldPadding)
            .background(BgGray)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // 汇总卡
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            colors = CardDefaults.cardColors(containerColor = PrimaryBlue),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                FinanceSummaryRow("净资产", fmt2(summary.netAssets) + " 元", Color.White)
                Spacer(Modifier.height(8.dp))
                FinanceSummaryRow("融资负债", fmt2(summary.marginDebt) + " 元", Color.White)
                Spacer(Modifier.height(8.dp))
                FinanceSummaryRow("总资产", fmt2(summary.totalAssets) + " 元", Color.White, bold = true)
                Spacer(Modifier.height(12.dp))
                val ratioText = if (summary.maintenanceRatio.isInfinite()) "∞" else "%.2f%%".format(summary.maintenanceRatio)
                FinanceSummaryRow("维持担保比例", ratioText,
                    if (summary.maintenanceRatio > 300) Color.White else Color(0xFFFFD54F))
                if (summary.marginDebt > 0) {
                    Spacer(Modifier.height(4.dp))
                    val status = when {
                        summary.maintenanceRatio > 300 -> "可取款（>300%）"
                        summary.maintenanceRatio > 150 -> "正常（>150%）"
                        summary.maintenanceRatio > 130 -> "警戒（>130%）"
                        else -> "危险（≤130%）"
                    }
                    val statusColor = when {
                        summary.maintenanceRatio > 300 -> Color(0xFF81C784)
                        summary.maintenanceRatio > 150 -> Color.White.copy(alpha = 0.85f)
                        else -> Color(0xFFFFD54F)
                    }
                    Text("状态：$status", fontSize = 12.sp, color = statusColor)
                }
            }
        }

        // 融资负债展开
        if (summary.marginDebt > 0 || marginEntries.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(1.dp),
                onClick = { debtExpanded = !debtExpanded }
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("融资负债明细", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.weight(1f))
                        Text(fmt2(summary.marginDebt) + " 元", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ProfitRed)
                        IconButton(onClick = { debtExpanded = !debtExpanded }) {
                            Icon(
                                if (debtExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = "展开/收起",
                                tint = TextHint
                            )
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("本金：${fmt2(summary.marginPrincipal)} 元", fontSize = 13.sp, color = TextSecondary)
                        Text("利息：${fmt2(summary.marginInterest)} 元", fontSize = 13.sp, color = TextSecondary)
                    }

                    AnimatedVisibility(
                        visible = debtExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(Modifier.padding(top = 8.dp)) {
                            if (marginEntries.isEmpty()) {
                                Text("暂无融资交易记录", fontSize = 13.sp, color = TextHint)
                            } else {
                                // 表头
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(BgGray, RoundedCornerShape(4.dp))
                                        .padding(vertical = 6.dp, horizontal = 8.dp)
                                ) {
                                    Text("股票", fontSize = 11.sp, color = TextHint, modifier = Modifier.weight(1f))
                                    Text("日期", fontSize = 11.sp, color = TextHint, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                    Text("天数", fontSize = 11.sp, color = TextHint, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                                    Text("本金", fontSize = 11.sp, color = TextHint, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                    Text("利息", fontSize = 11.sp, color = TextHint, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                    Text("合计", fontSize = 11.sp, color = TextHint, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                }
                                marginEntries.forEach { e ->
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp, horizontal = 8.dp)
                                    ) {
                                        Text(e.assetName, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1)
                                        Text(e.date, fontSize = 11.sp, color = TextSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                        Text("${e.days}天", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                                        Text(fmt2(e.principal), fontSize = 12.sp,
                                            color = if (e.principal >= 0) TextPrimary else LossGreen,
                                            modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                        Text(fmt2(e.interest), fontSize = 12.sp,
                                            color = if (e.interest >= 0) ProfitRed else LossGreen,
                                            modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                        Text(fmt2(e.total), fontSize = 12.sp,
                                            color = if (e.total >= 0) ProfitRed else LossGreen,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 预估取款
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("预估取款", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text("维持担保比例 > 300% 时可取出资金。输入取款金额，计算需卖出多少股票。", fontSize = 12.sp, color = TextHint)
                Spacer(Modifier.height(8.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactTextField(
                        value = withdrawInput,
                        onValueChange = { v -> withdrawInput = v.filter { it.isDigit() || it == '.' } },
                        modifier = Modifier.weight(1f),
                        placeholder = "取款金额",
                        keyboardType = KeyboardType.Decimal
                    )
                    Spacer(Modifier.width(8.dp))
                    androidx.compose.material3.Button(
                        onClick = {
                            val amount = withdrawInput.toDoubleOrNull() ?: 0.0
                            if (amount <= 0) {
                                withdrawResult = AppRepository.WithdrawEstimate(
                                    false, 0, 0.0, summary.maintenanceRatio, "请输入有效的取款金额"
                                )
                            } else {
                                withdrawResult = repo.estimateWithdraw(prices, amount)
                            }
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue
                        )
                    ) { Text("计算", color = Color.White) }
                }

                withdrawResult?.let { r ->
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                if (r.canWithdraw) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                if (r.canWithdraw) "✓ 可取款" else "✗ 不可取款",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (r.canWithdraw) LossGreen else ProfitRed
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(r.message, fontSize = 13.sp, color = TextPrimary)
                            if (r.canWithdraw) {
                                Spacer(Modifier.height(6.dp))
                                Text("需卖出股数：${r.sellShares} 股", fontSize = 13.sp, color = TextSecondary)
                                Text("卖出金额：${fmt2(r.sellAmount)} 元", fontSize = 13.sp, color = TextSecondary)
                                Text("取出后担保比例：%.2f%%".format(r.ratioAfter), fontSize = 13.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }

        // 各资产市值明细
        Spacer(Modifier.height(4.dp))
        Text("持仓市值明细", fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
        holdingAssets.forEach { asset ->
            val price = prices[asset.id]
            val marketValue = price?.let { it * asset.shares }
            val hasMargin = trades.any { it.assetId == asset.id && it.isMarginBuy && (it.buyShares > 0 || it.buyTotal > 0) }
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(asset.name, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            if (hasMargin) {
                                Spacer(Modifier.width(4.dp))
                                Text("融资", fontSize = 10.sp, color = Color.White,
                                    modifier = Modifier
                                        .background(ProfitRed, RoundedCornerShape(3.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp))
                            }
                        }
                        Text("${asset.shares}股 × ${price?.let { fmt2(it) } ?: "--"}", fontSize = 12.sp, color = TextSecondary)
                    }
                    Text(
                        marketValue?.let { fmt2(it) + " 元" } ?: "加载中...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PriceOrange
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun FinanceSummaryRow(label: String, value: String, valueColor: Color, bold: Boolean = false) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = valueColor.copy(alpha = 0.85f))
        Text(value, fontSize = if (bold) 20.sp else 16.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium, color = valueColor)
    }
}
