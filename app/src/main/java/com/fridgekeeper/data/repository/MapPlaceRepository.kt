package com.fridgekeeper.data.repository

import com.fridgekeeper.data.local.MapPlaceDao
import com.fridgekeeper.data.local.MapPlaceEntity
import kotlinx.coroutines.flow.Flow

class MapPlaceRepository(private val mapPlaceDao: MapPlaceDao) {

    fun observeAll(): Flow<List<MapPlaceEntity>> = mapPlaceDao.observeAll()

    suspend fun add(name: String, posX: Float, posY: Float): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return -1L
        if (mapPlaceDao.countByName(trimmed) > 0) return -1L
        return mapPlaceDao.insert(
            MapPlaceEntity(
                name = trimmed,
                posX = posX.coerceIn(0f, 1f),
                posY = posY.coerceIn(0f, 1f)
            )
        )
    }

    suspend fun delete(id: Long) = mapPlaceDao.delete(id)
}
