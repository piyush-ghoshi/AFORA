package com.afora.android.ui.leave

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LeaveDatesTest {

    @Test
    fun `picker millis round-trip to the same ISO date`() {
        val millis = LeaveDates.millisFromIso("2026-03-17")!!
        assertEquals("2026-03-17", LeaveDates.isoFromMillis(millis))
    }

    @Test
    fun `inclusive days counts both ends`() {
        assertEquals(1, LeaveDates.inclusiveDays("2026-03-17", "2026-03-17"))
        assertEquals(2, LeaveDates.inclusiveDays("2026-03-17", "2026-03-18"))
        assertEquals(7, LeaveDates.inclusiveDays("2026-03-01", "2026-03-07"))
    }

    @Test
    fun `inclusive days spans a month boundary and a leap day`() {
        assertEquals(3, LeaveDates.inclusiveDays("2024-02-28", "2024-03-01"))
        assertEquals(2, LeaveDates.inclusiveDays("2026-01-31", "2026-02-01"))
    }

    @Test
    fun `reversed or invalid ranges give null`() {
        assertNull(LeaveDates.inclusiveDays("2026-03-18", "2026-03-17"))
        assertNull(LeaveDates.inclusiveDays("not-a-date", "2026-03-17"))
        assertNull(LeaveDates.millisFromIso("2026-13-45"))
    }

    @Test
    fun `days label is singular for one day`() {
        assertEquals("1 Academic Day", LeaveDates.daysLabel("2026-03-17", "2026-03-17"))
        assertEquals("2 Academic Days", LeaveDates.daysLabel("2026-03-17", "2026-03-18"))
        assertEquals("", LeaveDates.daysLabel("2026-03-18", "2026-03-17"))
    }

    @Test
    fun `submitted label keeps the time and drops seconds`() {
        val label = LeaveDates.submittedLabel("2026-09-15T10:05:33")
        assert(label.endsWith(", 10:05")) { label }
    }

    @Test
    fun `submitted label tolerates short input`() {
        assertEquals("bad", LeaveDates.submittedLabel("bad"))
    }
}