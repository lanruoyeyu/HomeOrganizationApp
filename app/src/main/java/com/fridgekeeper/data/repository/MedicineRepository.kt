package com.fridgekeeper.data.repository

import com.fridgekeeper.data.local.MedicineDao
import com.fridgekeeper.data.local.MedicineEntity
import kotlinx.coroutines.flow.Flow

class MedicineRepository(private val medicineDao: MedicineDao) {

    fun observeAll(): Flow<List<MedicineEntity>> = medicineDao.observeAll()

    suspend fun add(medicine: MedicineEntity): Long = medicineDao.insert(medicine)

    suspend fun getById(id: Long): MedicineEntity? = medicineDao.getById(id)

    suspend fun update(medicine: MedicineEntity) = medicineDao.update(medicine)

    suspend fun delete(id: Long) = medicineDao.delete(id)
}
