package com.academia.shared

import com.academia.shared.util.DateTimeUtil
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Foundation test for DateTimeUtil.
 * Phase A1: Smoke test to verify datetime utilities work.
 */
class DateTimeUtilTest {

    @Test
    fun `now should return current timestamp`() {
        val now = DateTimeUtil.now()
        assertNotNull(now)
        assertTrue(now.isNotEmpty())
    }

    @Test
    fun `parseDateTime should handle valid timestamp`() {
        val timestamp = "2024-08-25T10:30:00Z"
        val instant = DateTimeUtil.parseDateTime(timestamp)
        assertNotNull(instant)
    }

    @Test
    fun `today should return current date`() {
        val today = DateTimeUtil.today()
        assertNotNull(today)
        assertTrue(today.isNotEmpty())
        assertTrue(today.contains("-")) // Should contain date separators
    }
}
