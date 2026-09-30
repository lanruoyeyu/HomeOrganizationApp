package com.fridgekeeper.model

import java.util.Locale

object InventoryMath {

    fun dailyCost(purchasePrice: Double, fromTimestamp: Long, toTimestamp: Long): Double {
        val days = DateUtils.calculateDaysHeld(fromTimestamp, toTimestamp)
        return purchasePrice / days
    }

    fun toGrams(quantity: Double, unit: WeightUnit): Double =
        if (unit == WeightUnit.KG) quantity * 1000.0 else quantity

    fun fromGrams(grams: Double, unit: WeightUnit): Double =
        if (unit == WeightUnit.KG) grams / 1000.0 else grams

    fun formatQuantity(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString()
        else String.format(Locale.CHINA, "%.2f", value)
}
