package com.fridgekeeper.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fridgekeeper.data.local.LocationEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationDropdown(
    locations: List<LocationEntity>,
    selectedId: Long?,
    onSelected: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = locations.firstOrNull { it.id == selectedId }?.displayName ?: "不设置位置"
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("存放位置") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("不设置位置") },
                onClick = {
                    onSelected(null)
                    expanded = false
                }
            )
            locations.forEach { loc ->
                DropdownMenuItem(
                    text = { Text(loc.displayName) },
                    onClick = {
                        onSelected(loc.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun DailyCostFields(
    trackDailyCost: Boolean,
    onTrackChange: (Boolean) -> Unit,
    buyPrice: String,
    onBuyPriceChange: (String) -> Unit,
    requiredWhenEnabled: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text("计算每日价格", modifier = Modifier.weight(1f))
            Switch(checked = trackDailyCost, onCheckedChange = onTrackChange)
        }
        if (trackDailyCost) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = buyPrice,
                onValueChange = {
                    if (it.isEmpty() || it.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                        onBuyPriceChange(it)
                    }
                },
                label = { Text(if (requiredWhenEnabled) "买入价格（元）" else "买入价格（可选）") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
