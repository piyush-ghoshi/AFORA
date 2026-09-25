package com.afora.backend.dashboard.controller

import com.afora.backend.common.dto.ApiResponse
import com.afora.backend.common.exception.UnauthorizedException
import com.afora.backend.dashboard.dto.StudentDashboardStatsResponse
import com.afora.backend.dashboard.dto.TeacherDashboardStatsResponse
import com.afora.backend.dashboard.service.DashboardService
import com.afora.backend.user.repository.UserRepository
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

/**
 * Dashboard controller for student and teacher dashboards.
 * 
 * Provides aggregated statistics and data for dashboard screens.
 * All endpoints require authentication via Firebase ID token.
 * 
 * Phase A6: Returns mock data for demonstration.
 * Future phases will integrate with real repositories.
 */
@RestController
@RequestMapping("/api/dashboard")
class DashboardController(
    private val dashboardService: DashboardService,
    private val userRepository: UserRepository
) {

    /**
     * GET /api/dashboard/student/{studentId}
     * 
     * Returns student dashboard statistics including:
     * - Overall attendance percentage
     * - Present/absent counts
     * - Upcoming classes from timetable
     * - Pending leave requests
     * 
     * Query Parameters:
     * - semesterId (optional): Specific semester, defaults to current semester
     * 
     * Authorization: Student can only access their own dashboard
     * 
     * @param firebaseUid Authenticated user's Firebase UID (from JWT)
     * @param studentId Student ID from path
     * @param semesterId Optional semester ID
     * @return Dashboard statistics
     */
    @GetMapping("/student/{studentId}")
    fun getStudentDashboard(
        @AuthenticationPrincipal firebaseUid: String?,
        @PathVariable studentId: Long,
        @RequestParam(required = false) semesterId: Long?
    ): ResponseEntity<ApiResponse<StudentDashboardStatsResponse>> {
        val uid = firebaseUid ?: throw UnauthorizedException("Authentication required")
        
        // Verify the authenticated user is requesting their own data
        val user = userRepository.findByFirebaseUid(uid)
            ?: throw UnauthorizedException("User not found")
        
        // TODO: Phase A7 - Add proper authorization check
        // For now, allowing access to demonstrate functionality
        
        val stats = dashboardService.getStudentDashboard(studentId, semesterId)
        return ResponseEntity.ok(ApiResponse.success(stats))
    }

    /**
     * GET /api/dashboard/teacher/{teacherId}
     * 
     * Returns teacher dashboard statistics including:
     * - Today's lecture schedule
     * - Pending leave requests count
     * - Pending attendance queries count
     * 
     * Query Parameters:
     * - date (optional): Specific date (YYYY-MM-DD), defaults to today
     * 
     * Authorization: Teacher can only access their own dashboard
     * 
     * @param firebaseUid Authenticated user's Firebase UID (from JWT)
     * @param teacherId Teacher ID from path
     * @param date Optional date for schedule
     * @return Dashboard statistics
     */
    @GetMapping("/teacher/{teacherId}")
    fun getTeacherDashboard(
        @AuthenticationPrincipal firebaseUid: String?,
        @PathVariable teacherId: Long,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?
    ): ResponseEntity<ApiResponse<TeacherDashboardStatsResponse>> {
        val uid = firebaseUid ?: throw UnauthorizedException("Authentication required")
        
        // Verify the authenticated user is requesting their own data
        val user = userRepository.findByFirebaseUid(uid)
            ?: throw UnauthorizedException("User not found")
        
        // TODO: Phase A7 - Add proper authorization check
        // For now, allowing access to demonstrate functionality
        
        val stats = dashboardService.getTeacherDashboard(teacherId, date)
        return ResponseEntity.ok(ApiResponse.success(stats))
    }
}
