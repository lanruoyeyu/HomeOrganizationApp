package com.fridgekeeper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.fridgekeeper.data.settings.AppPage
import com.fridgekeeper.data.settings.AppSettings
import com.fridgekeeper.tassetbook.TAssetBookPage
import com.fridgekeeper.ui.screen.CheckinScreen
import com.fridgekeeper.ui.screen.ItemsHubScreen
import com.fridgekeeper.ui.screen.LedgerScreen
import com.fridgekeeper.ui.screen.SettingsScreen
import com.fridgekeeper.ui.screen.StatsScreen
import com.fridgekeeper.ui.screen.WelfareScreen
import com.fridgekeeper.ui.theme.Background
import com.fridgekeeper.ui.theme.FridgeKeeperTheme
import com.fridgekeeper.ui.theme.LocalSpacingScale
import com.fridgekeeper.viewmodel.DigitalViewModel
import com.fridgekeeper.viewmodel.FridgeViewModel
import com.fridgekeeper.viewmodel.LedgerViewModel
import com.fridgekeeper.viewmodel.MedicineViewModel
import com.fridgekeeper.viewmodel.PetViewModel
import com.fridgekeeper.viewmodel.StatsViewModel
import com.fridgekeeper.viewmodel.ViewModelFactory
import com.fridgekeeper.viewmodel.WelfareViewModel

class MainActivity : ComponentActivity() {

    private val viewModelFactory: ViewModelFactory by lazy {
        val app = application as FridgeApp
        ViewModelFactory(
            app.repository,
            app.digitalRepository,
            app.petRepository,
            app.medicineRepository,
            app.statsRepository,
            app.catalogRepository,
            app.mapMarkerRepository,
            app.mapPlaceRepository,
            app.usageRecordRepository,
            app.ledgerRepository,
            app.ledgerAiParser,
            app.ledgerSettings,
            app.dataBackupManager,
            app.ledgerExcelManager,
            app.welfareRepository
        )
    }

    private val appSettings: AppSettings by lazy { (application as FridgeApp).appSettings }
    private val ledgerViewModel: LedgerViewModel by viewModels { viewModelFactory }
    private val fridgeViewModel: FridgeViewModel by viewModels { viewModelFactory }
    private val digitalViewModel: DigitalViewModel by viewModels { viewModelFactory }
    private val petViewModel: PetViewModel by viewModels { viewModelFactory }
    private val medicineViewModel: MedicineViewModel by viewModels { viewModelFactory }
    private val statsViewModel: StatsViewModel by viewModels { viewModelFactory }
    private val welfareViewModel: WelfareViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        setContent {
            FridgeKeeperTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Background) {
                    MainScreen(
                        appSettings = appSettings,
                        ledgerViewModel = ledgerViewModel,
                        fridgeViewModel = fridgeViewModel,
                        digitalViewModel = digitalViewModel,
                        petViewModel = petViewModel,
                        medicineViewModel = medicineViewModel,
                        statsViewModel = statsViewModel,
                        welfareViewModel = welfareViewModel
                    )
                }
            }
        }
    }

    /** 优惠券到期、打卡提醒需要通知权限（Android 13+） */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
    }
}

@Composable
fun MainScreen(
    appSettings: AppSettings,
    ledgerViewModel: LedgerViewModel,
    fridgeViewModel: FridgeViewModel,
    digitalViewModel: DigitalViewModel,
    petViewModel: PetViewModel,
    medicineViewModel: MedicineViewModel,
    statsViewModel: StatsViewModel,
    welfareViewModel: WelfareViewModel
) {
    val settingsState by appSettings.state.collectAsState()
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var selectedPageName by rememberSaveable { mutableStateOf(AppPage.LEDGER.name) }
    val visiblePages = AppPage.values().filter { it in settingsState.visiblePages }
    val currentPage = visiblePages.firstOrNull { it.name == selectedPageName } ?: visiblePages.first()
    val openSettings: () -> Unit = { showSettings = true }

    LaunchedEffect(currentPage) { selectedPageName = currentPage.name }

    CompositionLocalProvider(LocalSpacingScale provides settingsState.cardDensity.spacingScale) {
        if (showSettings) {
            SettingsScreen(
                appSettings = appSettings,
                onBack = { showSettings = false }
            )
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        visiblePages.forEach { page ->
                            NavigationBarItem(
                                selected = currentPage == page,
                                onClick = { selectedPageName = page.name },
                                icon = {
                                    Icon(
                                        imageVector = when (page) {
                                            AppPage.LEDGER -> Icons.Filled.AccountBalanceWallet
                                            AppPage.ITEMS -> Icons.Filled.Inventory2
                                            AppPage.STATS -> Icons.Filled.Analytics
                                            AppPage.WELFARE -> Icons.Filled.CardGiftcard
                                            AppPage.CHECKIN -> Icons.Filled.CheckCircle
                                            AppPage.TASSET -> Icons.Filled.TrendingUp
                                        },
                                        contentDescription = page.label
                                    )
                                },
                                label = { Text(page.label) }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    when (currentPage) {
                        AppPage.LEDGER -> LedgerScreen(
                            viewModel = ledgerViewModel,
                            onOpenDrawer = openSettings
                        )
                        AppPage.ITEMS -> ItemsHubScreen(
                            fridgeViewModel = fridgeViewModel,
                            digitalViewModel = digitalViewModel,
                            petViewModel = petViewModel,
                            medicineViewModel = medicineViewModel,
                            onOpenDrawer = openSettings
                        )
                        AppPage.STATS -> StatsScreen(
                            viewModel = statsViewModel,
                            onOpenDrawer = openSettings
                        )
                        AppPage.WELFARE -> WelfareScreen(
                            viewModel = welfareViewModel,
                            onOpenDrawer = openSettings
                        )
                        AppPage.CHECKIN -> CheckinScreen(
                            viewModel = welfareViewModel,
                            onOpenDrawer = openSettings,
                            circleColor = Color(settingsState.checkinColor.argb),
                            asList = settingsState.checkinAsList,
                            onToggleList = {
                                appSettings.setCheckinAsList(!settingsState.checkinAsList)
                            }
                        )
                        AppPage.TASSET -> TAssetBookPage()
                    }
                }
            }
        }
    }
}
