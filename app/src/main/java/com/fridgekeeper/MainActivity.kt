package com.fridgekeeper

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
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.fridgekeeper.ui.screen.DigitalScreen
import com.fridgekeeper.ui.screen.FridgeScreen
import com.fridgekeeper.ui.screen.LedgerScreen
import com.fridgekeeper.ui.screen.MedicineScreen
import com.fridgekeeper.ui.screen.PetScreen
import com.fridgekeeper.ui.screen.StatsScreen
import com.fridgekeeper.ui.theme.Background
import com.fridgekeeper.ui.theme.FridgeKeeperTheme
import com.fridgekeeper.viewmodel.DigitalViewModel
import com.fridgekeeper.viewmodel.FridgeViewModel
import com.fridgekeeper.viewmodel.LedgerViewModel
import com.fridgekeeper.viewmodel.MedicineViewModel
import com.fridgekeeper.viewmodel.PetViewModel
import com.fridgekeeper.viewmodel.StatsViewModel
import com.fridgekeeper.viewmodel.ViewModelFactory

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
            app.ledgerSettings
        )
    }

    private val ledgerViewModel: LedgerViewModel by viewModels { viewModelFactory }
    private val fridgeViewModel: FridgeViewModel by viewModels { viewModelFactory }
    private val digitalViewModel: DigitalViewModel by viewModels { viewModelFactory }
    private val petViewModel: PetViewModel by viewModels { viewModelFactory }
    private val medicineViewModel: MedicineViewModel by viewModels { viewModelFactory }
    private val statsViewModel: StatsViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FridgeKeeperTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Background
                ) {
                    MainScreen(
                        ledgerViewModel = ledgerViewModel,
                        fridgeViewModel = fridgeViewModel,
                        digitalViewModel = digitalViewModel,
                        petViewModel = petViewModel,
                        medicineViewModel = medicineViewModel,
                        statsViewModel = statsViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    ledgerViewModel: LedgerViewModel,
    fridgeViewModel: FridgeViewModel,
    digitalViewModel: DigitalViewModel,
    petViewModel: PetViewModel,
    medicineViewModel: MedicineViewModel,
    statsViewModel: StatsViewModel
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null) },
                    label = { Text("记账") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.Kitchen, contentDescription = null) },
                    label = { Text("冰箱") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.Devices, contentDescription = null) },
                    label = { Text("数码") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Filled.Pets, contentDescription = null) },
                    label = { Text("宠物") }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Filled.Medication, contentDescription = null) },
                    label = { Text("药品") }
                )
                NavigationBarItem(
                    selected = selectedTab == 5,
                    onClick = { selectedTab = 5 },
                    icon = { Icon(Icons.Filled.Analytics, contentDescription = null) },
                    label = { Text("统计") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> LedgerScreen(viewModel = ledgerViewModel)
                1 -> FridgeScreen(viewModel = fridgeViewModel)
                2 -> DigitalScreen(viewModel = digitalViewModel)
                3 -> PetScreen(viewModel = petViewModel)
                4 -> MedicineScreen(viewModel = medicineViewModel)
                5 -> StatsScreen(viewModel = statsViewModel)
            }
        }
    }
}
