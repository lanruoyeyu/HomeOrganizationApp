package com.fridgekeeper.tassetbook

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fridgekeeper.tassetbook.data.AppRepository
import com.fridgekeeper.tassetbook.ui.TAssetBookTheme
import com.fridgekeeper.tassetbook.ui.screens.AccountsScreen
import com.fridgekeeper.tassetbook.ui.screens.AddAccountScreen
import com.fridgekeeper.tassetbook.ui.screens.AddAssetScreen
import com.fridgekeeper.tassetbook.ui.screens.AssetsScreen
import com.fridgekeeper.tassetbook.ui.screens.EstimateTradeScreen
import com.fridgekeeper.tassetbook.ui.screens.ExportConfigScreen
import com.fridgekeeper.tassetbook.ui.screens.FinanceScreen
import com.fridgekeeper.tassetbook.ui.screens.MineScreen
import com.fridgekeeper.tassetbook.ui.screens.StatisticsScreen
import com.fridgekeeper.tassetbook.ui.screens.StockManageScreen
import com.fridgekeeper.tassetbook.ui.screens.TradeOperationScreen
import com.fridgekeeper.tassetbook.ui.screens.TradeRecordScreen

private object Routes {
    const val ASSETS = "assets"
    const val STATISTICS = "statistics"
    const val MINE = "mine"
    const val FINANCE = "finance"
    const val ADD_ASSET = "addAsset"
    const val TRADE_OP = "tradeOperation"
    const val TRADE_RECORD = "tradeRecord"
    const val ESTIMATE = "estimate"
    const val EXPORT = "exportConfig"
    const val STOCK_MANAGE = "stockManage"
    const val STOCK_DETAIL = "stockDetail"
    const val ADD_ACCOUNT = "addAccount"
}

private data class RootTab(val route: String, val label: String)

private val rootTabs = listOf(
    RootTab(Routes.ASSETS, "股票"),
    RootTab(Routes.FINANCE, "资产"),
    RootTab(Routes.MINE, "我的")
)

/**
 * 股票做T模块入口，作为当前应用底部导航的一个页面。
 * 根页面使用顶部页签切换，二级页面保留各自返回按钮。
 */
