package com.fridgekeeper.model

import com.fridgekeeper.data.local.CheckinCycle
import com.fridgekeeper.data.local.CheckinRecordEntity
import com.fridgekeeper.data.local.CheckinTaskEntity
import com.fridgekeeper.data.local.CouponEntity
import java.util.Calendar
import kotlin.math.ceil

/** 优惠券到期状态：到期日当天仍算有效 */
data class CouponItemUiState(
    val id: Long,
    val source: String,
    val type: String,
    val threshold: Double,
    val amount: Double,
    val validFrom: Long?,
    val validUntil: Long,
    val scene: String,
    val isUsed: Boolean,
    val usedAt: Long?,
    val note: String,
    val remainingDays: Int,
    val isExpiringSoon: Boolean,
    val isExpired: Boolean
) {
    companion object {
        fun fromEntity(entity: CouponEntity, now: Long = System.currentTimeMillis()): CouponItemUiState {
            val remaining = remainingDays(entity.validUntil, now)
            return CouponItemUiState(
                id = entity.id,
                source = entity.source,
                type = entity.type,
                threshold = entity.threshold,
                amount = entity.amount,
                validFrom = entity.validFrom,
                validUntil = entity.validUntil,
                scene = entity.scene,
                isUsed = entity.isUsed,
                usedAt = entity.usedAt,
                note = entity.note,
                remainingDays = remaining,
                isExpiringSoon = remaining in 1..EXPIRING_SOON_DAYS,
                isExpired = remaining <= 0
            )
        }

        private const val DAY_MS = 24 * 60 * 60 * 1000L
        private const val EXPIRING_SOON_DAYS = 3

        /** 到期日按当天结束计算，向上取整 */
        fun remainingDays(validUntil: Long, now: Long): Int {
            val diff = validUntil + DAY_MS - now
            return ceil(diff.toDouble() / DAY_MS).toInt()
        }
    }
}

data class CheckinItemUiState(
    val id: Long,
    val platform: String,
    val name: String,
    val cycle: String,
    val lastCheckinAt: Long?,
    val remindHour: Int,
    val checkedInThisPeriod: Boolean,
    val totalCheckins: Int,
    val weeklyCheckins: Int,
    val weeklyCheckinDays: Int,
    val cycleLabel: String
) {
    companion object {
        fun fromEntity(
            entity: CheckinTaskEntity,
            records: List<CheckinRecordEntity>,
            now: Long = System.currentTimeMillis()
        ): CheckinItemUiState {
            val start = CheckinPeriod.startOf(entity.cycle, now)
            val end = CheckinPeriod.endOf(entity.cycle, now)
            val last = entity.lastCheckinAt
            val taskRecords = records.filter { it.taskId == entity.id }
            val weekStart = CheckinPeriod.startOf(CheckinCycle.WEEKLY, now)
            val weekEnd = CheckinPeriod.addDays(weekStart, 7)
            val weekRecords = taskRecords.filter { it.checkedAt >= weekStart && it.checkedAt < weekEnd }
            return CheckinItemUiState(
                id = entity.id,
                platform = entity.platform,
                name = entity.name,
                cycle = entity.cycle,
                lastCheckinAt = last,
                remindHour = entity.remindHour,
                checkedInThisPeriod = last != null && last >= start && last < end,
                totalCheckins = taskRecords.size,
                weeklyCheckins = weekRecords.size,
                weeklyCheckinDays = weekRecords
                    .map { CheckinPeriod.startOf(CheckinCycle.DAILY, it.checkedAt) }
                    .distinct()
                    .size,
                cycleLabel = CheckinCycle.label(entity.cycle)
            )
        }
    }
}

data class CheckinWeekDayUiState(
    val date: Long,
    val dayOfMonth: Int,
    val weekdayLabel: String,
    val isToday: Boolean,
    val checkinCount: Int
)

object CheckinPeriod {
    /** 当前周期起点（每日 0 点 / 本周一 0 点 / 本月 1 日 0 点） */
    fun startOf(cycle: String, now: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        when (cycle) {
            CheckinCycle.WEEKLY -> {
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val delta = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
                calendar.add(Calendar.DAY_OF_MONTH, -delta)
            }
            CheckinCycle.MONTHLY -> calendar.set(Calendar.DAY_OF_MONTH, 1)
        }
        return calendar.timeInMillis
    }

    fun endOf(cycle: String, now: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = startOf(cycle, now) }
        when (cycle) {
            CheckinCycle.WEEKLY -> calendar.add(Calendar.DAY_OF_MONTH, 7)
            CheckinCycle.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            else -> calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return calendar.timeInMillis
    }

    fun addDays(timestamp: Long, days: Int): Long = Calendar.getInstance().run {
        timeInMillis = timestamp
        add(Calendar.DAY_OF_MONTH, days)
        timeInMillis
    }

    fun dayOfMonth(timestamp: Long): Int = Calendar.getInstance().run {
        timeInMillis = timestamp
        get(Calendar.DAY_OF_MONTH)
    }
}

data class CouponSourceStat(
    val source: String,
    val totalCount: Int,
    val unusedCount: Int,
    val totalAmount: Double,
    val unusedAmount: Double
)

data class WelfareUiState(
    val coupons: List<CouponItemUiState> = emptyList(),
    val sourceStats: List<CouponSourceStat> = emptyList(),
    val checkins: List<CheckinItemUiState> = emptyList(),
    val unusedCount: Int = 0,
    val unusedValue: Double = 0.0,
    val expiringCount: Int = 0,
    val expiringValue: Double = 0.0,
    val usedValue: Double = 0.0,
    val bestCoupons: List<CouponItemUiState> = emptyList(),
    val todayPendingCheckins: Int = 0,
    val weekDays: List<CheckinWeekDayUiState> = emptyList(),
    val weekCheckinCount: Int = 0,
    val weekActiveDays: Int = 0,
    val isLoading: Boolean = true
)
