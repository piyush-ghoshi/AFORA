package com.afora.backend.dashboard.controller

import com.afora.backend.common.exception.ForbiddenException
import com.afora.backend.common.exception.UnauthorizedException
import com.afora.backend.dashboard.dto.StudentDashboardStatsResponse
import com.afora.backend.dashboard.dto.TeacherDashboardStatsResponse
import com.afora.backend.dashboard.service.DashboardService
import com.afora.backend.user.entity.Student
import com.afora.backend.user.entity.Teacher
import com.afora.backend.user.entity.User
import com.afora.backend.user.entity.UserRole
import com.afora.backend.user.repository.StudentRepository
import com.afora.backend.user.repository.TeacherRepository
import com.afora.backend.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Unit tests for DashboardController.
 *
 * Phase A8.4 added ownership authorization, which is the bulk of what these
 * tests cover: a caller must not be able to read somebody else's dashboard.
 */
class DashboardControllerTest {

    private lateinit var dashboardService: DashboardService
    private lateinit var userRepository: UserRepository
    private lateinit var studentRepository: StudentRepository
    private lateinit var teacherRepository: TeacherRepository
    private lateinit var controller: DashboardController

    private val studentUid = "uid_student"
    private val teacherUid = "uid_teacher"
    private val adminUid = "uid_admin"

    private val studentUserId = 1L
    private val teacherUserId = 2L
    private val adminUserId = 3L

    private val studentId = 11L
    private val teacherId = 22L

    private val studentStats = StudentDashboardStatsResponse(
        attendancePercentage = 86.54,
        presentCount = 45,
        totalCount = 52,
        upcomingClasses = emptyList(),
        pendingLeaveRequests = 2
    )
    private val teacherStats = TeacherDashboardStatsResponse(
        todaySchedule = emptyList(),
        pendingLeaveRequests = 5,
        pendingQueries = 0
    )

    private fun user(id: Long, uid: String, role: UserRole) = User(
        id = id,
        firebaseUid = uid,
        email = "$uid@afora.edu",
        firstName = "Test",
        lastName = "User",
        role = role
    )

    @BeforeEach
    fun setUp() {
        dashboardService = mockk()
        userRepository = mockk()
        studentRepository = mockk()
        teacherRepository = mockk()
        controller = DashboardController(
            dashboardService, userRepository, studentRepository, teacherRepository
        )

        val studentUser = user(studentUserId, studentUid, UserRole.STUDENT)
        val teacherUser = user(teacherUserId, teacherUid, UserRole.TEACHER)
        val adminUser = user(adminUserId, adminUid, UserRole.ADMIN)

        every { userRepository.findByFirebaseUid(studentUid) } returns Optional.of(studentUser)
        every { userRepository.findByFirebaseUid(teacherUid) } returns Optional.of(teacherUser)
        every { userRepository.findByFirebaseUid(adminUid) } returns Optional.of(adminUser)

        every { studentRepository.findByUserId(studentUserId) } returns Optional.of(
            Student(id = studentId, user = studentUser, rollNumber = "2024CS001", department = "CS", batch = "2024")
        )
        every { teacherRepository.findByUserId(teacherUserId) } returns Optional.of(
            Teacher(id = teacherId, user = teacherUser, employeeId = "EMP001", department = "CS")
        )

        every { dashboardService.getStudentDashboard(any(), any()) } returns studentStats
        every { dashboardService.getTeacherDashboard(any(), any()) } returns teacherStats
    }

    // -- authentication ------------------------------------------------------

    @Test
    fun `student endpoint rejects a missing token`() {
        assertThrows<UnauthorizedException> {
            controller.getStudentDashboard(null, studentId, null)
        }
    }

    @Test
    fun `teacher endpoint rejects a missing token`() {
        assertThrows<UnauthorizedException> {
            controller.getTeacherDashboard(null, teacherId, null)
        }
    }

    @Test
    fun `unknown firebase uid is rejected`() {
        every { userRepository.findByFirebaseUid("ghost") } returns Optional.empty()
        assertThrows<UnauthorizedException> {
            controller.getStudentDashboard("ghost", studentId, null)
        }
    }

    // -- student dashboard authorization -------------------------------------

    @Test
    fun `student can read their own dashboard`() {
        val response = controller.getStudentDashboard(studentUid, studentId, null)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body?.data)
        assertEquals(86.54, response.body!!.data!!.attendancePercentage)
    }

    @Test
    fun `student cannot read another students dashboard`() {
        val ex = assertThrows<ForbiddenException> {
            controller.getStudentDashboard(studentUid, 9999L, null)
        }
        assertEquals("Cannot view another student's dashboard", ex.message)
    }

    @Test
    fun `teacher cannot read an individual student dashboard`() {
        assertThrows<ForbiddenException> {
            controller.getStudentDashboard(teacherUid, studentId, null)
        }
    }

    @Test
    fun `admin can read any student dashboard`() {
        val response = controller.getStudentDashboard(adminUid, 9999L, null)
        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `student account without a student profile is refused`() {
        every { studentRepository.findByUserId(studentUserId) } returns Optional.empty()
        assertThrows<ForbiddenException> {
            controller.getStudentDashboard(studentUid, studentId, null)
        }
    }

    // -- teacher dashboard authorization -------------------------------------

    @Test
    fun `teacher can read their own dashboard`() {
        val response = controller.getTeacherDashboard(teacherUid, teacherId, null)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(5, response.body!!.data!!.pendingLeaveRequests)
    }

    @Test
    fun `teacher cannot read another teachers dashboard`() {
        val ex = assertThrows<ForbiddenException> {
            controller.getTeacherDashboard(teacherUid, 8888L, null)
        }
        assertEquals("Cannot view another teacher's dashboard", ex.message)
    }

    @Test
    fun `student cannot read a teacher dashboard`() {
        assertThrows<ForbiddenException> {
            controller.getTeacherDashboard(studentUid, teacherId, null)
        }
    }

    @Test
    fun `admin can read any teacher dashboard`() {
        val response = controller.getTeacherDashboard(adminUid, 8888L, null)
        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `teacher account without a teacher profile is refused`() {
        every { teacherRepository.findByUserId(teacherUserId) } returns Optional.empty()
        assertThrows<ForbiddenException> {
            controller.getTeacherDashboard(teacherUid, teacherId, null)
        }
    }

    // -- response envelope ---------------------------------------------------

    @Test
    fun `responses are wrapped in the ApiResponse envelope`() {
        val student = controller.getStudentDashboard(studentUid, studentId, null)
        assertEquals(true, student.body!!.success)
        assertEquals(null, student.body!!.error)

        val teacher = controller.getTeacherDashboard(teacherUid, teacherId, null)
        assertEquals(true, teacher.body!!.success)
        assertEquals(null, teacher.body!!.error)
    }
}