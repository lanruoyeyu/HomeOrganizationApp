package com.fridgekeeper.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fridgekeeper.model.DateUtils
import com.fridgekeeper.model.DigitalItemUiState
import com.fridgekeeper.ui.theme.Primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDigitalItemDialog(
    locations: List<com.fridgekeeper.data.local.LocationEntity> = emptyList(),
    editItem: DigitalItemUiState? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, purchasePrice: Double, purchaseDate: Long, locationId: Long?) -> Unit
) {
    var name by remember { mutableStateOf(editItem?.name ?: "") }
    var nameError by remember { mutableStateOf(false) }
    var price by remember {
        mutableStateOf(editItem?.purchasePrice?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
    }
    var priceError by remember { mutableStateOf(false) }

    var purchaseDate by remember { mutableLongStateOf(editItem?.purchaseDate ?: DateUtils.todayStartMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var locationId by remember { mutableStateOf<Long?>(editItem?.locationId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editItem == null) "添加数码产品" else "编辑数码产品", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 物品名称
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        if (it.length <= 20) {
                            name = it
                            nameError = false
                        }
                    },
                    label = { Text("物品名称") },
                    isError = nameError,
                    supportingText = {
                        if (nameError) Text("请输入物品名称", color = Color.Red)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 买入价格
                OutlinedTextField(
                    value = price,
                    onValueChange = {
                        // 允许最多 2 位小数
                        if (it.isEmpty() || it.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                            price = it
                            priceError = false
                        }
                    },
                    label = { Text("买入价格（元）") },
                    isError = priceError,
                    supportingText = {
                        if (priceError) Text("请输入有效的买入价格", color = Color.Red)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 买入时间
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "买入时间：${DateUtils.formatDate(purchaseDate)}",
                        modifier = Modifier.weight(1f)
                    )
                    Text("📅", fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))
                LocationDropdown(locations, locationId, onSelected = { locationId = it })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    var hasError = false
                    if (name.isBlank()) {
                        nameError = true
                        hasError = true
                    }
                    val priceValue = price.toDoubleOrNull()
                    if (priceValue == null || priceValue <= 0) {
                        priceError = true
                        hasError = true
                    }
                    if (!hasError) {
                        onConfirm(name.trim(), priceValue!!, purchaseDate, locationId)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(if (editItem == null) "确认添加" else "保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.clampToPickerRange(purchaseDate)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { purchaseDate = it }
                        showDatePicker = false
                    }
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}
