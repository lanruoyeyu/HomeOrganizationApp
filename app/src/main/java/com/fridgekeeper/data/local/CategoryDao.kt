package com.fridgekeeper.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY CASE WHEN isBuiltin = 1 THEN 0 ELSE 1 END ASC, id ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE kind = :kind LIMIT 1")
    suspend fun getByKind(kind: String): CategoryEntity?

    @Query("SELECT COUNT(*) FROM categories WHERE name = :name")
    suspend fun countByName(name: String): Int

    @Insert
    suspend fun insert(entity: CategoryEntity): Long
}
