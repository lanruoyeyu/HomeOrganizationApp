package com.fridgekeeper.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fridgekeeper.data.local.UsageRecordEntity
import com.fridgekeeper.model.DateUtils
import com.fridgekeeper.ui.theme.Primary

/**
 * 卡片上的「使用」「记录」两个操作按钮
 */
@Composable
fun UsageActionButtons(
    onUse: () -> Unit,
    onHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            onClick = onUse,
            shape = RoundedCornerShape(20.dp),
            color = Primary
        ) {
            Text(
                text = "使用",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
        Surface(
            onClick = onHistory,
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFE0E0E0)
        ) {
            Text(
                text = "记录",
                color = Color(0xFF616161),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

/**
 * 使用弹窗：填写使用情况、使用时间（默认今天）、备注场景
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UseItemDialog(
    itemName: String,
    onDismiss: () -> Unit,
    onConfirm: (usageText: String, usedAt: Long, scene: String) -> Unit
) {
    var usage by remember { mutableStateOf("") }
    var usageError by remember { mutableStateOf(false) }
    var usedAt by remember { mutableLongStateOf(DateUtils.todayStartMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var scene by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("使用 - $itemName", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = usage,
                    onValueChange = {
                        if (it.length <= 50) {
                            usage = it
                            usageError = false
                        }
                    },
                    label = { Text("使用情况") },
                    isError = usageError,
                    supportingText = { if (usageError) Text("请填写使用情况", color = Color.Red) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "使用时间：${DateUtils.formatDate(usedAt)}",
                        modifier = Modifier.weight(1f)
                    )
                    Text("📅", fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = scene,
                    onValueChange = { if (it.length <= 50) scene = it },
                    label = { Text("备注场景（选填）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (usage.isBlank()) {
                        usageError = true
                    } else {
                        onConfirm(usage.trim(), usedAt, scene.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) { Text("确认") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.clampToPickerRange(usedAt)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { usedAt = it }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } }
        ) { DatePicker(state = state) }
    }
}

/**
 * 使用记录历史弹窗
 */
@Composable
fun UsageHistoryDialog(
    itemName: String,
    records: List<UsageRecordEntity>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("使用记录 - $itemName", style = MaterialTheme.typography.titleLarge) },
        text = {
            if (records.isEmpty()) {
                Text(
                    text = "暂无使用记录",
                    fontSize = 14.sp,
                    color = Color(0xFF888888)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                ) {
                    items(records, key = { it.id }) { record ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = DateUtils.formatDate(record.usedAt),
                                fontSize = 12.sp,
                                color = Color(0xFF888888)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = record.usageText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF212121)
                            )
                            if (record.scene.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "场景：${record.scene}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF888888)
                                )
                            }
                        }
                        HorizontalDivider(color = Color(0xFFEEEEEE))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}
