package com.fridgekeeper.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fridgekeeper.data.local.CheckinCycle
import com.fridgekeeper.model.CheckinItemUiState
import com.fridgekeeper.ui.theme.Primary
import kotlin.math.roundToInt

@Composable
fun AddCheckinDialog(
    editItem: CheckinItemUiState? = null,
    onDismiss: () -> Unit,
    onConfirm: (platform: String, name: String, cycle: String, remindHour: Int) -> Unit
) {
    var platform by remember { mutableStateOf(editItem?.platform ?: "") }
    var name by remember { mutableStateOf(editItem?.name ?: "") }
    var cycle by remember { mutableStateOf(editItem?.cycle ?: CheckinCycle.DAILY) }
    var remindHour by remember { mutableFloatStateOf((editItem?.remindHour ?: 9).toFloat()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editItem == null) "添加打卡" else "编辑打卡",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = platform,
                    onValueChange = { if (it.length <= 16) platform = it },
                    label = { Text("平台（如 云闪付）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 20) name = it },
                    label = { Text("打卡内容（如 会员每月礼包）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))
                Text("打卡周期", fontSize = 13.sp, color = Color(0xFF616161))
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CheckinCycle.ALL.forEach { item ->
                        FilterChip(
                            selected = cycle == item,
                            onClick = { cycle = item },
                            label = { Text(CheckinCycle.label(item)) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text = "提醒时间：${remindHour.roundToInt()} 点",
                    fontSize = 13.sp,
                    color = Color(0xFF616161)
                )
                Slider(
                    value = remindHour,
                    onValueChange = { remindHour = it },
                    valueRange = 0f..23f,
                    steps = 22
                )

                errorText?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = Color.Red, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        platform.isBlank() -> errorText = "请填写平台"
                        name.isBlank() -> errorText = "请填写打卡内容"
                        else -> onConfirm(
                            platform.trim(),
                            name.trim(),
                            cycle,
                            remindHour.roundToInt()
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(if (editItem == null) "添加" else "保存")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
