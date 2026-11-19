package com.fridgekeeper.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fridgekeeper.data.ledger.LedgerTaxonomy
import com.fridgekeeper.data.local.LedgerEntryEntity
import com.fridgekeeper.model.DateUtils
import com.fridgekeeper.model.LedgerDraft
import com.fridgekeeper.model.LedgerItemUiState
import com.fridgekeeper.ui.components.formatLedgerAmount
import com.fridgekeeper.ui.components.ledgerEmoji
import com.fridgekeeper.ui.theme.AlertRed
import com.fridgekeeper.ui.theme.Background
import com.fridgekeeper.ui.theme.NormalGreen
import com.fridgekeeper.ui.theme.Primary

/**
 * 记账编辑页：仿照「记账.jpg」的版式，全屏展示。
 * 顶部为类型 / 账本切换，中间是分类网格与字段，底部是金额与数字键盘。
 * 所有文字使用 sp，随系统字体大小缩放。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerEditScreen(
    editItem: LedgerItemUiState? = null,
    onDismiss: () -> Unit,
    onConfirm: (LedgerDraft) -> Unit
) {
    val isEdit = editItem != null

    var type by remember { mutableStateOf(editItem?.type ?: LedgerEntryEntity.TYPE_EXPENSE) }
    var category by remember {
        mutableStateOf(editItem?.category ?: LedgerTaxonomy.categoriesFor(type).first())
    }
    var subcategory by remember { mutableStateOf(editItem?.subcategory.orEmpty()) }
    var amountText by remember {
        mutableStateOf(editItem?.amount?.let { formatLedgerAmount(it) } ?: "")
    }
    var account by remember {
        mutableStateOf(editItem?.paymentMethod?.ifBlank { null } ?: defaultAccount())
    }
    var account2 by remember { mutableStateOf(editItem?.account2.orEmpty()) }
    var tags by remember { mutableStateOf(editItem?.tags.orEmpty()) }
    var book by remember {
        mutableStateOf(editItem?.book?.ifBlank { LedgerTaxonomy.DEFAULT_BOOK }
            ?: LedgerTaxonomy.DEFAULT_BOOK)
    }
    var note by remember { mutableStateOf(editItem?.note.orEmpty()) }
    var occurredAt by remember {
        mutableLongStateOf(editItem?.occurredAt ?: DateUtils.todayStartMillis())
    }
    var storedToInventory by remember { mutableStateOf(editItem?.storedToInventory ?: false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val accent = accentFor(type)
    val baseAccounts = remember { accountOptions() }
    val categories = LedgerTaxonomy.categoriesFor(type)
    val categoryList = if (category in categories) categories else listOf(category) + categories
    val subcategoryOptions = listOf(LedgerTaxonomy.EMPTY_OPTION) +
        LedgerTaxonomy.subcategoriesFor(category)
    val bookOptions = if (book in LedgerTaxonomy.BOOKS) {
        LedgerTaxonomy.BOOKS
    } else {
        listOf(book) + LedgerTaxonomy.BOOKS
    }

    fun selectType(newType: String) {
        if (type == newType) return
        type = newType
        category = LedgerTaxonomy.categoriesFor(newType).first()
        subcategory = ""
    }

    fun onKey(key: String) {
        when (key) {
            "⌫" -> amountText = amountText.dropLast(1)
            "清空" -> amountText = ""
            "." -> if (!amountText.contains(".")) amountText += "."
            else -> {
                val next = amountText + key
                if (next.matches(Regex("""\d*\.?\d{0,2}""")) && next.length <= 10) {
                    amountText = next
                }
            }
        }
    }

    fun buildDraft(): LedgerDraft? {
        val amount = amountText.toDoubleOrNull() ?: return null
        if (amount <= 0) return null
        return LedgerDraft(
            type = type,
            amount = amount,
            category = category,
            subcategory = subcategory,
            account = account,
            account2 = account2,
            tags = tags.trim(),
            book = book,
            typeLabel = "",
            note = note.trim(),
            occurredAt = occurredAt,
            storedToInventory = storedToInventory
        )
    }

    fun resetForNext() {
        amountText = ""
        note = ""
        subcategory = ""
        tags = ""
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Background) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ---- 顶部栏：关闭 + 类型 + 账本 ----
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "关闭", tint = Color(0xFF424242))
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TypeTab("支出", type == LedgerEntryEntity.TYPE_EXPENSE, accent) {
                            selectType(LedgerEntryEntity.TYPE_EXPENSE)
                        }
                        TypeTab("收入", type == LedgerEntryEntity.TYPE_INCOME, NormalGreen) {
                            selectType(LedgerEntryEntity.TYPE_INCOME)
                        }
                        TypeTab("转账", type == LedgerEntryEntity.TYPE_TRANSFER, Color(0xFF757575)) {
                            selectType(LedgerEntryEntity.TYPE_TRANSFER)
                        }
                    }
                    BookChip(book = book, options = bookOptions, onSelect = { book = it })
                }

                // ---- 滚动内容 ----
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    CategoryGrid(
                        categories = categoryList,
                        selected = category,
                        accent = accent,
                        onSelect = {
                            category = it
                            subcategory = ""
                        }
                    )

                    Spacer(Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                            FieldDropdown(
                                label = "账户",
                                value = account,
                                options = if (account in baseAccounts) baseAccounts
                                else listOf(account) + baseAccounts,
                                onSelect = { account = it }
                            )
                            if (type == LedgerEntryEntity.TYPE_TRANSFER) {
                                FieldDropdown(
                                    label = "转入账户",
                                    value = account2.ifBlank { LedgerTaxonomy.EMPTY_OPTION },
                                    options = listOf(LedgerTaxonomy.EMPTY_OPTION) + baseAccounts,
                                    onSelect = {
                                        account2 = if (it == LedgerTaxonomy.EMPTY_OPTION) "" else it
                                    }
                                )
                            }
                            FieldDropdown(
                                label = "二级分类",
                                value = subcategory.ifBlank { LedgerTaxonomy.EMPTY_OPTION },
                                options = subcategoryOptions,
                                onSelect = {
                                    subcategory = if (it == LedgerTaxonomy.EMPTY_OPTION) "" else it
                                }
                            )
                            FieldInput(
                                label = "标签",
                                value = tags,
                                placeholder = "#淘宝",
                                onValueChange = { if (it.length <= 20) tags = it }
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "加入收纳",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF212121)
                                    )
                                    Text(
                                        text = "开启后，该笔消费自动加入收纳物品",
                                        fontSize = 11.sp,
                                        color = Color(0xFF9E9E9E)
                                    )
                                }
                                Switch(
                                    checked = storedToInventory,
                                    onCheckedChange = { storedToInventory = it },
                                    colors = androidx.compose.material3.SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = accent
                                    )
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ---- 金额 ----
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "¥",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = amountText.ifBlank { "0.00" },
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // ---- 日期 + 备注 ----
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showDatePicker = true }) {
                            Text(
                                text = "🕐 ${DateUtils.formatDate(occurredAt)}",
                                fontSize = 13.sp,
                                color = Color(0xFF757575)
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "·",
                            fontSize = 13.sp,
                            color = Color(0xFFBDBDBD)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = note.ifBlank { "点击填写备注" },
                            fontSize = 13.sp,
                            color = if (note.isBlank()) Color(0xFFBDBDBD) else Color(0xFF616161),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = note,
                        onValueChange = { if (it.length <= 40) note = it },
                        label = { Text("备注（选填）") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )

                    Spacer(Modifier.height(12.dp))
                }

                // ---- 数字键盘 ----
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    KeypadRow(listOf("1", "2", "3", "⌫"), accent, ::onKey)
                    KeypadRow(listOf("4", "5", "6", "清空"), accent, ::onKey)
                    KeypadRow(
                        keys = listOf("7", "8", "9", "保存再记"),
                        accent = accent,
                        onKey = ::onKey,
                        showSaveAgain = !isEdit,
                        onSaveAgain = {
                            buildDraft()?.let { draft ->
                                onConfirm(draft)
                                resetForNext()
                            }
                        }
                    )
                    KeypadRow(
                        keys = listOf(".", "0", "00", "完成"),
                        accent = accent,
                        onKey = ::onKey,
                        isLastRow = true,
                        onDone = {
                            buildDraft()?.let { draft ->
                                onConfirm(draft)
                                onDismiss()
                            }
                        }
                    )
                }
            }
        }
    }

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

private fun accentFor(type: String): Color = when (type) {
    LedgerEntryEntity.TYPE_INCOME -> NormalGreen
    LedgerEntryEntity.TYPE_TRANSFER -> Color(0xFF757575)
    else -> AlertRed
}

private fun accountOptions(): List<String> {
    val asset = com.fridgekeeper.tassetbook.data.AppRepository.get()
        .ledgerSelectableAccountNames()
    return asset.ifEmpty { LedgerTaxonomy.ACCOUNTS }
}

private fun defaultAccount(): String = accountOptions().first()

@Composable
private fun TypeTab(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Text(
        text = label,
        fontSize = if (selected) 16.sp else 15.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        color = if (selected) accent else Color(0xFF9E9E9E),
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onClick)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookChip(book: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        Box(
            modifier = Modifier
                .menuAnchor()
                .clip(RoundedCornerShape(14.dp))
                .background(Primary.copy(alpha = 0.1f))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = book,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Primary
            )
        }
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

@Composable
private fun CategoryGrid(
    categories: List<String>,
    selected: String,
    accent: Color,
    onSelect: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp)) {
            categories.chunked(5).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { cat ->
                        CategoryCell(
                            name = cat,
                            selected = cat == selected,
                            accent = accent,
                            modifier = Modifier.weight(1f),
                            onClick = { onSelect(cat) }
                        )
                    }
                    repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun CategoryCell(
    name: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(if (selected) accent else Color(0xFFF2F2F2)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = ledgerEmoji(name), fontSize = 22.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = name,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) accent else Color(0xFF616161),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FieldDropdown(
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
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .padding(vertical = 4.dp)
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

@Composable
private fun FieldInput(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, fontSize = 13.sp, color = Color(0xFFAAAAAA)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}

@Composable
private fun KeypadRow(
    keys: List<String>,
    accent: Color,
    onKey: (String) -> Unit,
    showSaveAgain: Boolean = true,
    onSaveAgain: () -> Unit = {},
    isLastRow: Boolean = false,
    onDone: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        keys.forEach { key ->
            KeypadButton(
                key = key,
                accent = accent,
                modifier = Modifier.weight(1f),
                showSaveAgain = showSaveAgain,
                onClick = {
                    when {
                        key == "完成" -> onDone()
                        key == "保存再记" -> onSaveAgain()
                        else -> onKey(key)
                    }
                }
            )
        }
    }
    if (!isLastRow) Spacer(Modifier.height(6.dp))
}

@Composable
private fun KeypadButton(
    key: String,
    accent: Color,
    modifier: Modifier = Modifier,
    showSaveAgain: Boolean = true,
    onClick: () -> Unit
) {
    val isAccent = key == "完成"
    val isFunc = key in listOf("⌫", "清空", "00") || (key == "保存再记")
    val bg = when {
        isAccent -> accent
        isFunc -> Color(0xFFF2F2F2)
        else -> Color(0xFFF7F7F7)
    }
    val fg = when {
        isAccent -> Color.White
        key == "⌫" -> Color(0xFF757575)
        else -> Color(0xFF212121)
    }
    val showKey = key == "保存再记" && !showSaveAgain
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .then(if (showKey) Modifier else Modifier.clickable(onClick = onClick)),
        contentAlignment = Alignment.Center
    ) {
        if (!showKey) {
            Text(
                text = key,
                fontSize = when {
                    isAccent -> 18.sp
                    key.length > 2 -> 13.sp
                    else -> 22.sp
                },
                fontWeight = if (isAccent) FontWeight.Bold else FontWeight.Medium,
                color = fg,
                textAlign = TextAlign.Center
            )
        }
    }
}
