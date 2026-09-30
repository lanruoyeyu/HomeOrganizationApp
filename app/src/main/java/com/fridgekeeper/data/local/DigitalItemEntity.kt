package com.fridgekeeper.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "digital_items")
data class DigitalItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val purchasePrice: Double,
    val purchaseDate: Long,
    val sellPrice: Double? = null,
    val sellDate: Long? = null,
    val locationId: Long? = null,
    val trackDailyCost: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
