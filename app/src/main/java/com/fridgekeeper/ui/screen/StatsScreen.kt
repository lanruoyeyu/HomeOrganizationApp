package com.fridgekeeper.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.fridgekeeper.data.local.CategoryEntity
import com.fridgekeeper.model.MapMarkerUiState
import com.fridgekeeper.model.MapPlaceUiState
import com.fridgekeeper.model.ModuleStats
import com.fridgekeeper.model.PickerItem
import com.fridgekeeper.ui.components.AddNameDialog
import com.fridgekeeper.ui.components.ItemPickerDialog
import com.fridgekeeper.ui.components.MarkerInfoSheet
import com.fridgekeeper.ui.components.RoomMap
import com.fridgekeeper.ui.components.StatsCard
import com.fridgekeeper.ui.theme.Primary
import com.fridgekeeper.viewmodel.StatsViewModel

private const val MAP_MODE_LIST = 0
private const val MAP_MODE_MAP = 1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val markers by viewModel.markers.collectAsState()
    val places by viewModel.places.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val subcategories by viewModel.subcategories.collectAsState()

    var pickerPos by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var pickerItems by remember { mutableStateOf<List<PickerItem>>(emptyList()) }
    var selectedMarker by remember { mutableStateOf<MapMarkerUiState?>(null) }
    var selectedPlace by remember { mutableStateOf<MapPlaceUiState?>(null) }
    var pendingPlacePos by remember { mutableStateOf<Pair<Float, Float>?>(null) }

    // 房间地图默认以列表显示
    var mapMode by rememberSaveable { mutableIntStateOf(MAP_MODE_LIST) }
    var placing by remember { mutableStateOf(false) }

    var showAddCategory by remember { mutableStateOf(false) }
    var addSubCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("物品统计", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary)
            )
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
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                StatsCard(icon = "📦", title = "冰箱物品", stats = uiState.fridgeStats)
                StatsCard(icon = "📱", title = "数码产品", stats = uiState.digitalStats)
                StatsCard(icon = "🐾", title = "宠物用品", stats = uiState.petStats)
                StatsCard(icon = "💊", title = "药品", stats = uiState.medicineStats)
                StatsCard(
                    icon = "📊",
                    title = "总计",
                    stats = ModuleStats(
                        itemCount = uiState.fridgeStats.itemCount + uiState.digitalStats.itemCount +
                            uiState.petStats.itemCount + uiState.medicineStats.itemCount,
                        totalCost = uiState.digitalStats.totalCost + uiState.petStats.totalCost +
                            uiState.medicineStats.totalCost,
                        dailyCost = uiState.totalDailyCost,
                        hasPrice = true
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ---------- 物品分类管理 ----------
                SectionHeader(
                    title = "物品分类",
                    actionText = "+ 大类",
                    onAction = { showAddCategory = true }
                )

                if (categories.isEmpty()) {
                    Text(
                        text = "暂无分类，点击右上角 + 大类 添加",
                        fontSize = 13.sp,
                        color = Color(0xFF888888),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                } else {
                    categories.forEach { category ->
                        CategoryCard(
                            category = category,
                            subcategoryNames = subcategories
                                .filter { it.categoryId == category.id }
                                .map { it.name },
                            onAddSubcategory = { addSubCategory = category }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ---------- 房间地图 ----------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "房间地图",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = mapMode == MAP_MODE_LIST,
                        onClick = { mapMode = MAP_MODE_LIST },
                        label = { Text("列表") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = mapMode == MAP_MODE_MAP,
                        onClick = { mapMode = MAP_MODE_MAP },
                        label = { Text("地图") }
                    )
                    IconButton(onClick = {
                        mapMode = MAP_MODE_MAP
                        placing = true
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "添加位置",
                            tint = Primary
                        )
                    }
                }

                if (placing) {
                    Surface(
                        color = Primary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 12.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "请在地图上点击要添加的位置",
                                fontSize = 13.sp,
                                color = Primary,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { placing = false }) { Text("取消") }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (mapMode == MAP_MODE_MAP) {
                    RoomMap(
                        markers = markers,
                        places = places,
                        onTapMarker = { marker -> selectedMarker = marker },
                        onTapPlace = { place -> selectedPlace = place },
                        onTapBlank = { x, y ->
                            if (placing) {
                                pendingPlacePos = Pair(x, y)
                                placing = false
                            }
                        },
                        onLongPress = { x, y ->
                            pickerPos = Pair(x, y)
                            viewModel.buildPickerItems { items -> pickerItems = items }
                        }
                    )
                } else {
                    MapListView(
                        places = places,
                        markers = markers,
                        onDeletePlace = { place -> viewModel.deletePlace(place.id) }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showAddCategory) {
        AddNameDialog(
            title = "添加物品大类",
            label = "大类名称",
            hint = "例如：书籍、工具、纪念品",
            onDismiss = { showAddCategory = false },
            onConfirm = { name, onResult -> viewModel.addCategory(name, onResult) }
        )
    }

    addSubCategory?.let { category ->
        AddNameDialog(
            title = "为「${category.name}」添加小类",
            label = "小类名称",
            hint = "例如：小说、漫画、教材",
            onDismiss = { addSubCategory = null },
            onConfirm = { name, onResult ->
                viewModel.addSubcategory(category.id, name, onResult)
            }
        )
    }

    pendingPlacePos?.let { pos ->
        AddNameDialog(
            title = "添加具体位置",
            label = "位置名称",
            hint = "例如：书架、衣柜、阳台",
            onDismiss = { pendingPlacePos = null },
            onConfirm = { name, onResult ->
                viewModel.addPlace(name, pos.first, pos.second, onResult)
            }
        )
    }

    pickerPos?.let { pos ->
        ItemPickerDialog(
            items = pickerItems,
            onDismiss = { pickerPos = null },
            onPick = { item ->
                viewModel.addMarker(
                    itemType = item.itemType,
                    itemId = item.itemId,
                    displayName = item.displayName,
                    posX = pos.first,
                    posY = pos.second
                )
                pickerPos = null
            }
        )
    }

    selectedMarker?.let { marker ->
        val infoText = "${marker.itemType.icon} ${marker.itemDisplayName}\n来源：${marker.itemType.label}"
        MarkerInfoSheet(
            marker = marker,
            infoText = infoText,
            onDismiss = { selectedMarker = null },
            onRemove = {
                viewModel.deleteMarker(marker.id)
                selectedMarker = null
            }
        )
    }

    selectedPlace?.let { place ->
        AlertDialog(
            onDismissRequest = { selectedPlace = null },
            title = { Text(place.name) },
            text = { Text("房间地图上的具体位置", fontSize = 14.sp, color = Color(0xFF555555)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlace(place.id)
                    selectedPlace = null
                }) {
                    Text("删除", color = Color(0xFFFF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedPlace = null }) { Text("关闭") }
            }
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionText: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121),
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onAction) {
            Text(actionText, color = Primary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun CategoryCard(
    category: CategoryEntity,
    subcategoryNames: List<String>,
    onAddSubcategory: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 8.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = category.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF212121),
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onAddSubcategory,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("+ 小类", color = Primary, fontSize = 13.sp)
                }
            }
            if (subcategoryNames.isEmpty()) {
                Text(
                    text = "暂无小类",
                    fontSize = 12.sp,
                    color = Color(0xFF999999)
                )
            } else {
                subcategoryNames.forEach { name ->
                    Text(
                        text = "· $name",
                        fontSize = 13.sp,
                        color = Color(0xFF555555),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MapListView(
    places: List<MapPlaceUiState>,
    markers: List<MapMarkerUiState>,
    onDeletePlace: (MapPlaceUiState) -> Unit
) {
    if (places.isEmpty() && markers.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无位置\n点击右上角 + 添加具体位置",
                fontSize = 14.sp,
                color = Color(0xFF888888),
                textAlign = TextAlign.Center
            )
        }
        return
    }

    if (places.isNotEmpty()) {
        Text(
            text = "具体位置",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF555555),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        places.forEach { place ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "📍", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = place.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF212121),
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { onDeletePlace(place) }) {
                    Text("删除", color = Color(0xFF999999), fontSize = 13.sp)
                }
            }
        }
    }

    if (markers.isNotEmpty()) {
        Text(
            text = "已标注物品",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF555555),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        markers.forEach { marker ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = marker.itemType.icon, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = marker.itemDisplayName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF212121)
                    )
                    Text(
                        text = marker.itemType.label,
                        fontSize = 12.sp,
                        color = Color(0xFF888888)
                    )
                }
            }
        }
    }
}
