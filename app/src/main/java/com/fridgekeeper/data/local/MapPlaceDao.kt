package com.fridgekeeper.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MapPlaceDao {

    @Query("SELECT * FROM map_places ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<MapPlaceEntity>>

    @Query("SELECT COUNT(*) FROM map_places WHERE name = :name")
    suspend fun countByName(name: String): Int

    @Insert
    suspend fun insert(entity: MapPlaceEntity): Long

    @Query("DELETE FROM map_places WHERE id = :id")
    suspend fun delete(id: Long)
}
