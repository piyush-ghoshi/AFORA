package com.afora.backend.attendance.controller

import com.afora.backend.attendance.dto.AmendAttendanceRequestDto
import com.afora.backend.attendance.dto.AmendmentResultResponse
import com.afora.backend.attendance.dto.AttendanceConfirmationResponse
import com.afora.backend.attendance.dto.AuditHistoryResponse
import com.afora.backend.attendance.dto.ConfirmAttendanceRequestDto
import com.afora.backend.attendance.dto.RosterResponse
import com.afora.backend.attendance.dto.SessionSummary
import com.afora.backend.attendance.dto.StudentAttendanceResponse
import com.afora.backend.attendance.security.AttendanceAccessGuard
import com.afora.backend.attendance.service.AttendanceService
import com.afora.backend.attendance.service.StudentAttendanceService
import com.afora.backend.common.dto.ApiResponse
import com.afora.backend.common.exception.ForbiddenException
import com.afora.backend.common.exception.UnauthorizedException
import com.afora.backend.user.entity.UserRole
import com.afora.backend.user.repository.StudentRepository
import com.afora.backend.user.repository.UserRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * The six attendance-marking endpoints. `server.servlet.context-path: /api`
 * means these resolve under `/api/attendance/...` (R10.9).
 *
 * Every method is the same three lines: resolve the caller, assert session
 * access, delegate to the [AttendanceService] facade. Every body is wrapped
 * in [ApiResponse.success] (R10.10). No method issues a repository query
 * beyond resolving the caller (R10.12).
 */
@RestController
@RequestMapping("/attendance")
class AttendanceController(
    private val attendanceService: AttendanceService,
    private val studentAttendanceService: StudentAttendanceService,
    private val accessGuard: AttendanceAccessGuard,
    private val userRepository: UserRepository,
    private val studentRepository: StudentRepository
) {

    @GetMapping("/sessions/{sessionId}/roster")
    fun roster(
        @AuthenticationPrincipal firebaseUid: String?,
        @PathVariable sessionId: Long
    ): ResponseEntity<ApiResponse<RosterResponse>> {
        val caller = accessGuard.requireCaller(firebaseUid)
        val session = accessGuard.requireSessionAccess(caller, sessionId)
        return ResponseEntity.ok(ApiResponse.success(attendanceService.roster(session)))
    }

    @PostMapping("/sessions/{sessionId}/activate")
    fun activate(
        @AuthenticationPrincipal firebaseUid: String?,
        @PathVariable sessionId: Long
    ): ResponseEntity<ApiResponse<SessionSummary>> {
        val caller = accessGuard.requireCaller(firebaseUid)
        val session = accessGuard.requireSessionAccess(caller, sessionId)
        return ResponseEntity.ok(ApiResponse.success(attendanceService.activate(session, caller)))
    }

    @PostMapping("/sessions/{sessionId}/cancel")
    fun cancel(
        @AuthenticationPrincipal firebaseUid: String?,
        @PathVariable sessionId: Long
    ): ResponseEntity<ApiResponse<SessionSummary>> {
        val caller = accessGuard.requireCaller(firebaseUid)
        val session = accessGuard.requireSessionAccess(caller, sessionId)
        return ResponseEntity.ok(ApiResponse.success(attendanceService.cancel(session, caller)))
    }

    @PostMapping("/sessions/{sessionId}/confirm")
    fun confirm(
        @AuthenticationPrincipal firebaseUid: String?,
        @PathVariable sessionId: Long,
        @RequestBody request: ConfirmAttendanceRequestDto
    ): ResponseEntity<ApiResponse<AttendanceConfirmationResponse>> {
        val caller = accessGuard.requireCaller(firebaseUid)
        accessGuard.requireSessionAccess(caller, sessionId)
        return ResponseEntity.ok(ApiResponse.success(attendanceService.confirm(sessionId, caller, request)))
    }

    @PostMapping("/sessions/{sessionId}/amend")
    fun amend(
        @AuthenticationPrincipal firebaseUid: String?,
        @PathVariable sessionId: Long,
        @RequestBody request: AmendAttendanceRequestDto
    ): ResponseEntity<ApiResponse<AmendmentResultResponse>> {
        val caller = accessGuard.requireCaller(firebaseUid)
        accessGuard.requireSessionAccess(caller, sessionId)
        return ResponseEntity.ok(ApiResponse.success(attendanceService.amend(sessionId, caller, request)))
    }

    @GetMapping("/sessions/{sessionId}/audit-history")
    fun auditHistory(
        @AuthenticationPrincipal firebaseUid: String?,
        @PathVariable sessionId: Long
    ): ResponseEntity<ApiResponse<AuditHistoryResponse>> {
        val caller = accessGuard.requireCaller(firebaseUid)
        accessGuard.requireSessionAccess(caller, sessionId)
        return ResponseEntity.ok(ApiResponse.success(attendanceService.auditHistory(sessionId)))
    }

    /**
     * GET /api/attendance/student/{studentId}
     *
     * Returns subject-wise and session-wise attendance for a student.
     * A student may only read their own data; teachers and admins may read any.
     */
    @GetMapping("/student/{studentId}")
    fun studentAttendance(
        @AuthenticationPrincipal firebaseUid: String?,
        @PathVariable studentId: Long,
        @RequestParam(required = false) semesterId: UUID?
    ): ResponseEntity<ApiResponse<StudentAttendanceResponse>> {
        val uid = firebaseUid ?: throw UnauthorizedException("Authentication required")
        val user = userRepository.findByFirebaseUid(uid)
            .orElseThrow { UnauthorizedException("User not found") }

        when (user.role) {
            UserRole.ADMIN, UserRole.TEACHER -> { /* may read any student */ }
            UserRole.STUDENT -> {
                val ownId = studentRepository.findByUserId(user.id!!)
                    .orElseThrow { ForbiddenException("No student profile for this account") }.id
                if (ownId != studentId) throw ForbiddenException("Cannot view another student's attendance")
            }
        }

        return ResponseEntity.ok(
            ApiResponse.success(studentAttendanceService.getStudentAttendance(studentId, semesterId))
        )
    }
}