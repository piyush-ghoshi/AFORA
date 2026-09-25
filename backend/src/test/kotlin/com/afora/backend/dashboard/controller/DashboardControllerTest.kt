package com.afora.backend.dashboard.controller

import com.afora.backend.dashboard.dto.StudentDashboardStatsResponse
import com.afora.backend.dashboard.dto.TeacherDashboardStatsResponse
import com.afora.backend.dashboard.service.DashboardService
import com.afora.backend.user.entity.User
import com.afora.backend.user.entity.UserRole
import com.afora.backend.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Unit tests for DashboardController.
 * 
 * Tests API endpoints, authentication, and response structure.
 */
class DashboardControllerTest {

    private lateinit var dashboardService: DashboardService
    private lateinit var userRepository: UserRepository
    private lateinit var controller: DashboardController

    private val mockFirebaseUid = "test_firebase_uid"
    private val mockStudentId = 1L
    private val mockTeacherId = 20L

    @BeforeEach
    fun setUp() {
        dashboardService = mock()
        userRepository = mock()
        controller = DashboardController(dashboardService, userRepository)

        // Setup mock user
        val mockUser = User(
            firebaseUid = mockFirebaseUid,
            email = "test@afora.edu",
            firstName = "Test",
            lastName = "User",
            role = UserRole.STUDENT
        ).apply { id = 1L }

        whenever(userRepository.findByFirebaseUid(mockFirebaseUid)).thenReturn(mockUser)
    }

    // ── Student Dashboard Tests ───────────────────────────────────────────────

    @Test
    fun `getStudentDashboard returns OK with valid data`() {
        // Given
        val mockStats = StudentDashboardStatsResponse(
            attendancePercentage = 85.5,
            presentCount = 45,
            totalCount = 52,
            upcomingClasses = emptyList(),
            pendingLeaveRequests = 2
        )
        whenever(dashboardService.getStudentDashboard(mockStudentId, null))
            .thenReturn(mockStats)

        // When
        val response = controller.getStudentDashboard(mockFirebaseUid, mockStudentId, null)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(true, response.body!!.success)
        assertNotNull(response.body!!.data)
        assertEquals(85.5, response.body!!.data!!.attendancePercentage)
    }

    @Test
    fun `getStudentDashboard with semesterId passes parameter to service`() {
        // Given
        val semesterId = 5L
        val mockStats = StudentDashboardStatsResponse(
            attendancePercentage = 90.0,
            presentCount = 45,
            totalCount = 50,
            upcomingClasses = emptyList(),
            pendingLeaveRequests = 0
        )
        whenever(dashboardService.getStudentDashboard(mockStudentId, semesterId))
            .thenReturn(mockStats)

        // When
        val response = controller.getStudentDashboard(mockFirebaseUid, mockStudentId, semesterId)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body?.data)
        assertEquals(90.0, response.body!!.data!!.attendancePercentage)
    }

    @Test
    fun `getStudentDashboard without firebaseUid throws UnauthorizedException`() {
        // When/Then
        try {
            controller.getStudentDashboard(null, mockStudentId, null)
            throw AssertionError("Expected UnauthorizedException")
        } catch (e: Exception) {
            assertEquals("UnauthorizedException", e::class.simpleName)
        }
    }

    @Test
    fun `getStudentDashboard with unknown firebaseUid throws UnauthorizedException`() {
        // Given
        val unknownUid = "unknown_uid"
        whenever(userRepository.findByFirebaseUid(unknownUid)).thenReturn(null)

        // When/Then
        try {
            controller.getStudentDashboard(unknownUid, mockStudentId, null)
            throw AssertionError("Expected UnauthorizedException")
        } catch (e: Exception) {
            assertEquals("UnauthorizedException", e::class.simpleName)
        }
    }

    // ── Teacher Dashboard Tests ───────────────────────────────────────────────

    @Test
    fun `getTeacherDashboard returns OK with valid data`() {
        // Given
        val mockStats = TeacherDashboardStatsResponse(
            todaySchedule = emptyList(),
            pendingLeaveRequests = 5,
            pendingQueries = 3
        )
        whenever(dashboardService.getTeacherDashboard(mockTeacherId, null))
            .thenReturn(mockStats)

        // When
        val response = controller.getTeacherDashboard(mockFirebaseUid, mockTeacherId, null)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(true, response.body!!.success)
        assertNotNull(response.body!!.data)
        assertEquals(5, response.body!!.data!!.pendingLeaveRequests)
        assertEquals(3, response.body!!.data!!.pendingQueries)
    }

    @Test
    fun `getTeacherDashboard with date passes parameter to service`() {
        // Given
        val specificDate = LocalDate.of(2026, 9, 17)
        val mockStats = TeacherDashboardStatsResponse(
            todaySchedule = emptyList(),
            pendingLeaveRequests = 2,
            pendingQueries = 1
        )
        whenever(dashboardService.getTeacherDashboard(mockTeacherId, specificDate))
            .thenReturn(mockStats)

        // When
        val response = controller.getTeacherDashboard(mockFirebaseUid, mockTeacherId, specificDate)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body?.data)
    }

    @Test
    fun `getTeacherDashboard without firebaseUid throws UnauthorizedException`() {
        // When/Then
        try {
            controller.getTeacherDashboard(null, mockTeacherId, null)
            throw AssertionError("Expected UnauthorizedException")
        } catch (e: Exception) {
            assertEquals("UnauthorizedException", e::class.simpleName)
        }
    }

    @Test
    fun `getTeacherDashboard with unknown firebaseUid throws UnauthorizedException`() {
        // Given
        val unknownUid = "unknown_uid"
        whenever(userRepository.findByFirebaseUid(unknownUid)).thenReturn(null)

        // When/Then
        try {
            controller.getTeacherDashboard(unknownUid, mockTeacherId, null)
            throw AssertionError("Expected UnauthorizedException")
        } catch (e: Exception) {
            assertEquals("UnauthorizedException", e::class.simpleName)
        }
    }

    // ── Response Structure Tests ──────────────────────────────────────────────

    @Test
    fun `student dashboard response has ApiResponse wrapper`() {
        // Given
        val mockStats = StudentDashboardStatsResponse(
            attendancePercentage = 75.0,
            presentCount = 30,
            totalCount = 40,
            upcomingClasses = emptyList(),
            pendingLeaveRequests = 1
        )
        whenever(dashboardService.getStudentDashboard(any(), any())).thenReturn(mockStats)

        // When
        val response = controller.getStudentDashboard(mockFirebaseUid, mockStudentId, null)

        // Then
        assertNotNull(response.body)
        assertEquals(true, response.body!!.success)
        assertNotNull(response.body!!.data)
        assertEquals(null, response.body!!.error)
    }

    @Test
    fun `teacher dashboard response has ApiResponse wrapper`() {
        // Given
        val mockStats = TeacherDashboardStatsResponse(
            todaySchedule = emptyList(),
            pendingLeaveRequests = 0,
            pendingQueries = 0
        )
        whenever(dashboardService.getTeacherDashboard(any(), any())).thenReturn(mockStats)

        // When
        val response = controller.getTeacherDashboard(mockFirebaseUid, mockTeacherId, null)

        // Then
        assertNotNull(response.body)
        assertEquals(true, response.body!!.success)
        assertNotNull(response.body!!.data)
        assertEquals(null, response.body!!.error)
    }
}
