package com.fridgekeeper.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 房间地图上的具体位置（如书架、衣柜、冰箱旁等）
 */
@Entity(tableName = "map_places")
data class MapPlaceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val posX: Float,
    val posY: Float,
    val createdAt: Long = System.currentTimeMillis()
)
