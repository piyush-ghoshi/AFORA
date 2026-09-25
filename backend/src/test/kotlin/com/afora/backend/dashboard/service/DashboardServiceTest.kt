package com.afora.backend.dashboard.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for DashboardService.
 * 
 * Phase A6: Tests mock data implementation.
 * Phase A7: Will add tests for real repository integration.
 */
class DashboardServiceTest {

    private val dashboardService = DashboardService()

    // ── Student Dashboard Tests ───────────────────────────────────────────────

    @Test
    fun `getStudentDashboard returns valid statistics`() {
        val studentId = 1L
        val result = dashboardService.getStudentDashboard(studentId, null)

        assertNotNull(result)
        assertTrue(result.attendancePercentage >= 0.0 && result.attendancePercentage <= 100.0)
        assertTrue(result.presentCount >= 0)
        assertTrue(result.totalCount >= 0)
        assertTrue(result.presentCount <= result.totalCount)
        assertTrue(result.pendingLeaveRequests >= 0)
    }

    @Test
    fun `getStudentDashboard returns upcoming classes`() {
        val studentId = 1L
        val result = dashboardService.getStudentDashboard(studentId, null)

        assertNotNull(result.upcomingClasses)
        assertTrue(result.upcomingClasses.isNotEmpty())
        
        // Verify first class has required fields
        val firstClass = result.upcomingClasses.first()
        assertNotNull(firstClass.subjectCode)
        assertNotNull(firstClass.subjectName)
        assertNotNull(firstClass.teacherName)
        assertNotNull(firstClass.startTime)
        assertNotNull(firstClass.endTime)
        assertTrue(firstClass.dayOfWeek in 1..7) // Monday=1, Sunday=7
    }

    @Test
    fun `getStudentDashboard with semesterId returns valid data`() {
        val studentId = 1L
        val semesterId = 5L
        
        assertDoesNotThrow {
            val result = dashboardService.getStudentDashboard(studentId, semesterId)
            assertNotNull(result)
        }
    }

    @Test
    fun `getStudentDashboard calculates correct attendance percentage`() {
        val studentId = 1L
        val result = dashboardService.getStudentDashboard(studentId, null)

        // Mock data: 45 present out of 52 total = 86.54%
        val expectedPercentage = (result.presentCount.toDouble() / result.totalCount) * 100
        assertTrue(result.attendancePercentage >= expectedPercentage - 1.0) // Allow 1% tolerance
    }

    // ── Teacher Dashboard Tests ───────────────────────────────────────────────

    @Test
    fun `getTeacherDashboard returns valid statistics`() {
        val teacherId = 20L
        val result = dashboardService.getTeacherDashboard(teacherId, null)

        assertNotNull(result)
        assertTrue(result.pendingLeaveRequests >= 0)
        assertTrue(result.pendingQueries >= 0)
        assertNotNull(result.todaySchedule)
    }

    @Test
    fun `getTeacherDashboard returns today's schedule`() {
        val teacherId = 20L
        val result = dashboardService.getTeacherDashboard(teacherId, null)

        assertNotNull(result.todaySchedule)
        assertTrue(result.todaySchedule.isNotEmpty())
        
        // Verify first session has required fields
        val firstSession = result.todaySchedule.first()
        assertNotNull(firstSession.date)
        assertNotNull(firstSession.startTime)
        assertNotNull(firstSession.endTime)
        assertNotNull(firstSession.status)
        assertTrue(firstSession.teacherId == teacherId)
    }

    @Test
    fun `getTeacherDashboard with specific date returns data for that date`() {
        val teacherId = 20L
        val specificDate = LocalDate.of(2026, 9, 17)
        val result = dashboardService.getTeacherDashboard(teacherId, specificDate)

        assertNotNull(result)
        assertNotNull(result.todaySchedule)
        
        // All sessions should be for the specified date
        result.todaySchedule.forEach { session ->
            assertEquals(specificDate, session.date)
        }
    }

    @Test
    fun `getTeacherDashboard schedule is ordered by time`() {
        val teacherId = 20L
        val result = dashboardService.getTeacherDashboard(teacherId, null)

        val schedule = result.todaySchedule
        assertTrue(schedule.isNotEmpty())
        
        // Verify sessions are in chronological order
        for (i in 0 until schedule.size - 1) {
            val current = schedule[i].startTime
            val next = schedule[i + 1].startTime
            assertTrue(current <= next, "Schedule should be ordered by start time")
        }
    }

    @Test
    fun `getTeacherDashboard sessions have valid student counts`() {
        val teacherId = 20L
        val result = dashboardService.getTeacherDashboard(teacherId, null)

        result.todaySchedule.forEach { session ->
            assertTrue(session.presentCount >= 0)
            assertTrue(session.absentCount >= 0)
            assertTrue(session.onLeaveCount >= 0)
            
            // For SCHEDULED sessions, counts should be 0
            if (session.status == "SCHEDULED") {
                assertEquals(0, session.presentCount)
                assertEquals(0, session.absentCount)
                assertEquals(0, session.onLeaveCount)
            }
        }
    }

    @Test
    fun `getTeacherDashboard handles null date parameter`() {
        val teacherId = 20L
        
        assertDoesNotThrow {
            val result = dashboardService.getTeacherDashboard(teacherId, null)
            assertNotNull(result)
        }
    }

    // ── Data Consistency Tests ────────────────────────────────────────────────

    @Test
    fun `multiple calls return consistent data`() {
        val studentId = 1L
        val result1 = dashboardService.getStudentDashboard(studentId, null)
        val result2 = dashboardService.getStudentDashboard(studentId, null)

        assertEquals(result1.attendancePercentage, result2.attendancePercentage)
        assertEquals(result1.presentCount, result2.presentCount)
        assertEquals(result1.totalCount, result2.totalCount)
    }
}
