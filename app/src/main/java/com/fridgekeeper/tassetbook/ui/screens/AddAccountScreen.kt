package com.fridgekeeper.tassetbook.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.fridgekeeper.tassetbook.data.Account
import com.fridgekeeper.tassetbook.data.AccountTypes
import com.fridgekeeper.tassetbook.data.AppRepository
import com.fridgekeeper.tassetbook.ui.BgGray
import com.fridgekeeper.tassetbook.ui.CompactTextField
import com.fridgekeeper.tassetbook.ui.ConfirmDialog
import com.fridgekeeper.tassetbook.ui.DividerGray
import com.fridgekeeper.tassetbook.ui.PrimaryBlue
import com.fridgekeeper.tassetbook.ui.ProfitRed
import com.fridgekeeper.tassetbook.ui.SectionCard
import com.fridgekeeper.tassetbook.ui.TextHint
import com.fridgekeeper.tassetbook.ui.TextPrimary
import com.fridgekeeper.tassetbook.ui.TextSecondary
import com.fridgekeeper.tassetbook.ui.fmt2
import com.fridgekeeper.tassetbook.util.Dates
import com.fridgekeeper.tassetbook.util.Money
import java.time.LocalDate

private val CURRENCIES = listOf(
    "CNY" to "人民币 (CNY)",
    "USD" to "美元 (USD)",
    "HKD" to "港币 (HKD)",
    "EUR" to "欧元 (EUR)",
    "JPY" to "日元 (JPY)",
    "GBP" to "英镑 (GBP)"
)
private val CURRENCY_LABELS = CURRENCIES.map { it.second }
private fun currencyLabel(code: String): String =
    CURRENCIES.firstOrNull { it.first == code }?.second ?: code

private val REPAY_METHODS = listOf("等额本息", "等额本金", "先息后本", "到期一次还本付息")

private val BANKS = listOf(
    "招商银行", "工商银行", "建设银行", "农业银行", "中国银行", "交通银行", "邮储银行",
    "浦发银行", "中信银行", "兴业银行", "平安银行", "光大银行", "民生银行", "华夏银行", "其他银行"
)

private fun accountTypeEmoji(category: String, type: String): String = when {
    type == AccountTypes.TYPE_LOAN -> "\uD83C\uDFE0"
    category == AccountTypes.CAT_CREDIT -> "\uD83D\uDCB3"
    category == AccountTypes.CAT_PREPAID -> "\uD83C\uDFAB"
    category == AccountTypes.CAT_INVEST -> "\uD83D\uDCC8"
    else -> "\uD83D\uDCB0"
}

private fun onlyNumber(s: String) = s.filter { it.isDigit() }
private fun numberOrDot(s: String) = s.filter { it.isDigit() || it == '.' }

private fun pickDate(context: Context, initial: String, onPick: (String) -> Unit) {
    val cur = runCatching { LocalDate.parse(initial) }.getOrNull() ?: LocalDate.now()
    val dlg = DatePickerDialog(context, null, cur.year, cur.monthValue - 1, cur.dayOfMonth)
    dlg.setOnDateSetListener { _, y, m, d -> onPick(LocalDate.of(y, m + 1, d).toString()) }
    dlg.show()
}

