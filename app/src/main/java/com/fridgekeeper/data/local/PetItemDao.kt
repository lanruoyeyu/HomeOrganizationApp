package com.fridgekeeper.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PetItemDao {

    @Query("SELECT * FROM pet_items ORDER BY purchaseDate DESC")
    fun observeAll(): Flow<List<PetItemEntity>>

    @Insert
    suspend fun insert(item: PetItemEntity): Long

    @Query("SELECT * FROM pet_items WHERE id = :id")
    suspend fun getById(id: Long): PetItemEntity?

    @Update
    suspend fun update(item: PetItemEntity)

    @Query("DELETE FROM pet_items WHERE id = :id")
    suspend fun delete(id: Long)
}
