package com.fridgekeeper.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SubcategoryDao {

    @Query("SELECT * FROM subcategories ORDER BY id ASC")
    fun observeAll(): Flow<List<SubcategoryEntity>>

    @Query("SELECT * FROM subcategories WHERE categoryId = :categoryId ORDER BY id ASC")
    fun observeByCategory(categoryId: Long): Flow<List<SubcategoryEntity>>

    @Query("SELECT COUNT(*) FROM subcategories WHERE categoryId = :categoryId AND name = :name")
    suspend fun countByName(categoryId: Long, name: String): Int

    @Insert
    suspend fun insert(entity: SubcategoryEntity): Long
}
