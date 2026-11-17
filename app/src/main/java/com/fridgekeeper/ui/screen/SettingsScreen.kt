package com.fridgekeeper.ui.screen

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
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
import com.fridgekeeper.data.settings.AppPage
import com.fridgekeeper.data.settings.AppSettings
import com.fridgekeeper.data.settings.CardDensity
import com.fridgekeeper.data.settings.CheckinColor
import com.fridgekeeper.ui.theme.LocalSpacingScale
import com.fridgekeeper.ui.theme.Primary

private enum class SettingsSection {
    HOME,
    PAGE_DISPLAY,
    CARD_DENSITY,
    CHECKIN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(appSettings: AppSettings, onBack: () -> Unit) {
    val state by appSettings.state.collectAsState()
    val spacingScale = LocalSpacingScale.current
    var section by rememberSaveable { mutableStateOf(SettingsSection.HOME) }

    BackHandler {
        if (section == SettingsSection.HOME) onBack() else section = SettingsSection.HOME
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (section) {
                            SettingsSection.HOME -> "设置"
                            SettingsSection.PAGE_DISPLAY -> "页面显示"
                            SettingsSection.CARD_DENSITY -> "卡片细节"
                            SettingsSection.CHECKIN -> "打卡设置"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (section == SettingsSection.HOME) onBack() else section = SettingsSection.HOME
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
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
            verticalArrangement = Arrangement.spacedBy(16.dp * spacingScale)
        ) {
            when (section) {
                SettingsSection.HOME -> {
                    SettingsEntry(
                        title = "页面显示",
                        description = "设置哪些页面显示在底部导航中",
                        onClick = { section = SettingsSection.PAGE_DISPLAY }
                    )
                    SettingsEntry(
                        title = "卡片细节",
                        description = "调节卡片和列表之间的间距",
                        onClick = { section = SettingsSection.CARD_DENSITY }
                    )
                    SettingsEntry(
                        title = "打卡设置",
                        description = "设置打卡圆圈的颜色",
                        onClick = { section = SettingsSection.CHECKIN }
                    )
                }

                SettingsSection.PAGE_DISPLAY -> {
                    SettingsSectionCard(
                        title = "页面显示",
                        description = "选择底部导航中显示的页面，至少保留一个"
                    ) {
                        AppPage.values().forEach { page ->
                            val visible = page in state.visiblePages
                            val isLastVisible = visible && state.visiblePages.size == 1
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = visible,
                                    onCheckedChange = { appSettings.setPageVisible(page, it) },
                                    enabled = !isLastVisible
                                )
                                Column {
                                    Text(page.label, fontSize = 15.sp, color = Color(0xFF212121))
                                    Text(
                                        text = if (isLastVisible) "至少需要显示一个页面" else "在底部导航中显示",
                                        fontSize = 11.sp,
                                        color = Color(0xFF888888)
                                    )
                                }
                            }
                        }
                    }
                }

                SettingsSection.CARD_DENSITY -> {
                    SettingsSectionCard(
                        title = "卡片细节",
                        description = "调整卡片和列表之间的间距，立即生效"
                    ) {
                        CardDensity.values().forEach { density ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = state.cardDensity == density,
                                    onClick = { appSettings.setCardDensity(density) }
                                )
                                Column {
                                    Text(density.label, fontSize = 15.sp, color = Color(0xFF212121))
                                    Text(
                                        density.description,
                                        fontSize = 11.sp,
                                        color = Color(0xFF888888)
                                    )
                                }
                            }
                        }
                    }
                }

                SettingsSection.CHECKIN -> {
                    SettingsSectionCard(
                        title = "打卡设置",
                        description = "选择打卡圆圈的颜色，已打卡时以该颜色填充"
                    ) {
                        CheckinColor.values().forEach { color ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { appSettings.setCheckinColor(color) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(Color(color.argb), CircleShape)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = color.label,
                                    fontSize = 15.sp,
                                    color = Color(0xFF212121),
                                    modifier = Modifier.weight(1f)
                                )
                                RadioButton(
                                    selected = state.checkinColor == color,
                                    onClick = { appSettings.setCheckinColor(color) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SettingsEntry(title: String, description: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF212121))
                Spacer(Modifier.height(4.dp))
                Text(description, fontSize = 12.sp, color = Color(0xFF888888))
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF888888)
            )
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF212121))
            Spacer(Modifier.height(3.dp))
            Text(description, fontSize = 12.sp, color = Color(0xFF888888))
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}
