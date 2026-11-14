package com.fridgekeeper.model

import com.fridgekeeper.data.local.MedicineEntity

data class MedicineItemUiState(
    val id: Long,
    val name: String,
    val expiryDate: Long,
    val price: Double?,
    val remainingDays: Int,
    val isExpired: Boolean,
    val isExpiringSoon: Boolean
) {
    companion object {
        fun fromEntity(
            entity: MedicineEntity,
            now: Long = System.currentTimeMillis()
        ): MedicineItemUiState {
            val remaining = DateUtils.calculateRemainingDays(entity.expiryDate, now)
            return MedicineItemUiState(
                id = entity.id,
                name = entity.name,
                expiryDate = entity.expiryDate,
                price = entity.price,
                remainingDays = remaining,
                isExpired = DateUtils.isExpired(remaining),
                isExpiringSoon = DateUtils.isExpiringSoon(remaining)
            )
        }
    }
}

data class MedicineUiState(
    val items: List<MedicineItemUiState> = emptyList(),
    val isLoading: Boolean = true
)
