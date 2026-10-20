package com.afora.android.ui.components

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/**
 * Attendance threshold arithmetic for the student dashboard.
 *
 * [THRESHOLD_PERCENT] is the institution's minimum attendance. It is a single
 * constant here because the backend has no per-institution setting yet.
 */
object AttendanceMath {

    const val THRESHOLD_PERCENT: Double = 75.0

    private const val EPSILON = 1e-9

    /**
     * How many more classes the student could miss (with every later class
     * held) and still finish at or above [threshold]. Zero when already below.
     *
     * Solves present / (total + n) >= threshold / 100 for the largest n.
     */
    fun absencesRemaining(present: Int, total: Int, threshold: Double = THRESHOLD_PERCENT): Int {
        if (total <= 0 || threshold <= 0.0) return 0
        val allowed = floor(present * 100.0 / threshold - total + EPSILON)
        return max(0, allowed.toInt())
    }

    /**
     * How many consecutive classes the student must attend to reach
     * [threshold]. Zero when already at or above it.
     *
     * Solves (present + n) / (total + n) >= threshold / 100 for the smallest n.
     */
    fun classesNeeded(present: Int, total: Int, threshold: Double = THRESHOLD_PERCENT): Int {
        if (total <= 0 || threshold >= 100.0) return 0
        val needed = ceil((threshold * total - 100.0 * present) / (100.0 - threshold) - EPSILON)
        return max(0, needed.toInt())
    }

    fun isGoodStanding(percentage: Double, threshold: Double = THRESHOLD_PERCENT): Boolean =
        percentage >= threshold
}