package com.fridgekeeper.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fridgekeeper.data.local.LedgerEntryEntity
import com.fridgekeeper.model.DateUtils
import com.fridgekeeper.model.LedgerItemUiState
import com.fridgekeeper.model.ParsedLedgerEntry
import com.fridgekeeper.ui.components.AddLedgerDialog
import com.fridgekeeper.ui.components.LedgerItemCard
import com.fridgekeeper.ui.components.LedgerMonthSummary
import com.fridgekeeper.ui.components.LedgerSettingsDialog
import com.fridgekeeper.ui.components.SwipeEditDeleteBox
import com.fridgekeeper.ui.components.formatLedgerAmount
import com.fridgekeeper.ui.components.ledgerEmoji
import com.fridgekeeper.ui.theme.AlertRed
import com.fridgekeeper.ui.theme.NormalGreen
import com.fridgekeeper.ui.theme.Primary
import com.fridgekeeper.viewmodel.LedgerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(viewModel: LedgerViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<LedgerItemUiState?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var inputText by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("记账本", color = Color.White, fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "智能记账设置",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = Primary
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "手动记一笔",
                    tint = Color.White
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                LedgerMonthSummary(
                    monthExpense = uiState.monthExpense,
                    monthIncome = uiState.monthIncome,
                    balance = uiState.monthBalance,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                SmartEntryCard(
                    inputText = inputText,
                    onInputChange = { inputText = it },
                    isParsing = uiState.isParsing,
                    parsedItems = uiState.parsedItems,
                    error = uiState.parseError,
                    hint = uiState.parseHint,
                    onParse = { viewModel.parseInput(inputText) },
                    onRemove = { viewModel.removeParsedItem(it) },
                    onConfirm = {
                        viewModel.confirmParsed(inputText)
                        inputText = ""
                    },
                    onClear = { viewModel.clearParsed() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                Text(
                    text = "最近记录",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF212121),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (uiState.items.isEmpty()) {
                item {
                    Text(
                        text = "还没有记账记录\n在上方输入一句话，或点右下角 + 手动记一笔",
                        fontSize = 14.sp,
                        color = Color(0xFF888888),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp)
                    )
                }
            } else {
                items(items = uiState.items, key = { it.id }) { item ->
                    SwipeEditDeleteBox(
                        onEdit = { editTarget = item },
                        onDelete = { viewModel.deleteEntry(item.id) }
                    ) {
                        LedgerItemCard(item = item)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showAdd) {
        AddLedgerDialog(
            onDismiss = { showAdd = false },
            onConfirm = { type, amount, category, payment, note, occurredAt ->
                viewModel.addEntry(type, amount, category, payment, note, occurredAt)
                showAdd = false
            }
        )
    }

    editTarget?.let { target ->
        AddLedgerDialog(
            editItem = target,
            onDismiss = { editTarget = null },
            onConfirm = { type, amount, category, payment, note, occurredAt ->
                viewModel.updateEntry(
                    target.id, type, amount, category, payment, note, occurredAt
                )
                editTarget = null
            }
        )
    }

    if (showSettings) {
        val config = remember { viewModel.loadConfig() }
        LedgerSettingsDialog(
            config = config,
            onDismiss = { showSettings = false },
            onSave = { apiKey, baseUrl, model ->
                viewModel.saveConfig(apiKey, baseUrl, model)
                showSettings = false
            }
        )
    }
}

@Composable
private fun SmartEntryCard(
    inputText: String,
    onInputChange: (String) -> Unit,
    isParsing: Boolean,
    parsedItems: List<ParsedLedgerEntry>,
    error: String?,
    hint: String?,
    onParse: () -> Unit,
    onRemove: (Int) -> Unit,
    onConfirm: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🤖", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "智能记账",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF212121)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                placeholder = {
                    Text(
                        text = "例如：今天午饭35，打车20，工资到账8000",
                        fontSize = 13.sp,
                        color = Color(0xFFAAAAAA)
                    )
                },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "一句话可记多笔，自动识别金额、分类、时间",
                    fontSize = 10.sp,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onParse,
                    enabled = !isParsing && inputText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    if (isParsing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(if (isParsing) "解析中" else "智能解析", fontSize = 13.sp)
                }
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = error, fontSize = 11.sp, color = AlertRed)
            }
            if (hint != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = hint, fontSize = 11.sp, color = Color(0xFF757575))
            }

            if (parsedItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(4.dp))

                parsedItems.forEachIndexed { index, item ->
                    ParsedLedgerRow(
                        item = item,
                        onRemove = { onRemove(index) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onClear) {
                        Text("清空", color = Color(0xFF888888), fontSize = 13.sp)
                    }
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = NormalGreen)
                    ) {
                        Text("确认入账（${parsedItems.size}笔）", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ParsedLedgerRow(
    item: ParsedLedgerEntry,
    onRemove: () -> Unit
) {
    val isExpense = item.type == LedgerEntryEntity.TYPE_EXPENSE
    val amountColor = if (isExpense) AlertRed else NormalGreen

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = ledgerEmoji(item.category), fontSize = 18.sp)
        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (item.note.isBlank()) item.category else "${item.category} · ${item.note}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF212121)
            )
            Text(
                text = "${DateUtils.formatDate(item.occurredAt)} · ${item.paymentMethod}",
                fontSize = 10.sp,
                color = Color(0xFF9E9E9E)
            )
        }

        Text(
            text = (if (isExpense) "-" else "+") + formatLedgerAmount(item.amount),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )

        IconButton(onClick = onRemove, modifier = Modifier.size(30.dp)) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "移除",
                tint = Color(0xFFBDBDBD),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
