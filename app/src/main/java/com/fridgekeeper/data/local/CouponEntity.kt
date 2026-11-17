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
 * 优惠券：来源、类型、使用门槛、优惠金额、有效期与使用状态
 */
@Entity(tableName = "coupons")
data class CouponEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: String,
    val type: String,
    /** 使用条件门槛，0 表示无门槛 */
    val threshold: Double = 0.0,
    val amount: Double,
    val validFrom: Long? = null,
    val validUntil: Long,
    val scene: String = "",
    val isUsed: Boolean = false,
    val usedAt: Long? = null,
    val note: String = "",
    val createdAt: Long,
    val updatedAt: Long
)

@Dao
interface CouponDao {

    @Query("SELECT * FROM coupons ORDER BY isUsed ASC, validUntil ASC, id DESC")
    fun observeAll(): Flow<List<CouponEntity>>

    @Query("SELECT * FROM coupons ORDER BY isUsed ASC, validUntil ASC, id DESC")
    suspend fun getAll(): List<CouponEntity>

    @Query("SELECT * FROM coupons WHERE id = :id")
    suspend fun getById(id: Long): CouponEntity?

    @Insert
    suspend fun insert(entity: CouponEntity): Long

    @Update
    suspend fun update(entity: CouponEntity)

    @Delete
    suspend fun delete(entity: CouponEntity)

    @Query("DELETE FROM coupons WHERE id = :id")
    suspend fun deleteById(id: Long)
}
