package com.fridgekeeper.data.repository

import com.fridgekeeper.data.local.LedgerDao
import com.fridgekeeper.data.local.LedgerEntryEntity
import kotlinx.coroutines.flow.Flow

class LedgerRepository(private val ledgerDao: LedgerDao) {

    fun observeAll(): Flow<List<LedgerEntryEntity>> = ledgerDao.observeAll()

    suspend fun getById(id: Long): LedgerEntryEntity? = ledgerDao.getById(id)

    suspend fun add(entry: LedgerEntryEntity): Long = ledgerDao.insert(entry)

    suspend fun update(entry: LedgerEntryEntity) = ledgerDao.update(entry)

    suspend fun delete(id: Long) = ledgerDao.delete(id)
}
