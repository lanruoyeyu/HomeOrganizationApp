package com.fridgekeeper.tassetbook.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fridgekeeper.tassetbook.data.AccountTypes
import com.fridgekeeper.tassetbook.data.AppRepository
import com.fridgekeeper.tassetbook.ui.BgGray
import com.fridgekeeper.tassetbook.ui.CompactTextField
import com.fridgekeeper.tassetbook.ui.ConfirmDialog
import com.fridgekeeper.tassetbook.ui.PrimaryBlue
import com.fridgekeeper.tassetbook.ui.ProfitRed
import com.fridgekeeper.tassetbook.ui.SectionCard
import com.fridgekeeper.tassetbook.ui.TextHint
import com.fridgekeeper.tassetbook.ui.TextPrimary
import com.fridgekeeper.tassetbook.ui.TextSecondary
import com.fridgekeeper.tassetbook.ui.fmt2

/**
 * 添加/编辑资产页账户。选择类别 → 类型，填写名称、金额、备注。
 * 信用账户的金额记为欠款（汇总时计负）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    accountId: String?,
    onBack: () -> Unit
) {
    val repo = AppRepository.get()
    val context = LocalContext.current
    val editing = accountId?.let { repo.getAccount(it) }

    var category by remember { mutableStateOf(editing?.category ?: AccountTypes.CAT_ASSET) }
    var type by remember { mutableStateOf(editing?.type ?: "") }
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var balance by remember { mutableStateOf(editing?.let { fmt2(it.balance) } ?: "") }
    var remark by remember { mutableStateOf(editing?.remark ?: "") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var ledgerSelectable by remember { mutableStateOf(editing?.ledgerSelectable != false) }
    var billingDay by remember { mutableStateOf(editing?.billingDay?.toString().orEmpty()) }
    var repaymentDay by remember { mutableStateOf(editing?.repaymentDay?.toString().orEmpty()) }
    val isLoan = category == AccountTypes.CAT_CREDIT && type == AccountTypes.TYPE_LOAN

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editing != null) "编辑账户" else "添加账户") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", modifier = Modifier.size(48.dp))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BgGray)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // 类别选择
            SectionCard {
                Text("账户类别", fontSize = 13.sp, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccountTypes.categories.forEach { cat ->
                        SelectChip(
                            text = cat.label,
                            selected = category == cat.key,
                            onClick = {
                                if (category != cat.key) {
                                    category = cat.key
                                    type = ""
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 类型选择
            SectionCard {
                Text("账户类型", fontSize = 13.sp, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                val types = AccountTypes.typesOf(category)
                types.chunked(3).forEach { rowTypes ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowTypes.forEach { t ->
                            SelectChip(
                                text = t,
                                selected = type == t,
                                onClick = {
                                    type = t
                                    if (name.isBlank() || editing == null && AccountTypes.categories
                                            .flatMap { it.types }.contains(name)
                                    ) {
                                        name = t
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(3 - rowTypes.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                if (category == AccountTypes.CAT_INVEST) {
                    Spacer(Modifier.height(4.dp))
                    Text("股票账户由系统自动生成（金额=持仓市值），无需手动添加", fontSize = 11.sp, color = TextHint)
                }
            }

            // 名称 / 金额 / 备注
            SectionCard {
                Text("名称", fontSize = 13.sp, color = TextSecondary)
                CompactTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    placeholder = type.ifBlank { "账户名称" }
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    if (category == AccountTypes.CAT_CREDIT) "欠款金额（元）" else "余额（元）",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                CompactTextField(
                    value = balance,
                    onValueChange = { v -> balance = v.filter { it.isDigit() || it == '.' || it == '-' } },
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    placeholder = "0.00",
                    keyboardType = KeyboardType.Decimal
                )
                Spacer(Modifier.height(10.dp))
                Text("备注", fontSize = 13.sp, color = TextSecondary)
                CompactTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    placeholder = "选填"
                )
            }

            // 信用账户：账单日 / 还款日
            if (category == AccountTypes.CAT_CREDIT) {
                SectionCard {
                    Text("账单日 / 还款日", fontSize = 13.sp, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("账单日", fontSize = 12.sp, color = TextHint)
                            CompactTextField(
                                value = billingDay,
                                onValueChange = { billingDay = it.filter { c -> c.isDigit() }.take(2) },
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                placeholder = "1-31",
                                keyboardType = KeyboardType.Number
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text("还款日", fontSize = 12.sp, color = TextHint)
                            CompactTextField(
                                value = repaymentDay,
                                onValueChange = { repaymentDay = it.filter { c -> c.isDigit() }.take(2) },
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                placeholder = "1-31",
                                keyboardType = KeyboardType.Number
                            )
                        }
                    }
                }
            }

            // 贷款不可被记账选择，不显示开关
            if (!isLoan) {
                SectionCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("记账时是否可被选择", fontSize = 13.sp, color = TextPrimary)
                            Text(
                                "关闭后，记账的账户选项中不显示该账户",
                                fontSize = 11.sp,
                                color = TextHint
                            )
                        }
                        Switch(
                            checked = ledgerSelectable,
                            onCheckedChange = { ledgerSelectable = it }
                        )
                    }
                }
            }

            // 保存
            Button(
                onClick = {
                    if (type.isBlank()) {
                        Toast.makeText(context, "请选择账户类型", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val amount = balance.toDoubleOrNull()
                    if (amount == null || amount < 0) {
                        Toast.makeText(context, "请输入有效的金额", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val billing = billingDay.toIntOrNull()
                    val repayment = repaymentDay.toIntOrNull()
                    if (category == AccountTypes.CAT_CREDIT) {
                        if (billingDay.isNotBlank() && (billing == null || billing !in 1..31)) {
                            Toast.makeText(context, "账单日需为 1-31", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (repaymentDay.isNotBlank() && (repayment == null || repayment !in 1..31)) {
                            Toast.makeText(context, "还款日需为 1-31", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                    }
                    val finalName = name.ifBlank { type }
                    val selectable = if (isLoan) false else ledgerSelectable
                    val finalBilling = if (category == AccountTypes.CAT_CREDIT) billing else null
                    val finalRepayment = if (category == AccountTypes.CAT_CREDIT) repayment else null
                    if (editing == null) {
                        repo.addAccount(
                            category, type, finalName, amount, remark,
                            selectable, finalBilling, finalRepayment
                        )
                        Toast.makeText(context, "已添加账户", Toast.LENGTH_SHORT).show()
                    } else {
                        repo.updateAccount(
                            editing.id, category, type, finalName, amount, remark,
                            selectable, finalBilling, finalRepayment
                        )
                        Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
                    }
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(if (editing != null) "保存" else "添加", color = Color.White)
            }

            // 删除（编辑模式）
            if (editing != null) {
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                ) {
                    Text("删除账户", color = ProfitRed)
                }
            }
        }
    }

    if (showDeleteConfirm && editing != null) {
        ConfirmDialog(
            title = "删除账户",
            content = "确定删除账户「${editing.name}」吗？该操作不可恢复。",
            confirmText = "删除",
            onConfirm = {
                repo.deleteAccount(editing.id)
                showDeleteConfirm = false
                onBack()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
private fun SelectChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) PrimaryBlue else Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 11.sp,
            color = if (selected) Color.White else TextPrimary,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1
        )
    }
}
