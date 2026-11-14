package com.fridgekeeper.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 物品使用记录：记录某件物品的一次使用情况
 */
@Entity(tableName = "usage_records")
data class UsageRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemType: String,
    val itemId: Long,
    val itemName: String,
    val usageText: String,
    val usedAt: Long,
    val scene: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_FRIDGE = "FRIDGE"
        const val TYPE_DIGITAL = "DIGITAL"
        const val TYPE_PET = "PET"
        const val TYPE_MEDICINE = "MEDICINE"
    }
}
