package com.fridgekeeper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fridgekeeper.data.local.CheckinCycle
import com.fridgekeeper.data.local.CheckinTaskEntity
import com.fridgekeeper.data.local.CouponEntity
import com.fridgekeeper.data.repository.WelfareRepository
import com.fridgekeeper.model.CheckinItemUiState
import com.fridgekeeper.model.CheckinPeriod
import com.fridgekeeper.model.CheckinWeekDayUiState
import com.fridgekeeper.model.CouponItemUiState
import com.fridgekeeper.model.CouponSourceStat
import com.fridgekeeper.model.WelfareUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WelfareViewModel(
    private val repository: WelfareRepository
) : ViewModel() {

    /** 让剩余天数随时间自然刷新 */
    private val refreshTick = flow {
        while (true) {
            emit(Unit)
            delay(60 * 60 * 1000L)
        }
    }

    val uiState: StateFlow<WelfareUiState> = combine(
        repository.observeCoupons(),
        repository.observeTasks(),
        repository.observeAllRecords(),
        refreshTick
    ) { coupons, tasks, records, _ ->
        val now = System.currentTimeMillis()
        val couponItems = coupons.map { CouponItemUiState.fromEntity(it, now) }
        val checkinItems = tasks.map { CheckinItemUiState.fromEntity(it, records, now) }
        val weekStart = CheckinPeriod.startOf(CheckinCycle.WEEKLY, now)
        val todayStart = CheckinPeriod.startOf(CheckinCycle.DAILY, now)
        val weekdayLabels = listOf("一", "二", "三", "四", "五", "六", "日")
        val weekDays = weekdayLabels.mapIndexed { index, label ->
            val dayStart = CheckinPeriod.addDays(weekStart, index)
            val dayEnd = CheckinPeriod.addDays(dayStart, 1)
            CheckinWeekDayUiState(
                date = dayStart,
                dayOfMonth = CheckinPeriod.dayOfMonth(dayStart),
                weekdayLabel = label,
                isToday = dayStart == todayStart,
                checkinCount = records.count { it.checkedAt >= dayStart && it.checkedAt < dayEnd }
            )
        }

        val unused = couponItems.filter { !it.isUsed }
        val expiring = unused.filter { it.isExpiringSoon }
        val sourceStats: List<CouponSourceStat> = couponItems
            .groupBy { it.source }
            .map { (source, list) ->
                CouponSourceStat(
                    source = source,
                    totalCount = list.size,
                    unusedCount = list.count { !it.isUsed },
                    totalAmount = list.sumOf { it.amount },
                    unusedAmount = list.filter { !it.isUsed }.sumOf { it.amount }
                )
            }
            .sortedByDescending { it.unusedAmount }

        // 今日展示规则：
        // - 未在当前周期打卡（待打卡）→ 展示，排在前面
        // - 今天刚打卡（doneToday）→ 展示为「已打卡」，排在后面
        // - 本周/本月早些时候已打卡（非今天）→ 不再展示
        val visibleCheckins = checkinItems
            .filter { !it.checkedInThisPeriod || it.doneToday }
            .sortedBy { if (!it.checkedInThisPeriod) 0 else 1 }
        val visiblePendingCount = visibleCheckins.count { !it.checkedInThisPeriod }

        WelfareUiState(
            coupons = couponItems,
            sourceStats = sourceStats,
            checkins = checkinItems,
            visibleCheckins = visibleCheckins,
            visiblePendingCount = visiblePendingCount,
            unusedCount = unused.size,
            unusedValue = unused.sumOf { it.amount },
            expiringCount = expiring.size,
            expiringValue = expiring.sumOf { it.amount },
            usedValue = couponItems.filter { it.isUsed }.sumOf { it.amount },
            bestCoupons = unused.sortedByDescending { it.amount }.take(3),
            todayPendingCheckins = checkinItems.count { !it.checkedInThisPeriod },
            weekDays = weekDays,
            weekCheckinCount = weekDays.sumOf { it.checkinCount },
            weekActiveDays = weekDays.count { it.checkinCount > 0 },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WelfareUiState(isLoading = true)
    )

    // ---------- 优惠券 ----------

    fun addCoupon(
        source: String,
        type: String,
        threshold: Double,
        amount: Double,
        validFrom: Long?,
        validUntil: Long,
        scene: String,
        note: String
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.addCoupon(
                CouponEntity(
                    source = source,
                    type = type,
                    threshold = threshold,
                    amount = amount,
                    validFrom = validFrom,
                    validUntil = validUntil,
                    scene = scene,
                    note = note,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    fun updateCoupon(
        id: Long,
        source: String,
        type: String,
        threshold: Double,
        amount: Double,
        validFrom: Long?,
        validUntil: Long,
        scene: String,
        note: String
    ) {
        viewModelScope.launch {
            val existing = repository.getCoupon(id) ?: return@launch
            repository.updateCoupon(
                existing.copy(
                    source = source,
                    type = type,
                    threshold = threshold,
                    amount = amount,
                    validFrom = validFrom,
                    validUntil = validUntil,
                    scene = scene,
                    note = note,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteCoupon(id: Long) {
        viewModelScope.launch { repository.deleteCoupon(id) }
    }

    fun markCouponUsed(id: Long, used: Boolean = true) {
        viewModelScope.launch {
            val existing = repository.getCoupon(id) ?: return@launch
            val now = System.currentTimeMillis()
            repository.updateCoupon(
                existing.copy(
                    isUsed = used,
                    usedAt = if (used) now else null,
                    updatedAt = now
                )
            )
        }
    }

    // ---------- 打卡 ----------

    fun addTask(platform: String, name: String, cycle: String, remindHour: Int) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.addTask(
                CheckinTaskEntity(
                    platform = platform,
                    name = name,
                    cycle = if (cycle in CheckinCycle.ALL) cycle else CheckinCycle.DAILY,
                    remindHour = remindHour.coerceIn(0, 23),
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    fun updateTask(id: Long, platform: String, name: String, cycle: String, remindHour: Int) {
        viewModelScope.launch {
            val existing = repository.getTask(id) ?: return@launch
            repository.updateTask(
                existing.copy(
                    platform = platform,
                    name = name,
                    cycle = if (cycle in CheckinCycle.ALL) cycle else CheckinCycle.DAILY,
                    remindHour = remindHour.coerceIn(0, 23),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch { repository.deleteTask(id) }
    }

    fun checkIn(id: Long) {
        viewModelScope.launch {
            repository.checkIn(id, System.currentTimeMillis())
        }
    }

    fun toggleCheckIn(id: Long) {
        viewModelScope.launch {
            val task = repository.getTask(id) ?: return@launch
            val now = System.currentTimeMillis()
            val periodStart = CheckinPeriod.startOf(task.cycle, now)
            val periodEnd = CheckinPeriod.endOf(task.cycle, now)
            val checked = task.lastCheckinAt?.let { it >= periodStart && it < periodEnd } == true
            if (checked) {
                repository.cancelCheckIn(id, periodStart, periodEnd, now)
            } else {
                repository.checkIn(id, now)
            }
        }
    }
}
