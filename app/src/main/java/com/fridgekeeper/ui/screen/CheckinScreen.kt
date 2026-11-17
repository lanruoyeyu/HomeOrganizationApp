package com.fridgekeeper.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fridgekeeper.model.CheckinItemUiState
import com.fridgekeeper.model.CheckinWeekDayUiState
import com.fridgekeeper.model.WelfareUiState
import com.fridgekeeper.ui.components.AddCheckinDialog
import com.fridgekeeper.ui.components.CheckinCard
import com.fridgekeeper.ui.components.CheckinListItem
import com.fridgekeeper.ui.theme.AlertRed
import com.fridgekeeper.ui.theme.LocalSpacingScale
import com.fridgekeeper.ui.theme.NormalGreen
import com.fridgekeeper.ui.theme.Primary
import com.fridgekeeper.viewmodel.WelfareViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckinScreen(
    viewModel: WelfareViewModel,
    onOpenDrawer: () -> Unit,
    circleColor: Color,
    asList: Boolean,
    onToggleList: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val spacingScale = LocalSpacingScale.current
    var showAddCheckin by remember { mutableStateOf(false) }
    var editCheckin by remember { mutableStateOf<CheckinItemUiState?>(null) }
    var manageCheckin by remember { mutableStateOf<CheckinItemUiState?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("打卡", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "打开设置", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onToggleList) {
                        Icon(
                            imageVector = if (asList) Icons.Filled.GridView else Icons.Filled.ViewList,
                            contentDescription = if (asList) "切换为圆圈显示" else "切换为列表显示",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCheckin = true },
                containerColor = Primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "添加打卡", tint = Color.White)
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (asList) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 12.dp * spacingScale,
                    bottom = 92.dp
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp * spacingScale)
            ) {
                item { CheckinWeekSummary(uiState, circleColor = circleColor) }
                item { CheckinSectionHeader(uiState, asList = true) }
                if (uiState.checkins.isEmpty()) {
                    item { EmptyCheckinHint() }
                } else {
                    listItems(items = uiState.checkins, key = { it.id }) { task ->
                        CheckinListItem(
                            item = task,
                            circleColor = circleColor,
                            onToggle = { viewModel.toggleCheckIn(task.id) },
                            onManage = { manageCheckin = task }
                        )
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 12.dp * spacingScale,
                    bottom = 92.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(4.dp * spacingScale),
                verticalArrangement = Arrangement.spacedBy(12.dp * spacingScale)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    CheckinWeekSummary(uiState, circleColor = circleColor)
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    CheckinSectionHeader(uiState, asList = false)
                }

                if (uiState.checkins.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyCheckinHint()
                    }
                } else {
                    gridItems(items = uiState.checkins, key = { it.id }) { task ->
                        CheckinCard(
                            item = task,
                            circleColor = circleColor,
                            onToggle = { viewModel.toggleCheckIn(task.id) },
                            onManage = { manageCheckin = task }
                        )
                    }
                }
            }
        }
    }

    if (showAddCheckin) {
        AddCheckinDialog(
            onDismiss = { showAddCheckin = false },
            onConfirm = { platform, name, cycle, remindHour ->
                viewModel.addTask(platform, name, cycle, remindHour)
                showAddCheckin = false
            }
        )
    }

    editCheckin?.let { target ->
        AddCheckinDialog(
            editItem = target,
            onDismiss = { editCheckin = null },
            onConfirm = { platform, name, cycle, remindHour ->
                viewModel.updateTask(target.id, platform, name, cycle, remindHour)
                editCheckin = null
            }
        )
    }

    manageCheckin?.let { target ->
        AlertDialog(
            onDismissRequest = { manageCheckin = null },
            title = { Text(target.name) },
            text = { Text("${target.platform} · ${target.cycleLabel}\n累计打卡 ${target.totalCheckins} 次") },
            confirmButton = {
                TextButton(
                    onClick = {
                        editCheckin = target
                        manageCheckin = null
                    }
                ) {
                    Text("编辑")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTask(target.id)
                        manageCheckin = null
                    }
                ) {
                    Text("删除", color = AlertRed)
                }
            }
        )
    }
}

@Composable
private fun CheckinSectionHeader(state: WelfareUiState, asList: Boolean) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "打卡项目（${state.checkins.size}）",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF212121),
                modifier = Modifier.weight(1f)
            )
            if (state.checkins.isNotEmpty()) {
                Text(
                    text = if (state.todayPendingCheckins > 0) {
                        "还有 ${state.todayPendingCheckins} 项待打卡"
                    } else {
                        "已全部完成"
                    },
                    fontSize = 12.sp,
                    color = if (state.todayPendingCheckins > 0) AlertRed else NormalGreen
                )
            }
        }
        if (state.checkins.isNotEmpty()) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = if (asList) {
                    "点击条目打卡或取消，长按条目编辑或删除"
                } else {
                    "点击圆圈打卡或取消，长按圆圈编辑或删除"
                },
                fontSize = 11.sp,
                color = Color(0xFF888888)
            )
        }
    }
}

@Composable
private fun EmptyCheckinHint() {
    Text(
        text = "暂无打卡项目\n点击右下角 + 添加",
        fontSize = 15.sp,
        color = Color(0xFF9E9E9E),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp)
    )
}

@Composable
private fun CheckinWeekSummary(state: WelfareUiState, circleColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${state.weekActiveDays}/7 天",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    color = circleColor
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "共 ${state.weekCheckinCount} 次",
                    fontSize = 12.sp,
                    color = Color(0xFF616161)
                )
            }

            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                state.weekDays.forEach { day ->
                    CheckinDay(day, circleColor = circleColor, modifier = Modifier.weight(1f))
                }
            }

        }
    }
}

@Composable
private fun CheckinDay(
    day: CheckinWeekDayUiState,
    circleColor: Color,
    modifier: Modifier = Modifier
) {
    val hasCheckin = day.checkinCount > 0
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(day.weekdayLabel, fontSize = 11.sp, color = Color(0xFF757575))
        Spacer(Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = when {
                        hasCheckin -> circleColor
                        day.isToday -> circleColor.copy(alpha = 0.12f)
                        else -> Color(0xFFF5F5F5)
                    },
                    shape = CircleShape
                )
                .border(
                    width = if (day.isToday && !hasCheckin) 1.dp else 0.dp,
                    color = if (day.isToday) circleColor else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.dayOfMonth.toString(),
                fontSize = 12.sp,
                fontWeight = if (day.isToday || hasCheckin) FontWeight.Bold else FontWeight.Normal,
                color = if (hasCheckin) Color.White else Color(0xFF424242)
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = if (hasCheckin) "${day.checkinCount} 次" else " ",
            fontSize = 9.sp,
            color = circleColor,
            textAlign = TextAlign.Center
        )
    }
}
