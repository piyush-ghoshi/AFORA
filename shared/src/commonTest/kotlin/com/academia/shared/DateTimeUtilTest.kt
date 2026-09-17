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
    fun `parseISO8601 should handle valid timestamp`() {
        val timestamp = "2024-08-25T10:30:00Z"
        val instant = DateTimeUtil.parseISO8601(timestamp)
        assertNotNull(instant)
    }

    @Test
    fun `formatISO8601 should format instant correctly`() {
        val instant = DateTimeUtil.parseISO8601("2024-08-25T10:30:00Z")
        val formatted = DateTimeUtil.formatISO8601(instant)
        assertNotNull(formatted)
        assertTrue(formatted.contains("2024"))
    }
}
