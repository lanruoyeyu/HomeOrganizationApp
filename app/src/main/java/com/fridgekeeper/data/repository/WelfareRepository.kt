package com.fridgekeeper.data.repository

import com.fridgekeeper.data.local.CheckinDao
import com.fridgekeeper.data.local.CheckinRecordEntity
import com.fridgekeeper.data.local.CheckinTaskEntity
import com.fridgekeeper.data.local.CouponDao
import com.fridgekeeper.data.local.CouponEntity
import kotlinx.coroutines.flow.Flow

class WelfareRepository(
    private val couponDao: CouponDao,
    private val checkinDao: CheckinDao
) {

    // ---------- 优惠券 ----------

    fun observeCoupons(): Flow<List<CouponEntity>> = couponDao.observeAll()

    suspend fun getAllCoupons(): List<CouponEntity> = couponDao.getAll()

    suspend fun addCoupon(coupon: CouponEntity): Long = couponDao.insert(coupon)

    suspend fun updateCoupon(coupon: CouponEntity) = couponDao.update(coupon)

    suspend fun deleteCoupon(id: Long) = couponDao.deleteById(id)

    suspend fun getCoupon(id: Long): CouponEntity? = couponDao.getById(id)

    // ---------- 打卡 ----------

    fun observeTasks(): Flow<List<CheckinTaskEntity>> = checkinDao.observeTasks()

    fun observeAllRecords(): Flow<List<CheckinRecordEntity>> = checkinDao.observeAllRecords()

    suspend fun getAllTasks(): List<CheckinTaskEntity> = checkinDao.getAllTasks()

    suspend fun addTask(task: CheckinTaskEntity): Long = checkinDao.insertTask(task)

    suspend fun updateTask(task: CheckinTaskEntity) = checkinDao.updateTask(task)

    suspend fun deleteTask(id: Long) {
        checkinDao.deleteRecordsByTask(id)
        checkinDao.deleteTaskById(id)
    }

    suspend fun getTask(id: Long): CheckinTaskEntity? = checkinDao.getTaskById(id)

    suspend fun checkIn(taskId: Long, checkedAt: Long, note: String = "") {
        checkinDao.insertRecord(
            CheckinRecordEntity(taskId = taskId, checkedAt = checkedAt, note = note)
        )
        checkinDao.getTaskById(taskId)?.let { task ->
            checkinDao.updateTask(task.copy(lastCheckinAt = checkedAt, updatedAt = checkedAt))
        }
    }

    suspend fun cancelCheckIn(taskId: Long, periodStart: Long, periodEnd: Long, updatedAt: Long) {
        checkinDao.deleteRecordsInPeriod(taskId, periodStart, periodEnd)
        val latest = checkinDao.getLatestRecord(taskId)
        checkinDao.getTaskById(taskId)?.let { task ->
            checkinDao.updateTask(
                task.copy(lastCheckinAt = latest?.checkedAt, updatedAt = updatedAt)
            )
        }
    }
}
