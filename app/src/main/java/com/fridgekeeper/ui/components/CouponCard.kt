package com.fridgekeeper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fridgekeeper.model.CouponItemUiState
import com.fridgekeeper.model.CouponSourceStat
import com.fridgekeeper.model.DateUtils
import com.fridgekeeper.model.WelfareUiState
import com.fridgekeeper.ui.theme.AlertRed
import com.fridgekeeper.ui.theme.AlertRedBg
import com.fridgekeeper.ui.theme.NormalGreen
import com.fridgekeeper.ui.theme.Primary

/** 顶部汇总：可用、临期、已使用、已过期张数 */
@Composable
fun CouponSummaryCard(state: WelfareUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Primary)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp)
        ) {
            SummaryMetric("可用券", "${state.unusedCount} 张", Modifier.weight(1f))
            SummaryMetric("3天内到期", "${state.expiringCount} 张", Modifier.weight(1f))
            SummaryMetric(
                "已使用",
                "${state.coupons.count { it.isUsed }} 张",
                Modifier.weight(1f)
            )
            SummaryMetric(
                "已过期",
                "${state.coupons.count { it.isExpired }} 张",
                Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, color = Color(0xB3FFFFFF), fontSize = 11.sp)
        Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** 按来源统计 */
@Composable
fun SourceStatRow(stats: List<CouponSourceStat>) {
    Column {
        Text(
            text = "来源统计",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF212121)
        )
        Spacer(Modifier.height(8.dp))
        stats.forEach { stat ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stat.source, fontSize = 13.sp, color = Color(0xFF424242))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "可用 ${stat.unusedCount}/${stat.totalCount}",
                    fontSize = 12.sp,
                    color = Color(0xFF9E9E9E)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "¥${formatLedgerAmount(stat.unusedAmount)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Primary
                )
            }
        }
    }
}

/** 优惠力度排行 */
@Composable
fun CouponRankList(coupons: List<CouponItemUiState>) {
    if (coupons.isEmpty()) return
    Column {
        Spacer(Modifier.height(4.dp))
        Text(
            text = "优惠力度 TOP${coupons.size}",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF212121)
        )
        Spacer(Modifier.height(6.dp))
        coupons.forEachIndexed { index, coupon ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${index + 1}",
                    fontSize = 12.sp,
                    color = Color.White,
                    modifier = Modifier
                        .background(
                            if (index == 0) AlertRed else Color(0xFFBDBDBD),
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${coupon.source} · ${coupon.type}",
                    fontSize = 13.sp,
                    color = Color(0xFF424242)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "¥${formatLedgerAmount(coupon.amount)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AlertRed
                )
            }
        }
    }
}

/** 单张优惠券卡片 */
@Composable
fun CouponCard(
    item: CouponItemUiState,
    onToggleUsed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val inactive = item.isUsed || item.isExpired
    val urgent = !item.isUsed && (item.isExpiringSoon || item.isExpired)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                item.isUsed -> Color(0xFFF5F5F5)
                urgent -> AlertRedBg
                else -> Color(0xFFFFFFFF)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isUsed) 0.dp else 2.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "¥${formatLedgerAmount(item.amount)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (inactive) Color(0xFF9E9E9E) else AlertRed
                )
                Text(
                    text = if (item.threshold > 0) "满${formatLedgerAmount(item.threshold)}可用" else "无门槛",
                    fontSize = 10.sp,
                    color = Color(0xFF9E9E9E)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.type,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (inactive) Color(0xFF757575) else Color(0xFF212121)
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE0F2F1), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(item.source, fontSize = 10.sp, color = Primary)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (item.scene.isBlank()) "—" else item.scene,
                    fontSize = 11.sp,
                    color = Color(0xFF757575)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = when {
                        item.isUsed -> "已使用"
                        item.isExpired -> "已过期 · ${DateUtils.formatDate(item.validUntil)}"
                        else -> "有效期至 ${DateUtils.formatDate(item.validUntil)}（剩 ${item.remainingDays} 天）"
                    },
                    fontSize = 11.sp,
                    fontWeight = if (urgent) FontWeight.SemiBold else FontWeight.Normal,
                    color = when {
                        item.isUsed -> Color(0xFF9E9E9E)
                        item.isExpired -> Color(0xFF9E9E9E)
                        urgent -> AlertRed
                        else -> NormalGreen
                    }
                )
            }

            Surface(
                onClick = onToggleUsed,
                shape = RoundedCornerShape(18.dp),
                color = if (item.isUsed) Color(0xFFE0E0E0) else Primary
            ) {
                Text(
                    text = if (item.isUsed) "恢复" else "使用",
                    color = if (item.isUsed) Color(0xFF616161) else Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}
