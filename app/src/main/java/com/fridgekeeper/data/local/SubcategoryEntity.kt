package com.fridgekeeper.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subcategories")
data class SubcategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long,
    val name: String,
    val quantityMode: String = "COUNT",
    val trackDailyCostDefault: Boolean = false,
    val isBuiltin: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
