package com.fridgekeeper.model

import com.fridgekeeper.data.local.ItemEntity
import com.fridgekeeper.data.local.LocationEntity
import com.fridgekeeper.data.local.SubcategoryEntity

data class ItemUiState(
    val id: Long,
    val name: String,
    val subcategoryId: Long,
    val subcategoryName: String,
    val quantity: Int,
    val storageDate: Long,
    val expirationDate: Long,
    val remainingDays: Int,
    val isExpired: Boolean,
    val isWarning: Boolean,
    val isCleared: Boolean,
    val locationLabel: String?,
    val locationId: Long?,
    val trackDailyCost: Boolean,
    val buyPrice: Double?,
    val buyAt: Long?,
    val dailyCost: Double?
) {
    companion object {
        fun fromEntity(
            entity: ItemEntity,
            subcategoryName: String,
            locationLabel: String?,
            now: Long = System.currentTimeMillis()
        ): ItemUiState {
            val remaining = DateUtils.calculateRemainingDays(entity.expirationDate, now)
            val daily = if (entity.trackDailyCost && entity.buyPrice != null) {
                InventoryMath.dailyCost(entity.buyPrice, entity.buyAt ?: entity.storageDate, now)
            } else null
            return ItemUiState(
                id = entity.id,
                name = entity.name,
                subcategoryId = entity.subcategoryId,
                subcategoryName = subcategoryName,
                quantity = entity.quantity,
                storageDate = entity.storageDate,
                expirationDate = entity.expirationDate,
                remainingDays = remaining,
                isExpired = DateUtils.isExpired(remaining),
                isWarning = DateUtils.isWarning(remaining),
                isCleared = entity.isCleared,
                locationLabel = locationLabel,
                locationId = entity.locationId,
                trackDailyCost = entity.trackDailyCost,
                buyPrice = entity.buyPrice,
                buyAt = entity.buyAt,
                dailyCost = daily
            )
        }
    }
}

data class FridgeUiState(
    val items: List<ItemUiState> = emptyList(),
    val allItems: List<ItemUiState> = emptyList(),
    val subcategoryCount: Int = 0,
    val itemCount: Int = 0,
    val subcategories: List<SubcategoryEntity> = emptyList(),
    val locations: List<LocationEntity> = emptyList(),
    val selectedSubcategoryId: Long = 0L,
    val foodCategoryId: Long = 0L,
    val isLoading: Boolean = true
)

data class CategoryDisplay(
    val category: ItemCategory,
    val count: Int
)
