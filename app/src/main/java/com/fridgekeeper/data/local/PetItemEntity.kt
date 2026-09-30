package com.fridgekeeper.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pet_items")
data class PetItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val categoryId: Long,
    val purchasePrice: Double,
    val purchaseDate: Long,
    val quantityType: String,
    val weightUnit: String? = null,
    val quantity: Double,
    val tags: String = "",
    val locationId: Long? = null,
    val trackDailyCost: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
