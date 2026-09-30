package com.fridgekeeper.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val room: String,
    val spot: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    val displayName: String get() = "$room-$spot"
}
