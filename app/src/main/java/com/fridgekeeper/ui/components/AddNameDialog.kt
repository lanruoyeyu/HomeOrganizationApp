package com.fridgekeeper.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fridgekeeper.ui.theme.Primary

/**
 * 通用名称输入弹窗：用于添加地图位置、物品大类、小类
 */
@Composable
fun AddNameDialog(
    title: String,
    label: String,
    hint: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, onResult: (Boolean) -> Unit) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = hint,
                    fontSize = 13.sp,
                    color = Color(0xFF888888)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        if (it.length <= 10) {
                            name = it
                            hasError = false
                        }
                    },
                    label = { Text(label) },
                    isError = hasError,
                    supportingText = {
                        if (hasError) Text("名称为空或已存在", color = Color.Red)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = name.trim()
                if (trimmed.isEmpty()) {
                    hasError = true
                } else {
                    onConfirm(trimmed) { success ->
                        if (success) onDismiss() else hasError = true
                    }
                }
            }) { Text("确定", color = Primary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
