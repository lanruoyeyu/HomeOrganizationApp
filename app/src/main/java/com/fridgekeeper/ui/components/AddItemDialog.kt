package com.fridgekeeper.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.fridgekeeper.data.local.LocationEntity
import com.fridgekeeper.data.local.SubcategoryEntity
import com.fridgekeeper.model.DateUtils
import com.fridgekeeper.model.ItemUiState
import com.fridgekeeper.ui.theme.Primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemDialog(
    subcategories: List<SubcategoryEntity>,
    locations: List<LocationEntity>,
    onDismiss: () -> Unit,
    onAddSubcategory: (name: String, onResult: (Boolean, Long) -> Unit) -> Unit,
    editItem: ItemUiState? = null,
    onConfirm: (
        name: String,
        subcategoryId: Long,
        quantity: Int,
        storageDate: Long,
        expirationDate: Long,
        locationId: Long?,
        trackDailyCost: Boolean,
        buyPrice: Double?,
        buyAt: Long?
    ) -> Unit
) {
    var name by remember { mutableStateOf(editItem?.name ?: "") }
    var nameError by remember { mutableStateOf(false) }
    var selectedSubId by remember(subcategories, editItem) {
        mutableLongStateOf(editItem?.subcategoryId ?: subcategories.firstOrNull()?.id ?: 0L)
    }
    var subExpanded by remember { mutableStateOf(false) }
    var showNewSub by remember { mutableStateOf(false) }
    var quantity by remember { mutableStateOf(editItem?.quantity?.toString() ?: "1") }
    var quantityError by remember { mutableStateOf(false) }
    var storageDate by remember { mutableLongStateOf(editItem?.storageDate ?: DateUtils.todayStartMillis()) }
    var expirationDate by remember { mutableLongStateOf(editItem?.expirationDate ?: (DateUtils.todayStartMillis() + 7L * 24 * 60 * 60 * 1000)) }
    var dateError by remember { mutableStateOf(false) }
    var showStoragePicker by remember { mutableStateOf(false) }
    var showExpirationPicker by remember { mutableStateOf(false) }
    var locationId by remember { mutableStateOf<Long?>(editItem?.locationId) }
    var trackDailyCost by remember { mutableStateOf(editItem?.trackDailyCost ?: false) }
    var buyPrice by remember {
        mutableStateOf(editItem?.buyPrice?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
    }
    val selectedSubName = subcategories.firstOrNull { it.id == selectedSubId }?.name ?: "请选择小类"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editItem == null) "添加食品" else "编辑食品", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
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
                    supportingText = { if (nameError) Text("请输入物品名称", color = Color.Red) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = subExpanded,
                        onExpandedChange = { subExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedSubName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("小类") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(subExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = subExpanded,
                            onDismissRequest = { subExpanded = false }
                        ) {
                            subcategories.forEach { sub ->
                                DropdownMenuItem(
                                    text = { Text(sub.name) },
                                    onClick = {
                                        selectedSubId = sub.id
                                        trackDailyCost = sub.trackDailyCostDefault
                                        subExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    TextButton(onClick = { showNewSub = true }) { Text("新小类") }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = {
                        if (it.all { c -> c.isDigit() } && it.length <= 5) {
                            quantity = it
                            quantityError = false
                        }
                    },
                    label = { Text("数量") },
                    isError = quantityError,
                    supportingText = { if (quantityError) Text("数量至少为1", color = Color.Red) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showStoragePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("存放：${DateUtils.formatDate(storageDate)}", modifier = Modifier.weight(1f))
                    Text("📅", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showExpirationPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("过期：${DateUtils.formatDate(expirationDate)}", modifier = Modifier.weight(1f))
                    Text("📅", fontSize = 16.sp)
                }
                if (dateError) {
                    Text("过期时间不能早于存放时间", color = Color.Red, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                LocationDropdown(locations, locationId, onSelected = { locationId = it })
                Spacer(modifier = Modifier.height(12.dp))
                DailyCostFields(trackDailyCost, { trackDailyCost = it }, buyPrice, { buyPrice = it })
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
                    val qty = quantity.toIntOrNull() ?: 0
                    if (qty < 1) {
                        quantityError = true
                        hasError = true
                    }
                    if (expirationDate < storageDate) {
                        dateError = true
                        hasError = true
                    }
                    if (selectedSubId == 0L) hasError = true
                    val priceValue = buyPrice.toDoubleOrNull()
                    if (trackDailyCost && (priceValue == null || priceValue <= 0)) hasError = true
                    if (!hasError) {
                        onConfirm(
                            name.trim(),
                            selectedSubId,
                            qty,
                            storageDate,
                            expirationDate,
                            locationId,
                            trackDailyCost,
                            if (trackDailyCost) priceValue else null,
                            if (trackDailyCost) storageDate else null
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) { Text(if (editItem == null) "确认添加" else "保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )

    if (showStoragePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.clampToPickerRange(storageDate)
        )
        DatePickerDialog(
            onDismissRequest = { showStoragePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        storageDate = it
                        if (expirationDate < it) expirationDate = it + 7L * 24 * 60 * 60 * 1000
                        dateError = false
                    }
                    showStoragePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showStoragePicker = false }) { Text("取消") } }
        ) { DatePicker(state = state) }
    }
    if (showExpirationPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.clampToPickerRange(expirationDate)
        )
        DatePickerDialog(
            onDismissRequest = { showExpirationPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        expirationDate = it
                        dateError = it < storageDate
                    }
                    showExpirationPicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showExpirationPicker = false }) { Text("取消") } }
        ) { DatePicker(state = state) }
    }
    if (showNewSub) {
        var newName by remember { mutableStateOf("") }
        var dup by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showNewSub = false },
            title = { Text("新增小类") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        if (it.length <= 10) {
                            newName = it
                            dup = false
                        }
                    },
                    label = { Text("小类名称，如零食") },
                    isError = dup,
                    supportingText = { if (dup) Text("该小类已存在", color = Color.Red) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onAddSubcategory(newName) { success, id ->
                        if (success) {
                            selectedSubId = id
                            showNewSub = false
                        } else dup = true
                    }
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showNewSub = false }) { Text("取消") } }
        )
    }
}
