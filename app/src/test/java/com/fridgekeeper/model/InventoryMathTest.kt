package com.fridgekeeper.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryMathTest {

    private val day = 24 * 60 * 60 * 1000L

    @Test
    fun remainingDays_expiredIsRed() {
        val now = 10_000L * day
        val remaining = DateUtils.calculateRemainingDays(now - day, now)
        assertTrue(DateUtils.isExpired(remaining))
        assertEquals(-1, remaining)
    }

    @Test
    fun remainingDays_sameDayIsExpired() {
        val now = 10_000L * day
        val remaining = DateUtils.calculateRemainingDays(now, now)
        assertTrue(DateUtils.isExpired(remaining))
        assertFalse(DateUtils.isWarning(remaining))
    }

    @Test
    fun dailyCost_sameDayUsesOneDay() {
        val cost = InventoryMath.dailyCost(100.0, 1_000L, 1_000L)
        assertEquals(100.0, cost, 0.0001)
    }

    @Test
    fun dailyCost_dividesByHeldDays() {
        val from = 0L
        val to = 10 * day
        val cost = InventoryMath.dailyCost(100.0, from, to)
        assertEquals(10.0, cost, 0.0001)
    }

    @Test
    fun weight_gToKgAndBack() {
        assertEquals(1500.0, InventoryMath.toGrams(1.5, WeightUnit.KG), 0.0001)
        assertEquals(500.0, InventoryMath.toGrams(500.0, WeightUnit.G), 0.0001)
        assertEquals(1.5, InventoryMath.fromGrams(1500.0, WeightUnit.KG), 0.0001)
    }
}
