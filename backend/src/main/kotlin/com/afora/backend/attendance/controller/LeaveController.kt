package com.afora.backend.attendance.controller

import com.afora.backend.attendance.dto.LeaveClassOptionResponse
import com.afora.backend.attendance.dto.LeaveRequestResponse
import com.afora.backend.attendance.dto.ReviewLeaveRequestDto
import com.afora.backend.attendance.dto.SubmitLeaveRequestDto
import com.afora.backend.attendance.entity.LeaveStatus
import com.afora.backend.attendance.security.LeaveAccessGuard
import com.afora.backend.attendance.service.LeaveService
import com.afora.backend.common.dto.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * The Leave Requests feature's six endpoints. `server.servlet.context-path: /api`
 * means these resolve under `/api/leave/...`.
 *
 * Every method resolves the caller through [LeaveAccessGuard], asserts
 * ownership, then delegates to [LeaveService] -- the same shape as
 * [AttendanceController] and [com.afora.backend.attendance.security.AttendanceAccessGuard].
 */
@RestController
@RequestMapping("/leave")
class LeaveController(
    private val leaveService: LeaveService,
    private val accessGuard: LeaveAccessGuard
) {

    /** GET /api/leave/classes - the calling student's active classes, for the scope picker. */
    @GetMapping("/classes")
    fun listClassOptions(
        @AuthenticationPrincipal firebaseUid: String?
    ): ResponseEntity<ApiResponse<List<LeaveClassOptionResponse>>> {
        val user = accessGuard.requireCaller(firebaseUid)
        val studentId = accessGuard.requireOwnStudentId(user)
        return ResponseEntity.ok(ApiResponse.success(leaveService.listClassOptions(studentId)))
    }

    /** POST /api/leave/requests — a student submits a new leave request. */
    @PostMapping("/requests")
    fun submit(
        @AuthenticationPrincipal firebaseUid: String?,
        @RequestBody request: SubmitLeaveRequestDto
    ): ResponseEntity<ApiResponse<LeaveRequestResponse>> {
        val user = accessGuard.requireCaller(firebaseUid)
        val studentId = accessGuard.requireOwnStudentId(user)
        return ResponseEntity.ok(ApiResponse.success(leaveService.submit(studentId, request)))
    }

    /** GET /api/leave/requests/me — the authenticated student's own requests. */
    @GetMapping("/requests/me")
    fun listMine(
        @AuthenticationPrincipal firebaseUid: String?,
        @RequestParam(required = false) status: LeaveStatus?
    ): ResponseEntity<ApiResponse<List<LeaveRequestResponse>>> {
        val user = accessGuard.requireCaller(firebaseUid)
        val studentId = accessGuard.requireOwnStudentId(user)
        return ResponseEntity.ok(ApiResponse.success(leaveService.listForStudent(studentId, status)))
    }

    /** GET /api/leave/requests/pending/{teacherId} — a teacher's PENDING review queue. */
    @GetMapping("/requests/pending/{teacherId}")
    fun listPendingForTeacher(
        @AuthenticationPrincipal firebaseUid: String?,
        @PathVariable teacherId: Long
    ): ResponseEntity<ApiResponse<List<LeaveRequestResponse>>> {
        val user = accessGuard.requireCaller(firebaseUid)
        accessGuard.authorizeTeacherAccess(user, teacherId)
        return ResponseEntity.ok(ApiResponse.success(leaveService.listPendingForTeacher(teacherId)))
    }


    /** POST /api/leave/requests/{leaveRequestId}/approve */
    @PostMapping("/requests/{leaveRequestId}/approve")
    fun approve(
        @AuthenticationPrincipal firebaseUid: String?,
        @PathVariable leaveRequestId: Long,
        @RequestBody(required = false) request: ReviewLeaveRequestDto?
    ): ResponseEntity<ApiResponse<LeaveRequestResponse>> {
        val user = accessGuard.requireCaller(firebaseUid)
        val context = accessGuard.requireReviewAccess(user, leaveRequestId)
        return ResponseEntity.ok(
            ApiResponse.success(
                leaveService.approve(leaveRequestId, context.reviewingTeacher, request?.notes)
            )
        )
    }


    /** POST /api/leave/requests/{leaveRequestId}/reject */
    @PostMapping("/requests/{leaveRequestId}/reject")
    fun reject(
        @AuthenticationPrincipal firebaseUid: String?,
        @PathVariable leaveRequestId: Long,
        @RequestBody request: ReviewLeaveRequestDto
    ): ResponseEntity<ApiResponse<LeaveRequestResponse>> {
        val user = accessGuard.requireCaller(firebaseUid)
        val context = accessGuard.requireReviewAccess(user, leaveRequestId)
        return ResponseEntity.ok(
            ApiResponse.success(
                leaveService.reject(leaveRequestId, context.reviewingTeacher, request.notes)
            )
        )
    }


    /** POST /api/leave/requests/{leaveRequestId}/cancel — the requesting student withdraws it. */
    @PostMapping("/requests/{leaveRequestId}/cancel")
    fun cancel(
        @AuthenticationPrincipal firebaseUid: String?,
        @PathVariable leaveRequestId: Long
    ): ResponseEntity<ApiResponse<LeaveRequestResponse>> {
        val user = accessGuard.requireCaller(firebaseUid)
        accessGuard.requireCancelAccess(user, leaveRequestId)
        return ResponseEntity.ok(ApiResponse.success(leaveService.cancel(leaveRequestId)))
    }
}