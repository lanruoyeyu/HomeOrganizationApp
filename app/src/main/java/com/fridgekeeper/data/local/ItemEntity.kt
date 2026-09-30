package com.fridgekeeper.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String = "",
    val subcategoryId: Long = 0,
    val quantity: Int,
    val storageDate: Long,
    val expirationDate: Long,
    val locationId: Long? = null,
    val trackDailyCost: Boolean = false,
    val buyPrice: Double? = null,
    val buyAt: Long? = null,
    val isCleared: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
