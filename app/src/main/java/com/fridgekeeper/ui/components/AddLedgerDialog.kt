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
import com.fridgekeeper.data.ledger.LedgerTaxonomy
import com.fridgekeeper.data.local.LedgerEntryEntity
import com.fridgekeeper.model.DateUtils
import com.fridgekeeper.model.LedgerDraft
import com.fridgekeeper.model.LedgerItemUiState
import com.fridgekeeper.ui.theme.AlertRed
import com.fridgekeeper.ui.theme.NormalGreen
import com.fridgekeeper.ui.theme.Primary

/**
 * 手动添加 / 编辑记账条目，字段与 iCost 记账模板保持一致
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLedgerDialog(
    editItem: LedgerItemUiState? = null,
    onDismiss: () -> Unit,
    onConfirm: (LedgerDraft) -> Unit
) {
    var type by remember {
        mutableStateOf(editItem?.type ?: LedgerEntryEntity.TYPE_EXPENSE)
    }
    var category by remember {
        mutableStateOf(
            editItem?.category ?: LedgerTaxonomy.categoriesFor(type).first()
        )
    }
    var subcategory by remember { mutableStateOf(editItem?.subcategory.orEmpty()) }
    var amountText by remember {
        mutableStateOf(editItem?.amount?.let { formatLedgerAmount(it) } ?: "")
    }
    var amountError by remember { mutableStateOf(false) }

    // 账户选项来自「资产」中允许记账选择的账户，未配置时回退到默认选项
    val assetAccounts = remember {
        com.fridgekeeper.tassetbook.data.AppRepository.get().ledgerSelectableAccountNames()
    }
    val baseAccounts = assetAccounts.ifEmpty { LedgerTaxonomy.ACCOUNTS }

    var account by remember {
        mutableStateOf(
            editItem?.paymentMethod?.ifBlank { baseAccounts.first() } ?: baseAccounts.first()
        )
    }
    var account2 by remember { mutableStateOf(editItem?.account2.orEmpty()) }
    var tags by remember { mutableStateOf(editItem?.tags.orEmpty()) }
    var book by remember {
        mutableStateOf(
            editItem?.book?.ifBlank { LedgerTaxonomy.DEFAULT_BOOK }
                ?: LedgerTaxonomy.DEFAULT_BOOK
        )
    }
    var typeLabel by remember { mutableStateOf(editItem?.typeLabel.orEmpty()) }
    var note by remember { mutableStateOf(editItem?.note ?: "") }
    var occurredAt by remember {
        mutableLongStateOf(editItem?.occurredAt ?: DateUtils.todayStartMillis())
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val baseCategories = LedgerTaxonomy.categoriesFor(type)
    val categories = if (category.isBlank() || category in baseCategories) {
        baseCategories
    } else {
        listOf(category) + baseCategories
    }
    val subcategoryOptions =
        listOf(LedgerTaxonomy.EMPTY_OPTION) + LedgerTaxonomy.subcategoriesFor(category)
    val accountOptions = if (account.isBlank() || account in baseAccounts) {
        baseAccounts
    } else {
        listOf(account) + baseAccounts
    }
    val account2Options = if (account2.isBlank() || account2 in baseAccounts) {
        baseAccounts
    } else {
        listOf(account2) + baseAccounts
    }
    val bookOptions = if (book in LedgerTaxonomy.BOOKS) {
        LedgerTaxonomy.BOOKS
    } else {
        listOf(book) + LedgerTaxonomy.BOOKS
    }

    fun selectType(newType: String) {
        if (type == newType) return
        type = newType
        typeLabel = ""
        category = LedgerTaxonomy.categoriesFor(newType).first()
        subcategory = ""
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == LedgerEntryEntity.TYPE_EXPENSE,
                        onClick = { selectType(LedgerEntryEntity.TYPE_EXPENSE) },
                        label = { Text("支出") }
                    )
                    FilterChip(
                        selected = type == LedgerEntryEntity.TYPE_INCOME,
                        onClick = { selectType(LedgerEntryEntity.TYPE_INCOME) },
                        label = { Text("收入") }
                    )
                    FilterChip(
                        selected = type == LedgerEntryEntity.TYPE_TRANSFER,
                        onClick = { selectType(LedgerEntryEntity.TYPE_TRANSFER) },
                        label = { Text("转账") }
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

                Spacer(modifier = Modifier.height(10.dp))

                LedgerDropdown(
                    label = "一级分类",
                    value = category,
                    options = categories,
                    onSelect = {
                        category = it
                        subcategory = ""
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                LedgerDropdown(
                    label = "二级分类",
                    value = subcategory.ifBlank { LedgerTaxonomy.EMPTY_OPTION },
                    options = subcategoryOptions,
                    onSelect = {
                        subcategory = if (it == LedgerTaxonomy.EMPTY_OPTION) "" else it
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                LedgerDropdown(
                    label = if (type == LedgerEntryEntity.TYPE_TRANSFER) "转出账户" else "账户",
                    value = account.ifBlank { "其他" },
                    options = accountOptions,
                    onSelect = { account = it }
                )

                if (type == LedgerEntryEntity.TYPE_TRANSFER) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LedgerDropdown(
                        label = "转入账户",
                        value = account2.ifBlank { LedgerTaxonomy.EMPTY_OPTION },
                        options = account2Options,
                        onSelect = {
                            account2 = if (it == LedgerTaxonomy.EMPTY_OPTION) "" else it
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LedgerDropdown(
                    label = "账本",
                    value = book,
                    options = bookOptions,
                    onSelect = { book = it }
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = tags,
                    onValueChange = { if (it.length <= 20) tags = it },
                    label = { Text("标签（选填）") },
                    placeholder = { Text("#淘宝", fontSize = 13.sp, color = Color(0xFFAAAAAA)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

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
                        LedgerDraft(
                            type = type,
                            amount = amount,
                            category = category,
                            subcategory = subcategory,
                            account = account,
                            account2 = account2,
                            tags = tags.trim(),
                            book = book,
                            typeLabel = typeLabel.ifBlank {
                                LedgerTaxonomy.templateTypeOf(type)
                            },
                            note = note.trim(),
                            occurredAt = occurredAt
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (type) {
                        LedgerEntryEntity.TYPE_INCOME -> NormalGreen
                        LedgerEntryEntity.TYPE_TRANSFER -> Color(0xFF757575)
                        else -> Primary
                    }
                )
            ) { Text(if (editItem == null) "保存" else "更新") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.clampToPickerRange(occurredAt)
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LedgerDropdown(
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
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
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

internal fun formatLedgerAmount(value: Double): String =
    if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(java.util.Locale.CHINA, "%.2f", value)
    }

internal fun ledgerEmoji(category: String): String = when (category) {
    "餐饮" -> "🍜"
    "日用" -> "🧴"
    "宠物" -> "🐾"
    "交通" -> "🚕"
    "住房" -> "🏠"
    "服饰" -> "👕"
    "保险" -> "🛡️"
    "汽车" -> "🚗"
    "数码" -> "📱"
    "投资" -> "📈"
    "社交" -> "🎁"
    "医疗" -> "💊"
    "通讯" -> "📞"
    "学习" -> "📚"
    "卫浴" -> "🚿"
    "家具" -> "🛋️"
    "装饰" -> "🖼️"
    "家装建材" -> "🧱"
    "亏损" -> "📉"
    "分期本金" -> "💳"
    "分期利息" -> "📊"
    "还款手续费" -> "🧾"
    "还贷" -> "🏦"
    "其他支出" -> "📝"
    "工资" -> "💼"
    "其他收入" -> "💰"
    "转账" -> "🔄"
    "还款" -> "✅"
    "充值" -> "⬆️"
    "分期创建" -> "🧩"
    else -> "📝"
}

internal val LedgerExpenseColor = AlertRed
internal val LedgerIncomeColor = NormalGreen
internal val LedgerTransferColor = Color(0xFF757575)
