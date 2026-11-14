package com.fridgekeeper.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DigitalItemDao {

    @Query("SELECT * FROM digital_items ORDER BY CASE WHEN sellDate IS NULL THEN 0 ELSE 1 END ASC, purchaseDate DESC")
    fun observeAllItems(): Flow<List<DigitalItemEntity>>

    @Insert
    suspend fun insert(item: DigitalItemEntity): Long

    @Query("SELECT * FROM digital_items WHERE id = :id")
    suspend fun getById(id: Long): DigitalItemEntity?

    @Update
    suspend fun update(item: DigitalItemEntity)

    @Query("DELETE FROM digital_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE digital_items SET sellPrice = :sellPrice, sellDate = :sellDate, updatedAt = :timestamp WHERE id = :id")
    suspend fun markSold(id: Long, sellPrice: Double, sellDate: Long, timestamp: Long)
}
