package com.fridgekeeper.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.fridgekeeper.model.CouponItemUiState
import com.fridgekeeper.model.DateUtils
import com.fridgekeeper.ui.theme.Primary
import java.util.Calendar

data class CouponDraft(
    val source: String,
    val type: String,
    val threshold: Double,
    val amount: Double,
    val validFrom: Long?,
    val validUntil: Long,
    val scene: String,
    val note: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCouponDialog(
    editItem: CouponItemUiState? = null,
    sourceOptions: List<String> = emptyList(),
    typeOptions: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (CouponDraft) -> Unit
) {
    var source by remember {
        mutableStateOf(
            editItem?.source?.takeIf { it.isNotBlank() }
                ?: sourceOptions.firstOrNull().orEmpty()
        )
    }
    var type by remember {
        mutableStateOf(
            editItem?.type?.takeIf { it.isNotBlank() }
                ?: typeOptions.firstOrNull().orEmpty()
        )
    }
    val allSources = (sourceOptions + source).filter { it.isNotBlank() }.distinct()
    val allTypes = (typeOptions + type).filter { it.isNotBlank() }.distinct()
    var threshold by remember {
        mutableStateOf(editItem?.threshold?.takeIf { it > 0 }?.let(::formatPlain) ?: "")
    }
    var amount by remember { mutableStateOf(editItem?.amount?.let(::formatPlain) ?: "") }
    var scene by remember { mutableStateOf(editItem?.scene ?: "") }
    var note by remember { mutableStateOf(editItem?.note ?: "") }
    var validFrom by remember {
        mutableStateOf(editItem?.validFrom)
    }
    var validUntil by remember {
        mutableLongStateOf(editItem?.validUntil ?: defaultExpiry())
    }
    var showFromPicker by remember { mutableStateOf(false) }
    var showUntilPicker by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editItem == null) "添加优惠券" else "编辑优惠券",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OptionDropdown(
                    label = "来源",
                    value = source,
                    options = allSources,
                    onSelect = { source = it }
                )

                Spacer(Modifier.height(10.dp))
                OptionDropdown(
                    label = "类型",
                    value = type,
                    options = allTypes,
                    onSelect = { type = it }
                )

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = threshold,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(AMOUNT_REGEX)) threshold = input
                        },
                        label = { Text("满多少可用") },
                        placeholder = { Text("0 = 无门槛") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(AMOUNT_REGEX)) amount = input
                        },
                        label = { Text("优惠金额") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showFromPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "生效日期：${validFrom?.let(DateUtils::formatDate) ?: "不限"}",
                        modifier = Modifier.weight(1f)
                    )
                    if (validFrom != null) {
                        TextButton(onClick = { validFrom = null }) { Text("清除", fontSize = 12.sp) }
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showUntilPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "到期日期：${DateUtils.formatDate(validUntil)}",
                        modifier = Modifier.weight(1f)
                    )
                    Text("📅", fontSize = 14.sp)
                }

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = scene,
                    onValueChange = { if (it.length <= 24) scene = it },
                    label = { Text("使用场景") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 40) note = it },
                    label = { Text("备注（选填）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                errorText?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = Color.Red, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.trim().toDoubleOrNull()
                    val thresholdValue = threshold.trim().toDoubleOrNull() ?: 0.0
                    when {
                        source.isBlank() -> errorText = "请选择来源"
                        type.isBlank() -> errorText = "请选择类型"
                        amountValue == null || amountValue <= 0 -> errorText = "请填写有效的优惠金额"
                        thresholdValue < 0 -> errorText = "使用条件不能为负数"
                        else -> onConfirm(
                            CouponDraft(
                                source = source.trim(),
                                type = type.trim(),
                                threshold = thresholdValue,
                                amount = amountValue,
                                validFrom = validFrom,
                                validUntil = validUntil,
                                scene = scene.trim(),
                                note = note.trim()
                            )
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

    if (showFromPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.clampToPickerRange(
                validFrom ?: DateUtils.todayStartMillis()
            )
        )
        DatePickerDialog(
            onDismissRequest = { showFromPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { validFrom = it }
                    showFromPicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showFromPicker = false }) { Text("取消") }
            }
        ) { DatePicker(state = state) }
    }

    if (showUntilPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.clampToPickerRange(validUntil)
        )
        DatePickerDialog(
            onDismissRequest = { showUntilPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { validUntil = it }
                    showUntilPicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showUntilPicker = false }) { Text("取消") }
            }
        ) { DatePicker(state = state) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text("请选择$label") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

private val AMOUNT_REGEX = Regex("^\\d*(\\.\\d{0,2})?$")

private fun formatPlain(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

private fun defaultExpiry(): Long {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}