/**
 * 添加 / 编辑资金账户。
 * 顶部为「返回 + 标题 + ✓ 保存」，内容按卡片分组、左标签右控件，随账户类型显示专属字段，
 * 整体参照「添加账户 / 修改账户」设计稿。
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

    var category by remember { mutableStateOf(editing?.category ?: "") }
    var type by remember { mutableStateOf(editing?.type ?: "") }
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var balance by remember { mutableStateOf(editing?.let { fmt2(it.balance) } ?: "") }
    var remark by remember { mutableStateOf(editing?.remark ?: "") }
    var ledgerSelectable by remember { mutableStateOf(editing?.ledgerSelectable != false) }
    var includeInTotal by remember { mutableStateOf(editing?.includeInTotalAssets != false) }
    var currency by remember { mutableStateOf(editing?.currency ?: "CNY") }
    var billingDay by remember { mutableStateOf(editing?.billingDay?.toString().orEmpty()) }
    var repaymentDay by remember { mutableStateOf(editing?.repaymentDay?.toString().orEmpty()) }
    var bank by remember { mutableStateOf(editing?.bank ?: "") }
    var creditLimit by remember { mutableStateOf(editing?.creditLimit?.let { fmt2(it) } ?: "") }
    var billCountInCurrent by remember { mutableStateOf(editing?.billCountInCurrent == true) }
    var loanAmount by remember { mutableStateOf(editing?.loanAmount?.let { fmt2(it) } ?: "") }
    var loanRate by remember { mutableStateOf(editing?.loanRate?.toString().orEmpty()) }
    var repaymentMethod by remember { mutableStateOf(editing?.repaymentMethod ?: "") }
    var loanPeriods by remember { mutableStateOf(editing?.loanPeriods?.toString().orEmpty()) }
    var paidPeriods by remember { mutableStateOf(editing?.paidPeriods?.toString().orEmpty()) }
    var firstRepaymentDate by remember { mutableStateOf(editing?.firstRepaymentDate ?: "") }

    var showTypePicker by remember { mutableStateOf(false) }
    var showBankPicker by remember { mutableStateOf(false) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var showMethodPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isLoan = type == AccountTypes.TYPE_LOAN
    val isCredit = category == AccountTypes.CAT_CREDIT
    // 信用卡类账户（含花呗 / 白条等）显示信用卡专属字段；贷款单独显示贷款字段
    val isCreditCard = isCredit && !isLoan

    val typeDisplay = type.ifBlank { "请选择账户类型" }
    val typeEmoji = accountTypeEmoji(category, type)

    fun save() {
        if (type.isBlank()) {
            Toast.makeText(context, "请选择账户类型", Toast.LENGTH_SHORT).show()
            return
        }
        val amount = (if (isLoan) loanAmount else balance).toDoubleOrNull()
        if (amount == null || amount < 0) {
            Toast.makeText(context, "请输入有效的金额", Toast.LENGTH_SHORT).show()
            return
        }
        val billing = billingDay.toIntOrNull()
        val repayment = repaymentDay.toIntOrNull()
        if (isCredit) {
            if (billingDay.isNotBlank() && (billing == null || billing !in 1..31)) {
                Toast.makeText(context, "账单日需为 1-31", Toast.LENGTH_SHORT).show()
                return
            }
            if (repaymentDay.isNotBlank() && (repayment == null || repayment !in 1..31)) {
                Toast.makeText(context, "还款日需为 1-31", Toast.LENGTH_SHORT).show()
                return
            }
        }
        val now = Dates.nowString()
        val account = Account(
            id = editing?.id ?: System.currentTimeMillis().toString(),
            category = category,
            type = type,
            name = name.ifBlank { type },
            balance = Money.round2(amount),
            remark = remark.ifBlank { null },
            ledgerSelectable = if (isLoan) false else ledgerSelectable,
            billingDay = if (isCredit) billing else null,
            repaymentDay = if (isCredit) repayment else null,
            currency = currency,
            includeInTotalAssets = includeInTotal,
            bank = bank.ifBlank { null },
            creditLimit = creditLimit.toDoubleOrNull(),
            billCountInCurrent = billCountInCurrent,
            loanAmount = if (isLoan) loanAmount.toDoubleOrNull() else editing?.loanAmount,
            loanRate = if (isLoan) loanRate.toDoubleOrNull() else editing?.loanRate,
            repaymentMethod = if (isLoan) repaymentMethod.ifBlank { null } else editing?.repaymentMethod,
            loanPeriods = if (isLoan) loanPeriods.toIntOrNull() else editing?.loanPeriods,
            paidPeriods = if (isLoan) paidPeriods.toIntOrNull() else editing?.paidPeriods,
            firstRepaymentDate = if (isLoan) firstRepaymentDate.ifBlank { null } else editing?.firstRepaymentDate,
            order = editing?.order ?: ((repo.accountsFlow.value.minOfOrNull { it.order } ?: 0L) - 1L),
            createTime = editing?.createTime ?: now,
            updateTime = now
        )
        if (editing == null) {
            repo.addAccount(account)
            Toast.makeText(context, "已添加账户", Toast.LENGTH_SHORT).show()
        } else {
            repo.updateAccount(account)
            Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
        }
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editing != null) "修改账户" else "添加账户") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { save() }) {
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "保存",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
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
            // 账户类型
            SectionCard {
                FormDisplayRow(
                    label = "账户类型",
                    value = typeDisplay,
                    leading = typeEmoji,
                    onClick = { showTypePicker = true }
                )
            }

            // 名称 / 备注
            SectionCard {
                FormTextFieldRow(
                    label = "账户名称",
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "如：招商银行信用卡 6688"
                )
                FormDivider()
                FormTextFieldRow(
                    label = "账户备注",
                    value = remark,
                    onValueChange = { remark = it },
                    placeholder = "点击填写备注（可不填）"
                )
            }

            // 信用卡专属
            if (isCreditCard) {
                SectionCard {
                    FormDisplayRow("发卡银行", bank.ifBlank { "请选择银行" }, onClick = { showBankPicker = true })
                    FormDivider()
                    FormTextFieldRow(
                        label = "当前欠款",
                        value = balance,
                        onValueChange = { balance = numberOrDot(it) },
                        placeholder = "请输入当前欠款",
                        keyboardType = KeyboardType.Decimal
                    )
                    FormDivider()
                    FormTextFieldRow(
                        label = "总信用额度",
                        value = creditLimit,
                        onValueChange = { creditLimit = numberOrDot(it) },
                        placeholder = "请输入总额度",
                        keyboardType = KeyboardType.Decimal
                    )
                    FormDivider()
                    FormTextFieldRow(
                        label = "账单日",
                        value = billingDay,
                        onValueChange = { billingDay = onlyNumber(it).take(2) },
                        placeholder = "请输入日（1-31）",
                        suffix = "日",
                        keyboardType = KeyboardType.Number
                    )
                    FormDivider()
                    FormTextFieldRow(
                        label = "还款日",
                        value = repaymentDay,
                        onValueChange = { repaymentDay = onlyNumber(it).take(2) },
                        placeholder = "请输入日（1-31）",
                        suffix = "日",
                        keyboardType = KeyboardType.Number
                    )
                    FormDivider()
                    FormSwitchRow(
                        label = "出账日账单计入当期",
                        checked = billCountInCurrent,
                        onCheckedChange = { billCountInCurrent = it }
                    )
                }
            }

            // 贷款专属
            if (isLoan) {
                SectionCard {
                    FormTextFieldRow(
                        label = "贷款金额",
                        value = loanAmount,
                        onValueChange = { loanAmount = numberOrDot(it) },
                        placeholder = "请输入贷款金额",
                        keyboardType = KeyboardType.Decimal
                    )
                    FormDivider()
                    FormTextFieldRow(
                        label = "贷款年利率",
                        value = loanRate,
                        onValueChange = { loanRate = numberOrDot(it) },
                        placeholder = "如：3.05",
                        suffix = "%",
                        keyboardType = KeyboardType.Decimal
                    )
                    FormDivider()
                    FormDisplayRow(
                        label = "还款方式",
                        value = repaymentMethod.ifBlank { "请选择还款方式" },
                        onClick = { showMethodPicker = true }
                    )
                    FormDivider()
                    FormTextFieldRow(
                        label = "贷款期数",
                        value = loanPeriods,
                        onValueChange = { loanPeriods = onlyNumber(it) },
                        placeholder = "请输入期数",
                        suffix = "期",
                        keyboardType = KeyboardType.Number
                    )
                    FormDivider()
                    FormTextFieldRow(
                        label = "已还期数",
                        value = paidPeriods,
                        onValueChange = { paidPeriods = onlyNumber(it) },
                        placeholder = "请输入已还期数",
                        suffix = "期",
                        keyboardType = KeyboardType.Number
                    )
                    FormDivider()
                    FormDisplayRow(
                        label = "首次还款日",
                        value = firstRepaymentDate.ifBlank { "请选择日期" },
                        onClick = { showDatePicker = true }
                    )
                }
            }

            // 普通账户：余额
            if (!isCredit && !isLoan) {
                SectionCard {
                    FormTextFieldRow(
                        label = "账户余额",
                        value = balance,
                        onValueChange = { balance = numberOrDot(it) },
                        placeholder = "请输入余额",
                        keyboardType = KeyboardType.Decimal
                    )
                }
            }

            // 通用设置
            SectionCard {
                FormDisplayRow(
                    label = "账户币种",
                    value = currencyLabel(currency),
                    onClick = { showCurrencyPicker = true }
                )
                FormDivider()
                FormSwitchRow(
                    label = "计入总资产",
                    checked = includeInTotal,
                    onCheckedChange = { includeInTotal = it }
                )
                if (!isLoan) {
                    FormDivider()
                    FormSwitchRow(
                        label = "记账时可被选择",
                        sub = "关闭后，记账的账户选项中不显示该账户",
                        checked = ledgerSelectable,
                        onCheckedChange = { ledgerSelectable = it }
                    )
                }
            }

            // 删除账户（编辑模式）
            if (editing != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("删除账户", color = ProfitRed)
                }
            }
        }
    }

    if (showTypePicker) {
        TypePickerDialog(selectedType = type) { cat, t ->
            category = cat
            type = t
            if (name.isBlank() || AccountTypes.categories.flatMap { it.types }.contains(name)) {
                name = t
            }
            showTypePicker = false
        }
    }
    if (showBankPicker) {
        ChoiceDialog("选择发卡银行", BANKS, bank) {
            bank = it
            showBankPicker = false
        }
    }
    if (showCurrencyPicker) {
        ChoiceDialog("选择币种", CURRENCY_LABELS, currencyLabel(currency)) { label ->
            currency = CURRENCIES.firstOrNull { it.second == label }?.first ?: "CNY"
            showCurrencyPicker = false
        }
    }
    if (showMethodPicker) {
        ChoiceDialog("选择还款方式", REPAY_METHODS, repaymentMethod) {
            repaymentMethod = it
            showMethodPicker = false
        }
    }
    if (showDatePicker) {
        val initial = firstRepaymentDate.ifBlank { LocalDate.now().toString() }
        pickDate(context, initial) { picked ->
            firstRepaymentDate = picked
        }
        showDatePicker = false
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
private fun TypePickerDialog(
    selectedType: String,
    onPick: (category: String, type: String) -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("选择账户类型") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                AccountTypes.categories.forEach { cat ->
                    Text(
                        cat.label,
                        fontSize = 12.sp,
                        color = TextHint,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                    cat.types.forEach { t ->
                        val sel = t == selectedType
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onPick(cat.key, t) }
                                .padding(vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                t,
                                fontSize = 14.sp,
                                color = if (sel) PrimaryBlue else TextPrimary,
                                fontWeight = if (sel) FontWeight.Medium else null
                            )
                            if (sel) {
                                Spacer(Modifier.weight(1f))
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun ChoiceDialog(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { opt ->
                    val sel = opt == selected
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onSelect(opt) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            opt,
                            fontSize = 14.sp,
                            color = if (sel) PrimaryBlue else TextPrimary,
                            fontWeight = if (sel) FontWeight.Medium else null
                        )
                        if (sel) {
                            Spacer(Modifier.weight(1f))
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun FormDivider() {
    Divider(color = DividerGray, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 2.dp))
}

@Composable
private fun FormTextFieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    suffix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = TextSecondary, modifier = Modifier.width(92.dp))
        CompactTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = placeholder,
            keyboardType = keyboardType,
            trailingIcon = if (suffix != null) {
                { Text(suffix, fontSize = 13.sp, color = TextHint) }
            } else null
        )
    }
}

@Composable
private fun FormDisplayRow(
    label: String,
    value: String,
    leading: String? = null,
    valueColor: Color = TextHint,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            Text(leading, fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
        }
        Text(label, fontSize = 14.sp, color = TextSecondary, modifier = Modifier.width(92.dp))
        Spacer(Modifier.weight(1f))
        Text(
            value,
            fontSize = 14.sp,
            color = valueColor,
            maxLines = 1
        )
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = TextHint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun FormSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    sub: String? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, color = TextPrimary)
            if (sub != null) {
                Text(sub, fontSize = 11.sp, color = TextHint)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryBlue)
        )
    }
}
