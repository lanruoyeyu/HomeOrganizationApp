package com.fridgekeeper.data.repository

import com.fridgekeeper.data.local.UsageRecordDao
import com.fridgekeeper.data.local.UsageRecordEntity
import kotlinx.coroutines.flow.Flow

class UsageRecordRepository(private val usageRecordDao: UsageRecordDao) {

    fun observeByItem(itemType: String, itemId: Long): Flow<List<UsageRecordEntity>> =
        usageRecordDao.observeByItem(itemType, itemId)

    suspend fun add(record: UsageRecordEntity): Long = usageRecordDao.insert(record)

    suspend fun deleteByItem(itemType: String, itemId: Long) =
        usageRecordDao.deleteByItem(itemType, itemId)
}
