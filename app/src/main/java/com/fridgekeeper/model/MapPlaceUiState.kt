package com.fridgekeeper.model

import com.fridgekeeper.data.local.MapPlaceEntity

/**
 * 房间地图上的具体位置
 */
data class MapPlaceUiState(
    val id: Long,
    val name: String,
    val posX: Float,
    val posY: Float
) {
    companion object {
        fun fromEntity(entity: MapPlaceEntity): MapPlaceUiState =
            MapPlaceUiState(
                id = entity.id,
                name = entity.name,
                posX = entity.posX,
                posY = entity.posY
            )
    }
}
