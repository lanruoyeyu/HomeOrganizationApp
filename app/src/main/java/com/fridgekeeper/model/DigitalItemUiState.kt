package com.fridgekeeper.model

import com.fridgekeeper.data.local.DigitalItemEntity

data class DigitalItemUiState(
    val id: Long,
    val name: String,
    val purchasePrice: Double,
    val purchaseDate: Long,
    val sellPrice: Double?,
    val sellDate: Long?,
    val isSold: Boolean,
    val daysHeld: Int,
    val dailyCost: Double,
    val profit: Double?,
    val locationLabel: String?
) {
    companion object {
        fun fromEntity(
            entity: DigitalItemEntity,
            locationLabel: String? = null,
            now: Long = System.currentTimeMillis()
        ): DigitalItemUiState {
            val isSold = entity.sellPrice != null && entity.sellDate != null
            val endDate = if (isSold) entity.sellDate!! else now
            val daysHeld = DateUtils.calculateDaysHeld(entity.purchaseDate, endDate)
            val dailyCost = InventoryMath.dailyCost(entity.purchasePrice, entity.purchaseDate, endDate)
            val profit = if (isSold) entity.sellPrice!! - entity.purchasePrice else null
            return DigitalItemUiState(
                id = entity.id,
                name = entity.name,
                purchasePrice = entity.purchasePrice,
                purchaseDate = entity.purchaseDate,
                sellPrice = entity.sellPrice,
                sellDate = entity.sellDate,
                isSold = isSold,
                daysHeld = daysHeld,
                dailyCost = dailyCost,
                profit = profit,
                locationLabel = locationLabel
            )
        }
    }
}

data class DigitalUiState(
    val items: List<DigitalItemUiState> = emptyList(),
    val locations: List<com.fridgekeeper.data.local.LocationEntity> = emptyList(),
    val isLoading: Boolean = true
)
