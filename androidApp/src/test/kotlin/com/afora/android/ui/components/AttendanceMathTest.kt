package com.afora.android.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceMathTest {

    @Test
    fun `absences remaining counts how many more misses stay at 75 percent`() {
        // 48/(57+7) = 0.75 exactly, 48/(57+8) < 0.75
        assertEquals(7, AttendanceMath.absencesRemaining(present = 48, total = 57))
    }

    @Test
    fun `absences remaining is zero exactly on the threshold`() {
        assertEquals(0, AttendanceMath.absencesRemaining(present = 30, total = 40))
    }

    @Test
    fun `absences remaining is zero below the threshold`() {
        assertEquals(0, AttendanceMath.absencesRemaining(present = 45, total = 64))
    }

    @Test
    fun `classes needed reaches the threshold exactly`() {
        // (45+12)/(64+12) = 0.75
        assertEquals(12, AttendanceMath.classesNeeded(present = 45, total = 64))
    }

    @Test
    fun `classes needed is zero at or above the threshold`() {
        assertEquals(0, AttendanceMath.classesNeeded(present = 48, total = 57))
        assertEquals(0, AttendanceMath.classesNeeded(present = 30, total = 40))
    }

    @Test
    fun `result of classesNeeded really reaches the threshold and one fewer does not`() {
        for (total in 1..80) {
            for (present in 0..total) {
                val n = AttendanceMath.classesNeeded(present, total)
                val after = (present + n) * 100.0 / (total + n)
                assertTrue("present=$present total=$total n=$n", after >= 75.0 - 1e-9)
                if (n > 0) {
                    val before = (present + n - 1) * 100.0 / (total + n - 1)
                    assertTrue("present=$present total=$total n=$n", before < 75.0)
                }
            }
        }
    }

    @Test
    fun `result of absencesRemaining stays above the threshold and one more does not`() {
        for (total in 1..80) {
            for (present in 0..total) {
                val a = AttendanceMath.absencesRemaining(present, total)
                if (present * 100.0 / total >= 75.0) {
                    assertTrue(present * 100.0 / (total + a) >= 75.0 - 1e-9)
                    assertTrue(present * 100.0 / (total + a + 1) < 75.0)
                } else {
                    assertEquals(0, a)
                }
            }
        }
    }

    @Test
    fun `no classes held gives zero for both`() {
        assertEquals(0, AttendanceMath.absencesRemaining(0, 0))
        assertEquals(0, AttendanceMath.classesNeeded(0, 0))
    }

    @Test
    fun `good standing flips at the threshold`() {
        assertTrue(AttendanceMath.isGoodStanding(75.0))
        assertFalse(AttendanceMath.isGoodStanding(74.99))
    }

    @Test
    fun `initials use up to two words`() {
        assertEquals("AM", initialsOf("Alex Morgan"))
        assertEquals("D", initialsOf("Dana"))
        assertEquals("?", initialsOf("   "))
    }
}