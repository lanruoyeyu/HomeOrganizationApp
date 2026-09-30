package com.fridgekeeper.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val kind: String,
    val isBuiltin: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
