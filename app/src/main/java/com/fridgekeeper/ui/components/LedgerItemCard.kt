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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fridgekeeper.model.DateUtils
import com.fridgekeeper.model.LedgerItemUiState

@Composable
fun LedgerItemCard(
    item: LedgerItemUiState,
    modifier: Modifier = Modifier
) {
    val amountColor = if (item.isExpense) LedgerExpenseColor else LedgerIncomeColor
    val sign = if (item.isExpense) "-" else "+"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = ledgerEmoji(item.category), fontSize = 26.sp)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.category,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF212121)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFF0F0F0)
                    ) {
                        Text(
                            text = item.paymentMethod,
                            fontSize = 10.sp,
                            color = Color(0xFF888888),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                if (item.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.note,
                        fontSize = 13.sp,
                        color = Color(0xFF616161)
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = DateUtils.formatDate(item.occurredAt),
                    fontSize = 11.sp,
                    color = Color(0xFF9E9E9E)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$sign${formatLedgerAmount(item.amount)}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                Text(
                    text = if (item.isExpense) "支出" else "收入",
                    fontSize = 11.sp,
                    color = Color(0xFF9E9E9E)
                )
            }
        }
    }
}

@Composable
fun LedgerMonthSummary(
    monthExpense: Double,
    monthIncome: Double,
    balance: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF00695C))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "本月结余",
                fontSize = 12.sp,
                color = Color(0xFFB2DFDB)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = (if (balance >= 0) "¥${formatLedgerAmount(balance)}" else "-¥${formatLedgerAmount(-balance)}"),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("本月支出", fontSize = 11.sp, color = Color(0xFFB2DFDB))
                    Text(
                        text = "¥${formatLedgerAmount(monthExpense)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("本月收入", fontSize = 11.sp, color = Color(0xFFB2DFDB))
                    Text(
                        text = "¥${formatLedgerAmount(monthIncome)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFA5D6A7)
                    )
                }
            }
        }
    }
}
