package com.fridgekeeper.data.repository

import com.fridgekeeper.data.local.DigitalItemDao
import com.fridgekeeper.data.local.DigitalItemEntity
import kotlinx.coroutines.flow.Flow

class DigitalRepository(private val digitalItemDao: DigitalItemDao) {

    fun observeAllItems(): Flow<List<DigitalItemEntity>> = digitalItemDao.observeAllItems()

    suspend fun addItem(item: DigitalItemEntity): Long = digitalItemDao.insert(item)

    suspend fun getById(id: Long): DigitalItemEntity? = digitalItemDao.getById(id)

    suspend fun updateItem(item: DigitalItemEntity) = digitalItemDao.update(item)

    suspend fun deleteItem(id: Long) = digitalItemDao.delete(id)

    suspend fun markSold(id: Long, sellPrice: Double, sellDate: Long, timestamp: Long) {
        digitalItemDao.markSold(id, sellPrice, sellDate, timestamp)
    }
}
