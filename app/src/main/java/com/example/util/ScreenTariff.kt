package com.example.util

import kotlin.math.ceil

/**
 * Shop pricing table for screens (internal use). The public base price depends on the supplier cost range.
 * Discounts (10/15/20/25 percent) are fixed prices from the table; 25 percent is the maximum allowed.
 * For screens that cost more than 900 the table has no fixed price: the customer pays supplier cost plus
 * a labor surcharge. Prices from the table already include labor.
 */
object ScreenTariff {

    val DISCOUNTS = listOf(0, 10, 15, 20, 25)

    private class Tier(
        val maxCost: Int,
        val base: Double,
        val d10: Double,
        val d15: Double,
        val d20: Double,
        val d25: Double
    )

    private val TIERS = listOf(
        Tier(199, 1000.0, 900.0, 850.0, 800.0, 750.0),
        Tier(350, 1100.0, 990.0, 935.0, 880.0, 825.0),
        Tier(500, 1250.0, 1125.0, 1062.0, 1000.0, 937.0),
        Tier(600, 1450.0, 1305.0, 1233.0, 1160.0, 1087.0),
        Tier(700, 1650.0, 1485.0, 1402.0, 1320.0, 1237.0),
        Tier(900, 1900.0, 1710.0, 1615.0, 1520.0, 1425.0)
    )

    private const val MIN_COST = 150

    fun isScreen(category: String): Boolean =
        category.equals("Pantalla", ignoreCase = true) || category.equals("Display", ignoreCase = true)

    /** Customer price for a screen, or null when the table does not cover that cost (below 150). */
    fun price(cost: Double, discountPercent: Int): Double? {
        val rounded = ceil(cost).toInt()
        if (rounded < MIN_COST) return null
        if (rounded > 900) {
            val surcharge = when {
                rounded <= 1499 -> 600.0
                rounded <= 1999 -> 800.0
                else -> 1000.0
            }
            return cost + surcharge
        }
        val tier = TIERS.first { rounded <= it.maxCost }
        return when (discountPercent) {
            10 -> tier.d10
            15 -> tier.d15
            20 -> tier.d20
            25 -> tier.d25
            else -> tier.base
        }
    }
}
