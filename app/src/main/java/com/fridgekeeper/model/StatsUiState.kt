package com.fridgekeeper.model

data class StatsUiState(
    val dailyCostRows: List<StatsRow> = emptyList(),
    val quantityRows: List<StatsRow> = emptyList(),
    val isLoading: Boolean = true
)

data class StatsRow(
    val id: String,
    val name: String,
    val moduleLabel: String,
    val subcategoryName: String,
    val locationLabel: String?,
    val dailyCost: Double?,
    val remainingQuantity: String
)
