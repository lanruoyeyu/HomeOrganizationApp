package com.fridgekeeper.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageRecordDao {

    @Insert
    suspend fun insert(record: UsageRecordEntity): Long

    @Query(
        "SELECT * FROM usage_records WHERE itemType = :itemType AND itemId = :itemId " +
            "ORDER BY usedAt DESC, id DESC"
    )
    fun observeByItem(itemType: String, itemId: Long): Flow<List<UsageRecordEntity>>

    @Query("DELETE FROM usage_records WHERE itemType = :itemType AND itemId = :itemId")
    suspend fun deleteByItem(itemType: String, itemId: Long)
}
