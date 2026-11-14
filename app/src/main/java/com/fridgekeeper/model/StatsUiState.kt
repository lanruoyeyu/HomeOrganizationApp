package com.fridgekeeper.model

data class StatsUiState(
    val fridgeStats: ModuleStats = ModuleStats(),
    val digitalStats: ModuleStats = ModuleStats(),
    val petStats: ModuleStats = ModuleStats(),
    val medicineStats: ModuleStats = ModuleStats(),
    val totalDailyCost: Double = 0.0,
    val isLoading: Boolean = true
)

/**
 * 单个大类的汇总数据
 */
data class ModuleStats(
    val itemCount: Int = 0,
    val totalCost: Double = 0.0,
    val dailyCost: Double = 0.0,
    val hasPrice: Boolean = false
)