@Composable
fun TAssetBookPage() {
    val context = LocalContext.current
    remember(context) {
        AppRepository.init(context.applicationContext.filesDir)
    }

    TAssetBookTheme {
        val navController = rememberNavController()
        val backStack by navController.currentBackStackEntryAsState()
        val currentRoute = backStack?.destination?.route?.substringBefore('?')
        val selectedIndex = rootTabs.indexOfFirst { it.route == currentRoute }

        BackHandler(enabled = navController.previousBackStackEntry != null) {
            navController.popBackStack()
        }

        Column(modifier = Modifier.fillMaxSize()) {
            if (selectedIndex >= 0) {
                TabRow(
                    selectedTabIndex = selectedIndex,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    rootTabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = index == selectedIndex,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(Routes.ASSETS) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            text = { Text(tab.label) }
                        )
                    }
                }
            }

            NavHost(
                navController = navController,
                startDestination = Routes.ASSETS,
                modifier = Modifier.weight(1f)
            ) {
                composable(Routes.ASSETS) {
                    AssetsScreen(
                        onAddAsset = { navController.navigate(Routes.ADD_ASSET) },
                        onEditAsset = { id -> navController.navigate("${Routes.ADD_ASSET}?assetId=$id") },
                        onTradeOp = { assetId, assetName, status ->
                            navController.navigate(
                                "${Routes.TRADE_OP}?assetId=$assetId&assetName=$assetName" +
                                    "&tradeStatus=$status&source=asset"
                            )
                        },
                        onTradeRecord = { assetId, assetName ->
                            navController.navigate(
                                "${Routes.TRADE_RECORD}?assetId=$assetId&assetName=$assetName"
                            )
                        },
                        onEstimate = { assetId, assetName ->
                            navController.navigate(
                                "${Routes.ESTIMATE}?assetId=$assetId&assetName=$assetName"
                            )
                        },
                        onManageStocks = { navController.navigate(Routes.STOCK_MANAGE) },
                        onStatistics = { navController.navigate(Routes.STATISTICS) }
                    )
                }
                composable(Routes.STOCK_MANAGE) {
                    StockManageScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.STATISTICS) {
                    StatisticsScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.FINANCE) {
                    AccountsScreen(
                        onAddAccount = { navController.navigate(Routes.ADD_ACCOUNT) },
                        onEditAccount = { id ->
                            navController.navigate("${Routes.ADD_ACCOUNT}?accountId=$id")
                        },
                        onStockDetail = { navController.navigate(Routes.STOCK_DETAIL) }
                    )
                }
                composable(Routes.STOCK_DETAIL) {
                    FinanceScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    "${Routes.ADD_ACCOUNT}?accountId={accountId}",
                    arguments = listOf(
                        navArgument("accountId") {
                            nullable = true
                            type = NavType.StringType
                            defaultValue = null
                        }
                    )
                ) { entry ->
                    AddAccountScreen(
                        accountId = entry.arguments?.getString("accountId"),
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Routes.MINE) {
                    MineScreen(onExport = { navController.navigate(Routes.EXPORT) })
                }
                composable(
                    "${Routes.ADD_ASSET}?assetId={assetId}",
                    arguments = listOf(
                        navArgument("assetId") {
                            nullable = true
                            type = NavType.StringType
                            defaultValue = null
                        }
                    )
                ) { entry ->
                    AddAssetScreen(
                        assetId = entry.arguments?.getString("assetId"),
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    "${Routes.TRADE_OP}?assetId={assetId}&assetName={assetName}" +
                        "&tradeStatus={tradeStatus}&tradeId={tradeId}" +
                        "&source={source}&isEdit={isEdit}",
                    arguments = listOf(
                        navArgument("assetId") { type = NavType.StringType; defaultValue = "" },
                        navArgument("assetName") { type = NavType.StringType; defaultValue = "" },
                        navArgument("tradeStatus") { type = NavType.StringType; defaultValue = "" },
                        navArgument("tradeId") {
                            nullable = true
                            type = NavType.StringType
                            defaultValue = null
                        },
                        navArgument("source") { type = NavType.StringType; defaultValue = "asset" },
                        navArgument("isEdit") { type = NavType.StringType; defaultValue = "false" }
                    )
                ) { entry ->
                    TradeOperationScreen(
                        assetId = entry.arguments?.getString("assetId").orEmpty(),
                        assetName = entry.arguments?.getString("assetName").orEmpty(),
                        tradeStatusArg = entry.arguments?.getString("tradeStatus").orEmpty(),
                        tradeId = entry.arguments?.getString("tradeId"),
                        source = entry.arguments?.getString("source") ?: "asset",
                        isEdit = entry.arguments?.getString("isEdit") == "true",
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    "${Routes.TRADE_RECORD}?assetId={assetId}&assetName={assetName}",
                    arguments = listOf(
                        navArgument("assetId") { type = NavType.StringType; defaultValue = "" },
                        navArgument("assetName") { type = NavType.StringType; defaultValue = "" }
                    )
                ) { entry ->
                    TradeRecordScreen(
                        assetId = entry.arguments?.getString("assetId").orEmpty(),
                        assetName = entry.arguments?.getString("assetName").orEmpty(),
                        onBack = { navController.popBackStack() },
                        onEditTrade = { assetId, assetName, tradeId, status ->
                            navController.navigate(
                                "${Routes.TRADE_OP}?assetId=$assetId&assetName=$assetName" +
                                    "&tradeStatus=$status&tradeId=$tradeId" +
                                    "&source=tradeRecord&isEdit=true"
                            )
                        },
                        onSupplement = { assetId, assetName, tradeId, status ->
                            navController.navigate(
                                "${Routes.TRADE_OP}?assetId=$assetId&assetName=$assetName" +
                                    "&tradeStatus=$status&tradeId=$tradeId" +
                                    "&source=tradeRecord&isEdit=false"
                            )
                        }
                    )
                }
                composable(
                    "${Routes.ESTIMATE}?assetId={assetId}&assetName={assetName}",
                    arguments = listOf(
                        navArgument("assetId") { type = NavType.StringType; defaultValue = "" },
                        navArgument("assetName") { type = NavType.StringType; defaultValue = "" }
                    )
                ) { entry ->
                    EstimateTradeScreen(
                        assetId = entry.arguments?.getString("assetId").orEmpty(),
                        assetName = entry.arguments?.getString("assetName").orEmpty(),
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Routes.EXPORT) {
                    ExportConfigScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
