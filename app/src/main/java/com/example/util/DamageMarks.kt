package com.example.util

import java.util.Locale

/** One mark on the damage diagram. Coordinates are 0..1 inside the phone outline. */
data class DamageMark(val back: Boolean, val x: Float, val y: Float, val type: Char)

object DamageMarks {
    const val SCRATCH = 'R'
    const val CRACK = 'C'
    const val DENT = 'G'

    val TYPES = listOf(SCRATCH, CRACK, DENT)

    /** Stored as "F,0.420,0.310,C;B,0.500,0.700,G" (F = front, B = back). */
    fun parse(raw: String): List<DamageMark> =
        raw.split(';').mapNotNull { part ->
            val f = part.split(',')
            if (f.size != 4) return@mapNotNull null
            val x = f[1].toFloatOrNull() ?: return@mapNotNull null
            val y = f[2].toFloatOrNull() ?: return@mapNotNull null
            val type = f[3].firstOrNull() ?: return@mapNotNull null
            DamageMark(back = f[0] == "B", x = x.coerceIn(0f, 1f), y = y.coerceIn(0f, 1f), type = type)
        }

    fun serialize(marks: List<DamageMark>): String =
        marks.joinToString(";") {
            String.format(Locale.US, "%s,%.3f,%.3f,%c", if (it.back) "B" else "F", it.x, it.y, it.type)
        }

    /** ARGB color of each damage type (same in the app and in the PDF). */
    fun colorFor(type: Char): Int = when (type) {
        CRACK -> 0xFFDC2626.toInt()
        DENT -> 0xFF2563EB.toInt()
        else -> 0xFFF59E0B.toInt()
    }
}
