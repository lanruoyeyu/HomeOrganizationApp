package com.fridgekeeper.ui.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fridgekeeper.model.DateUtils
import com.fridgekeeper.model.MedicineItemUiState
import com.fridgekeeper.ui.theme.AlertRed
import com.fridgekeeper.ui.theme.AlertRedBg
import com.fridgekeeper.ui.theme.NormalGreen
import com.fridgekeeper.ui.theme.NormalGreenBg
import java.util.Locale

/**
 * 药品卡片：未过期显示浅绿色，过期或距有效期不足 10 天显示红色
 */
@Composable
fun MedicineItemCard(
    item: MedicineItemUiState,
    onUse: () -> Unit,
    onHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAlert = item.isExpired || item.isExpiringSoon
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAlert) AlertRedBg else NormalGreenBg
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "💊", fontSize = 28.sp)

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAlert) AlertRed else Color(0xFF1B5E20)
                        )
                        item.price?.let {
                            Text(
                                text = "¥${String.format(Locale.CHINA, "%.2f", it)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isAlert) AlertRed else Color(0xFF2E7D32)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "有效期至：${DateUtils.formatDate(item.expiryDate)}",
                        fontSize = 13.sp,
                        color = Color(0xFF616161)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = when {
                            item.isExpired -> "已过期"
                            item.remainingDays == 1 -> "有效期剩余：1天"
                            else -> "有效期剩余：${item.remainingDays}天"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isAlert) AlertRed else NormalGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                UsageActionButtons(onUse = onUse, onHistory = onHistory)
            }
        }
    }
}
