package com.fridgekeeper.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fridgekeeper.model.CouponItemUiState
import com.fridgekeeper.model.WelfareUiState
import com.fridgekeeper.data.settings.CouponSettings
import com.fridgekeeper.ui.components.AddCouponDialog
import com.fridgekeeper.ui.components.CouponCard
import com.fridgekeeper.ui.components.CouponRankList
import com.fridgekeeper.ui.components.CouponSummaryCard
import com.fridgekeeper.ui.components.SourceStatRow
import com.fridgekeeper.ui.components.SwipeEditDeleteBox
import com.fridgekeeper.ui.theme.AlertRed
import com.fridgekeeper.ui.theme.AlertRedBg
import com.fridgekeeper.ui.theme.LocalSpacingScale
import com.fridgekeeper.ui.theme.Primary
import com.fridgekeeper.viewmodel.WelfareViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelfareScreen(viewModel: WelfareViewModel, onOpenDrawer: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val couponSettings = remember { CouponSettings(context.applicationContext) }
    val couponOptions by couponSettings.state.collectAsState()
    var showAddCoupon by remember { mutableStateOf(false) }
    var editCoupon by remember { mutableStateOf<CouponItemUiState?>(null) }
    var showStatistics by remember { mutableStateOf(false) }
    var showCouponSettings by remember { mutableStateOf(false) }

    if (showCouponSettings) {
        CouponOptionsScreen(
            settings = couponSettings,
            onBack = { showCouponSettings = false }
        )
        return
    }

    if (showStatistics) {
        WelfareStatsScreen(
            state = uiState,
            onBack = { showStatistics = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("福利", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "打开设置", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showCouponSettings = true }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "优惠设置",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { showStatistics = true }) {
                        Icon(
                            imageVector = Icons.Filled.Analytics,
                            contentDescription = "统计",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCoupon = true },
                containerColor = Primary
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "添加优惠券",
                    tint = Color.White
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else {
                CouponTab(
                    state = uiState,
                    onToggleUsed = viewModel::markCouponUsed,
                    onEdit = { editCoupon = it },
                    onDelete = viewModel::deleteCoupon
                )
            }
        }
    }

    if (showAddCoupon) {
        AddCouponDialog(
            sourceOptions = couponOptions.sources,
            typeOptions = couponOptions.types,
            onDismiss = { showAddCoupon = false },
            onConfirm = { draft ->
                viewModel.addCoupon(
                    source = draft.source,
                    type = draft.type,
                    threshold = draft.threshold,
                    amount = draft.amount,
                    validFrom = draft.validFrom,
                    validUntil = draft.validUntil,
                    scene = draft.scene,
                    note = draft.note
                )
                showAddCoupon = false
            }
        )
    }

    editCoupon?.let { target ->
        AddCouponDialog(
            editItem = target,
            sourceOptions = couponOptions.sources,
            typeOptions = couponOptions.types,
            onDismiss = { editCoupon = null },
            onConfirm = { draft ->
                viewModel.updateCoupon(
                    id = target.id,
                    source = draft.source,
                    type = draft.type,
                    threshold = draft.threshold,
                    amount = draft.amount,
                    validFrom = draft.validFrom,
                    validUntil = draft.validUntil,
                    scene = draft.scene,
                    note = draft.note
                )
                editCoupon = null
            }
        )
    }
}

@Composable
private fun CouponTab(
    state: WelfareUiState,
    onToggleUsed: (Long, Boolean) -> Unit,
    onEdit: (CouponItemUiState) -> Unit,
    onDelete: (Long) -> Unit
) {
    val spacingScale = LocalSpacingScale.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 10.dp * spacingScale, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp * spacingScale)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                CouponSummaryCard(state)

                if (state.expiringCount > 0) {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AlertRedBg, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "⏰ ${state.expiringCount} 张优惠券 3 天内到期，" +
                                "共 ¥${state.expiringValue}，记得使用",
                            fontSize = 13.sp,
                            color = AlertRed
                        )
                    }
                }

                Spacer(Modifier.height(14.dp * spacingScale))
                Text(
                    text = "全部优惠券（${state.coupons.size}）",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF212121)
                )
            }
        }

        if (state.coupons.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无优惠券\n点击右下角 + 添加",
                        fontSize = 15.sp,
                        color = Color(0xFF9E9E9E),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(items = state.coupons, key = { it.id }) { coupon ->
                SwipeEditDeleteBox(
                    onEdit = { onEdit(coupon) },
                    onDelete = { onDelete(coupon.id) }
                ) {
                    CouponCard(
                        item = coupon,
                        onToggleUsed = { onToggleUsed(coupon.id, !coupon.isUsed) }
                    )
                }
            }
        }
    }
}

/** 优惠统计二级页面：来源统计与优惠力度 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WelfareStatsScreen(state: WelfareUiState, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("优惠统计", color = Color.White, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "返回",
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
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            if (state.sourceStats.isEmpty() && state.bestCoupons.isEmpty()) {
                Text(
                    text = "暂无统计数据",
                    fontSize = 14.sp,
                    color = Color(0xFF9E9E9E),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp)
                )
            } else {
                if (state.sourceStats.isNotEmpty()) {
                    Text(
                        text = "来源统计",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF212121)
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        SourceStatRow(state.sourceStats)
                    }
                    Spacer(Modifier.height(20.dp))
                }

                if (state.bestCoupons.isNotEmpty()) {
                    Text(
                        text = "优惠力度",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF212121)
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        CouponRankList(state.bestCoupons)
                    }
                }
            }
        }
    }
}
