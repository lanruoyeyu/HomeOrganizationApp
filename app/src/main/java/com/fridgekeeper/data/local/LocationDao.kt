package com.fridgekeeper.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Query("SELECT * FROM locations ORDER BY room ASC, spot ASC")
    fun observeAll(): Flow<List<LocationEntity>>

    @Insert
    suspend fun insert(entity: LocationEntity): Long

    @Update
    suspend fun update(entity: LocationEntity)

    @Delete
    suspend fun delete(entity: LocationEntity)
}
