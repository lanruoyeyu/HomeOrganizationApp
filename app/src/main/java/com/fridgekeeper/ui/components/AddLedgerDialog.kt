package com.fridgekeeper.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fridgekeeper.data.local.LedgerEntryEntity
import com.fridgekeeper.model.DateUtils
import com.fridgekeeper.model.LedgerItemUiState
import com.fridgekeeper.ui.theme.AlertRed
import com.fridgekeeper.ui.theme.NormalGreen
import com.fridgekeeper.ui.theme.Primary

/**
 * 手动添加 / 编辑记账条目
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLedgerDialog(
    editItem: LedgerItemUiState? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        type: String,
        amount: Double,
        category: String,
        paymentMethod: String,
        note: String,
        occurredAt: Long
    ) -> Unit
) {
    var type by remember {
        mutableStateOf(editItem?.type ?: LedgerEntryEntity.TYPE_EXPENSE)
    }
    var category by remember {
        mutableStateOf(
            editItem?.category
                ?: if (type == LedgerEntryEntity.TYPE_INCOME) {
                    LedgerEntryEntity.INCOME_CATEGORIES.first()
                } else {
                    LedgerEntryEntity.EXPENSE_CATEGORIES.first()
                }
        )
    }
    var amountText by remember {
        mutableStateOf(editItem?.amount?.let { formatLedgerAmount(it) } ?: "")
    }
    var amountError by remember { mutableStateOf(false) }
    var payment by remember { mutableStateOf(editItem?.paymentMethod ?: "其他") }
    var note by remember { mutableStateOf(editItem?.note ?: "") }
    var occurredAt by remember {
        mutableLongStateOf(editItem?.occurredAt ?: DateUtils.todayStartMillis())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var paymentExpanded by remember { mutableStateOf(false) }

    val categories = if (type == LedgerEntryEntity.TYPE_INCOME) {
        LedgerEntryEntity.INCOME_CATEGORIES
    } else {
        LedgerEntryEntity.EXPENSE_CATEGORIES
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editItem == null) "记一笔" else "编辑记录",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 收 / 支
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == LedgerEntryEntity.TYPE_EXPENSE,
                        onClick = {
                            if (type != LedgerEntryEntity.TYPE_EXPENSE) {
                                type = LedgerEntryEntity.TYPE_EXPENSE
                                category = LedgerEntryEntity.EXPENSE_CATEGORIES.first()
                            }
                        },
                        label = { Text("支出") }
                    )
                    FilterChip(
                        selected = type == LedgerEntryEntity.TYPE_INCOME,
                        onClick = {
                            if (type != LedgerEntryEntity.TYPE_INCOME) {
                                type = LedgerEntryEntity.TYPE_INCOME
                                category = LedgerEntryEntity.INCOME_CATEGORIES.first()
                            }
                        },
                        label = { Text("收入") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        if (it.length <= 10 && it.matches(Regex("""\d*\.?\d{0,2}"""))) {
                            amountText = it
                            amountError = false
                        }
                    },
                    label = { Text("金额（元）") },
                    isError = amountError,
                    supportingText = { if (amountError) Text("请输入有效金额", color = AlertRed) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("分类") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    category = option
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = paymentExpanded,
                    onExpandedChange = { paymentExpanded = !paymentExpanded }
                ) {
                    OutlinedTextField(
                        value = payment,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("支付方式") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = paymentExpanded,
                        onDismissRequest = { paymentExpanded = false }
                    ) {
                        LedgerEntryEntity.PAYMENT_METHODS.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    payment = option
                                    paymentExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("日期：${DateUtils.formatDate(occurredAt)}", fontSize = 14.sp)
                        Text("选择", fontSize = 13.sp, color = Primary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 40) note = it },
                    label = { Text("备注（选填）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        amountError = true
                        return@Button
                    }
                    onConfirm(
                        type,
                        amount,
                        category,
                        payment,
                        note.trim(),
                        occurredAt
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == LedgerEntryEntity.TYPE_INCOME) NormalGreen else Primary
                )
            ) { Text(if (editItem == null) "保存" else "更新") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = occurredAt)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { occurredAt = it }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) { DatePicker(state = state) }
    }
}

internal fun formatLedgerAmount(value: Double): String =
    if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(java.util.Locale.CHINA, "%.2f", value)
    }

internal fun ledgerEmoji(category: String): String = when (category) {
    "餐饮" -> "🍜"
    "交通" -> "🚕"
    "购物" -> "🛍️"
    "居住" -> "🏠"
    "娱乐" -> "🎬"
    "医疗" -> "💊"
    "教育" -> "📚"
    "人情" -> "🎁"
    "工资" -> "💼"
    "奖金" -> "🏆"
    "理财" -> "📈"
    "报销" -> "🧾"
    "退款" -> "↩️"
    "兼职" -> "💪"
    else -> "📝"
}

internal val LedgerExpenseColor = AlertRed
internal val LedgerIncomeColor = NormalGreen
