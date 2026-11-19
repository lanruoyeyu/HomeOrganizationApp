package com.fridgekeeper.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fridgekeeper.ui.theme.LocalSpacingScale
import com.fridgekeeper.ui.theme.Primary
import com.fridgekeeper.viewmodel.DigitalViewModel
import com.fridgekeeper.viewmodel.FridgeViewModel
import com.fridgekeeper.viewmodel.MedicineViewModel
import com.fridgekeeper.viewmodel.PetViewModel
import com.fridgekeeper.viewmodel.StatsViewModel

private const val SECTION_FOOD = "FOOD"
private const val SECTION_DIGITAL = "DIGITAL"
private const val SECTION_PET = "PET"
private const val SECTION_MEDICINE = "MEDICINE"

/**
 * 物品总览：食品 / 数码 / 宠物 / 药品 四个入口，点击进入各自详情
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsHubScreen(
    fridgeViewModel: FridgeViewModel,
    digitalViewModel: DigitalViewModel,
    petViewModel: PetViewModel,
    medicineViewModel: MedicineViewModel,
    statsViewModel: StatsViewModel,
    onOpenDrawer: () -> Unit
) {
    var section by rememberSaveable { mutableStateOf<String?>(null) }
    var showStats by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = section != null) { section = null }
    BackHandler(enabled = showStats) { showStats = false }

    if (showStats) {
        StatsScreen(
            viewModel = statsViewModel,
            onBack = { showStats = false }
        )
        return
    }

    when (section) {
        SECTION_FOOD -> FridgeScreen(fridgeViewModel, onBack = { section = null })
        SECTION_DIGITAL -> DigitalScreen(digitalViewModel, onBack = { section = null })
        SECTION_PET -> PetScreen(petViewModel, onBack = { section = null })
        SECTION_MEDICINE -> MedicineScreen(medicineViewModel, onBack = { section = null })
        else -> {
            val fridgeState by fridgeViewModel.uiState.collectAsState()
            val digitalState by digitalViewModel.uiState.collectAsState()
            val petState by petViewModel.uiState.collectAsState()
            val medicineState by medicineViewModel.uiState.collectAsState()
            val spacingScale = LocalSpacingScale.current

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text("物品", color = Color.White, fontWeight = FontWeight.Bold)
                        },
                        navigationIcon = {
                            IconButton(onClick = onOpenDrawer) {
                                Icon(Icons.Filled.Menu, contentDescription = "打开设置", tint = Color.White)
                            }
                        },
                        actions = {
                            IconButton(onClick = { showStats = true }) {
                                Icon(
                                    imageVector = Icons.Filled.Analytics,
                                    contentDescription = "物品统计",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary)
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp * spacingScale)
                ) {
                    HubCard(
                        emoji = "🧊",
                        title = "食品",
                        subtitle = "${fridgeState.itemCount} 件 · " +
                            "${fridgeState.subcategories.size} 个分类",
                        hint = "保质期与存放位置管理",
                        onClick = { section = SECTION_FOOD }
                    )
                    HubCard(
                        emoji = "💻",
                        title = "数码产品",
                        subtitle = "${digitalState.items.size} 件",
                        hint = "购入价、日均成本与卖出",
                        onClick = { section = SECTION_DIGITAL }
                    )
                    HubCard(
                        emoji = "🐾",
                        title = "宠物用品",
                        subtitle = "${petState.items.size} 件",
                        hint = "猫粮库存与喂养记录",
                        onClick = { section = SECTION_PET }
                    )
                    HubCard(
                        emoji = "💊",
                        title = "药品",
                        subtitle = "${medicineState.items.size} 种",
                        hint = "有效期与用药记录",
                        onClick = { section = SECTION_MEDICINE }
                    )
                }
            }
        }
    }
}

@Composable
private fun HubCard(
    emoji: String,
    title: String,
    subtitle: String,
    hint: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0xFFE0F2F1), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(text = emoji, fontSize = 22.sp)
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF212121)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = subtitle, fontSize = 12.sp, color = Primary)
                }
                Spacer(Modifier.height(4.dp))
                Text(text = hint, fontSize = 12.sp, color = Color(0xFF9E9E9E))
            }

            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFBDBDBD)
            )
        }
    }
}
