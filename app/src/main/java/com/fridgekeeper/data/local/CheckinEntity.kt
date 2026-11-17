package com.fridgekeeper.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 打卡习惯 / 会员福利：按每日、每周、每月周期打卡
 */
@Entity(tableName = "checkin_tasks")
data class CheckinTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val platform: String,
    val name: String,
    /** DAILY / WEEKLY / MONTHLY */
    val cycle: String,
    val lastCheckinAt: Long? = null,
    /** 提醒时间（0-23 点） */
    val remindHour: Int = 9,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "checkin_records")
data class CheckinRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val checkedAt: Long,
    val note: String = ""
)

object CheckinCycle {
    const val DAILY = "DAILY"
    const val WEEKLY = "WEEKLY"
    const val MONTHLY = "MONTHLY"

    val ALL = listOf(DAILY, WEEKLY, MONTHLY)

    fun label(cycle: String): String = when (cycle) {
        DAILY -> "每日"
        WEEKLY -> "每周"
        MONTHLY -> "每月"
        else -> cycle
    }
}

@Dao
interface CheckinDao {

    @Query("SELECT * FROM checkin_tasks ORDER BY id DESC")
    fun observeTasks(): Flow<List<CheckinTaskEntity>>

    @Query("SELECT * FROM checkin_tasks ORDER BY id DESC")
    suspend fun getAllTasks(): List<CheckinTaskEntity>

    @Query("SELECT * FROM checkin_tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): CheckinTaskEntity?

    @Insert
    suspend fun insertTask(entity: CheckinTaskEntity): Long

    @Update
    suspend fun updateTask(entity: CheckinTaskEntity)

    @Delete
    suspend fun deleteTask(entity: CheckinTaskEntity)

    @Query("DELETE FROM checkin_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("SELECT * FROM checkin_records WHERE taskId = :taskId ORDER BY checkedAt DESC")
    fun observeRecords(taskId: Long): Flow<List<CheckinRecordEntity>>

    @Query("SELECT * FROM checkin_records ORDER BY checkedAt DESC")
    fun observeAllRecords(): Flow<List<CheckinRecordEntity>>

    @Insert
    suspend fun insertRecord(entity: CheckinRecordEntity)

    @Query(
        "DELETE FROM checkin_records WHERE taskId = :taskId " +
            "AND checkedAt >= :periodStart AND checkedAt < :periodEnd"
    )
    suspend fun deleteRecordsInPeriod(taskId: Long, periodStart: Long, periodEnd: Long)

    @Query("SELECT * FROM checkin_records WHERE taskId = :taskId ORDER BY checkedAt DESC LIMIT 1")
    suspend fun getLatestRecord(taskId: Long): CheckinRecordEntity?

    @Query("DELETE FROM checkin_records WHERE taskId = :taskId")
    suspend fun deleteRecordsByTask(taskId: Long)
}
