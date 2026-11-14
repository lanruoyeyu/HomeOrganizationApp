package com.fridgekeeper.data.repository

import com.fridgekeeper.data.local.AppDatabase
import com.fridgekeeper.data.local.ItemDao
import com.fridgekeeper.data.local.ItemEntity
import com.fridgekeeper.data.local.SubcategoryCount
import kotlinx.coroutines.flow.Flow

class FridgeRepository(private val itemDao: ItemDao) {

    fun observeActiveItems(): Flow<List<ItemEntity>> = itemDao.observeActiveItems()

    fun observeActiveByCategory(categoryId: Long): Flow<List<ItemEntity>> =
        itemDao.observeActiveByCategory(categoryId)

    fun observeSubcategoryCounts(categoryId: Long): Flow<List<SubcategoryCount>> =
        itemDao.observeSubcategoryCounts(categoryId)

    suspend fun addItem(item: ItemEntity): Long = itemDao.insert(item)

    suspend fun getById(id: Long): ItemEntity? = itemDao.getById(id)

    suspend fun updateItem(item: ItemEntity) = itemDao.update(item)

    suspend fun updateQuantity(id: Long, quantity: Int) {
        itemDao.updateQuantity(id, quantity, System.currentTimeMillis())
    }

    suspend fun clearItem(id: Long) {
        itemDao.markCleared(id, System.currentTimeMillis())
    }

    companion object {
        @Volatile
        private var INSTANCE: FridgeRepository? = null

        fun getRepository(database: AppDatabase): FridgeRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = FridgeRepository(database.itemDao())
                INSTANCE = instance
                instance
            }
        }
    }
}
