package com.fridgekeeper.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fridgekeeper.model.ItemUiState
import com.fridgekeeper.ui.components.AddItemDialog
import com.fridgekeeper.ui.components.ItemCard
import com.fridgekeeper.ui.components.StatsBar
import com.fridgekeeper.ui.components.SwipeEditDeleteBox
import com.fridgekeeper.ui.components.UsageHistoryDialog
import com.fridgekeeper.ui.components.UseItemDialog
import com.fridgekeeper.ui.theme.LocalSpacingScale
import com.fridgekeeper.ui.theme.Primary
import com.fridgekeeper.viewmodel.FridgeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FridgeScreen(viewModel: FridgeViewModel, onBack: (() -> Unit)? = null) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<ItemUiState?>(null) }
    var useTarget by remember { mutableStateOf<ItemUiState?>(null) }
    var historyTarget by remember { mutableStateOf<ItemUiState?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("食品", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    onBack?.let { back ->
                        IconButton(onClick = back) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Primary
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "添加物品",
                    tint = Color.White
                )
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 统计栏
                StatsBar(
                    subcategoryCount = uiState.subcategoryCount,
                    itemCount = uiState.itemCount
                )

                // 物品列表
                if (uiState.items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "冰箱空空如也\n点击右下角 + 添加物品",
                            fontSize = 16.sp,
                            color = Color(0xFF888888),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp * LocalSpacingScale.current)
                    ) {
                        items(
                            items = uiState.items,
                            key = { it.id }
                        ) { item ->
                            SwipeEditDeleteBox(
                                onEdit = { editTarget = item },
                                onDelete = { viewModel.clearItem(item.id) }
                            ) {
                                ItemCard(
                                    item = item,
                                    onDecrease = {
                                        viewModel.decreaseQuantity(item.id, item.quantity)
                                    },
                                    onIncrease = {
                                        viewModel.increaseQuantity(item.id, item.quantity)
                                    },
                                    onClear = {
                                        viewModel.clearItem(item.id)
                                    },
                                    onUse = { useTarget = item },
                                    onHistory = { historyTarget = item }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 添加物品对话框
    if (showAddDialog) {
        AddItemDialog(
            subcategories = uiState.subcategories,
            locations = uiState.locations,
            onDismiss = { showAddDialog = false },
            onAddSubcategory = { name, onResult ->
                viewModel.addSubcategory(name) { success, id -> onResult(success, id) }
            },
            onConfirm = { name, subcategoryId, quantity, storageDate, expirationDate,
                          locationId, trackDailyCost, buyPrice, buyAt ->
                viewModel.addItem(
                    name,
                    subcategoryId,
                    quantity,
                    storageDate,
                    expirationDate,
                    locationId,
                    trackDailyCost,
                    buyPrice,
                    buyAt
                )
                showAddDialog = false
            }
        )
    }

    // 编辑物品对话框
    editTarget?.let { target ->
        AddItemDialog(
            subcategories = uiState.subcategories,
            locations = uiState.locations,
            onDismiss = { editTarget = null },
            onAddSubcategory = { name, onResult ->
                viewModel.addSubcategory(name) { success, id -> onResult(success, id) }
            },
            editItem = target,
            onConfirm = { name, subcategoryId, quantity, storageDate, expirationDate,
                          locationId, trackDailyCost, buyPrice, buyAt ->
                viewModel.updateItem(
                    target.id,
                    name,
                    subcategoryId,
                    quantity,
                    storageDate,
                    expirationDate,
                    locationId,
                    trackDailyCost,
                    buyPrice,
                    buyAt
                )
                editTarget = null
            }
        )
    }

    // 使用弹窗
    useTarget?.let { target ->
        UseItemDialog(
            itemName = target.name,
            onDismiss = { useTarget = null },
            onConfirm = { usage, usedAt, scene ->
                viewModel.addUsageRecord(target.id, target.name, usage, usedAt, scene)
                useTarget = null
            }
        )
    }

    // 使用记录弹窗
    historyTarget?.let { target ->
        val records by remember(target.id) { viewModel.observeUsageHistory(target.id) }
            .collectAsState(initial = emptyList())
        UsageHistoryDialog(
            itemName = target.name,
            records = records,
            onDismiss = { historyTarget = null }
        )
    }
}
