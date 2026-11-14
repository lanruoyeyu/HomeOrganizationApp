package com.fridgekeeper.data.repository

import com.fridgekeeper.data.local.MapMarkerDao
import com.fridgekeeper.data.local.MapMarkerEntity
import kotlinx.coroutines.flow.Flow

class MapMarkerRepository(private val mapMarkerDao: MapMarkerDao) {

    fun observeAll(): Flow<List<MapMarkerEntity>> = mapMarkerDao.observeAll()

    suspend fun add(marker: MapMarkerEntity): Long = mapMarkerDao.insert(marker)

    suspend fun delete(id: Long) = mapMarkerDao.delete(id)
}
