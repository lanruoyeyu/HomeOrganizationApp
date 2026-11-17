package com.fridgekeeper.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fridgekeeper.model.CheckinItemUiState
import com.fridgekeeper.ui.theme.LocalSpacingScale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CheckinCard(
    item: CheckinItemUiState,
    circleColor: Color,
    onToggle: () -> Unit,
    onManage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val done = item.checkedInThisPeriod
    val spacingScale = LocalSpacingScale.current

    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp * spacingScale),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(if (done) circleColor else Color.White)
                .border(BorderStroke(2.dp, circleColor), CircleShape)
                .combinedClickable(
                    onClick = onToggle,
                    onLongClick = onManage
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.name,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (done) Color.White else Color(0xFF212121),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = item.platform,
            fontSize = 12.sp,
            color = Color(0xFF616161),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "${item.cycleLabel} · 本周 ${item.weeklyCheckins} 次",
            fontSize = 10.sp,
            color = Color(0xFF888888),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** 列表显示方式的打卡条目，点击切换打卡，长按管理 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CheckinListItem(
    item: CheckinItemUiState,
    circleColor: Color,
    onToggle: () -> Unit,
    onManage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val done = item.checkedInThisPeriod
    val spacingScale = LocalSpacingScale.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp * spacingScale)
            .combinedClickable(onClick = onToggle, onLongClick = onManage),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(if (done) circleColor else Color.White, CircleShape)
                    .border(2.dp, circleColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (done) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF212121),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${item.platform} · ${item.cycleLabel}",
                    fontSize = 11.sp,
                    color = Color(0xFF616161),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = "本周 ${item.weeklyCheckins} 次 · 累计 ${item.totalCheckins} 次",
                    fontSize = 10.sp,
                    color = Color(0xFF888888),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (done) "已打卡" else "待打卡",
                fontSize = 12.sp,
                fontWeight = if (done) FontWeight.Medium else FontWeight.Normal,
                color = if (done) circleColor else Color(0xFF9E9E9E)
            )
        }
    }
}
