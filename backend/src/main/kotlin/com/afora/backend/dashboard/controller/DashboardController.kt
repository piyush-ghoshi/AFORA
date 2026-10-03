package com.afora.backend.dashboard.controller

import com.afora.backend.common.dto.ApiResponse
import com.afora.backend.common.exception.ForbiddenException
import com.afora.backend.common.exception.UnauthorizedException
import com.afora.backend.dashboard.dto.StudentDashboardStatsResponse
import com.afora.backend.dashboard.dto.TeacherDashboardStatsResponse
import com.afora.backend.dashboard.service.DashboardService
import com.afora.backend.user.entity.User
import com.afora.backend.user.entity.UserRole
import com.afora.backend.user.repository.StudentRepository
import com.afora.backend.user.repository.TeacherRepository
import com.afora.backend.user.repository.UserRepository
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.UUID

/**
 * Dashboard endpoints for students and teachers.
 *
 * All endpoints require a verified Firebase ID token. Phase A8.4 enforces
 * ownership: a student may only read their own dashboard, a teacher only
 * their own. Admins may read any.
 */
@RestController
@RequestMapping("/dashboard")
class DashboardController(
    private val dashboardService: DashboardService,
    private val userRepository: UserRepository,
    private val studentRepository: StudentRepository,
    private val teacherRepository: TeacherRepository
) {

    /**
     * GET /api/dashboard/student/{studentId}
     *
     * @param semesterId optional semester UUID; defaults to the current semester.
     */
    @GetMapping("/student/{studentId}")
    fun getStudentDashboard(
        @AuthenticationPrincipal firebaseUid: String?,
        @PathVariable studentId: Long,
        @RequestParam(required = false) semesterId: UUID?
    ): ResponseEntity<ApiResponse<StudentDashboardStatsResponse>> {
        val user = requireUser(firebaseUid)
        authorizeStudentAccess(user, studentId)

        val stats = dashboardService.getStudentDashboard(studentId, semesterId)
        return ResponseEntity.ok(ApiResponse.success(stats))
    }

    /**
     * GET /api/dashboard/teacher/{teacherId}
     *
     * @param date optional ISO date; defaults to today.
     */
    @GetMapping("/teacher/{teacherId}")
    fun getTeacherDashboard(
        @AuthenticationPrincipal firebaseUid: String?,
        @PathVariable teacherId: Long,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?
    ): ResponseEntity<ApiResponse<TeacherDashboardStatsResponse>> {
        val user = requireUser(firebaseUid)
        authorizeTeacherAccess(user, teacherId)

        val stats = dashboardService.getTeacherDashboard(teacherId, date)
        return ResponseEntity.ok(ApiResponse.success(stats))
    }

    // -- authorization ------------------------------------------------------

    /** Resolve the caller, rejecting absent tokens and unknown users. */
    private fun requireUser(firebaseUid: String?): User {
        val uid = firebaseUid ?: throw UnauthorizedException("Authentication required")
        return userRepository.findByFirebaseUid(uid)
            .orElseThrow { UnauthorizedException("User not found") }
    }

    /**
     * A student may only read their own dashboard. Admins are unrestricted.
     * Teachers are refused: reading an individual student's dashboard is not
     * part of the teacher flow.
     */
    private fun authorizeStudentAccess(user: User, studentId: Long) {
        if (user.role == UserRole.ADMIN) return
        if (user.role != UserRole.STUDENT) {
            throw ForbiddenException("Only the student or an admin may view this dashboard")
        }
        val ownId = studentRepository.findByUserId(user.id!!)
            .orElseThrow { ForbiddenException("No student profile for this account") }
            .id
        if (ownId != studentId) {
            throw ForbiddenException("Cannot view another student's dashboard")
        }
    }

    /** A teacher may only read their own dashboard. Admins are unrestricted. */
    private fun authorizeTeacherAccess(user: User, teacherId: Long) {
        if (user.role == UserRole.ADMIN) return
        if (user.role != UserRole.TEACHER) {
            throw ForbiddenException("Only the teacher or an admin may view this dashboard")
        }
        val ownId = teacherRepository.findByUserId(user.id!!)
            .orElseThrow { ForbiddenException("No teacher profile for this account") }
            .id
        if (ownId != teacherId) {
            throw ForbiddenException("Cannot view another teacher's dashboard")
        }
    }
}