package com.afora.backend.facerecognition.controller

import com.afora.backend.attendance.security.CallerContext
import com.afora.backend.common.dto.ApiResponse
import com.afora.backend.common.exception.ForbiddenException
import com.afora.backend.facerecognition.dto.EnrolledEmbeddingSetResponseDto
import com.afora.backend.facerecognition.dto.FaceConsentResponseDto
import com.afora.backend.facerecognition.dto.FaceProfileDeletionResponseDto
import com.afora.backend.facerecognition.dto.FaceProfileResponseDto
import com.afora.backend.facerecognition.dto.RegisterFaceProfileRequestDto
import com.afora.backend.facerecognition.entity.PurgeReason
import com.afora.backend.facerecognition.security.FaceProfileAccessGuard
import com.afora.backend.facerecognition.service.FaceProfileService
import com.afora.backend.user.entity.UserRole
import com.afora.backend.user.repository.StudentRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The four face-profile endpoints (`.kiro/specs/face-recognition/design.md` ->
 * "Components and Interfaces" -> `Face_Profile_Controller`). `server.servlet.context-path:
 * /api` means these resolve under `/api/face-profiles/...`.
 *
 * Every method is the same three-line shape as [com.afora.backend.attendance.controller.AttendanceController]:
 * resolve the caller through [FaceProfileAccessGuard], resolve/compare the identifier the
 * guard requires for that operation, delegate to [FaceProfileService]. Every body is
 * wrapped in [ApiResponse.success] (R11.7).
 *
 * Settled Decision D: consent and registration never read a studentId from the request
 * body or path -- both are resolved entirely from the authenticated caller's own student
 * profile via [requireStudentCaller]. `DELETE /{studentId}` is the one operation naming a
 * student in the path, specifically so an ADMIN can target a student other than itself
 * (R4.7), via [FaceProfileAccessGuard.requireOwnOrAdminStudentId].
 */
@RestController
@RequestMapping("/face-profiles")
class FaceProfileController(
    private val faceProfileService: FaceProfileService,
    private val accessGuard: FaceProfileAccessGuard,
    private val studentRepository: StudentRepository
) {

    /**
     * POST /face-profiles/consent. No request body (R1.3). Only a STUDENT caller holds a
     * student profile to consent for; TEACHER/ADMIN are rejected by [requireStudentCaller].
     */
    @PostMapping("/consent")
    fun recordConsent(
        @AuthenticationPrincipal firebaseUid: String?
    ): ResponseEntity<ApiResponse<FaceConsentResponseDto>> {
        val caller = accessGuard.requireCaller(firebaseUid)
        val studentId = requireStudentCaller(caller)
        return ResponseEntity.ok(ApiResponse.success(faceProfileService.recordConsent(studentId)))
    }

    /**
     * POST /face-profiles/embeddings. [RegisterFaceProfileRequestDto] carries no studentId
     * field (R2.8, Settled Decision D) -- the studentId is always the authenticated
     * caller's own.
     */
    @PostMapping("/embeddings")
    fun register(
        @AuthenticationPrincipal firebaseUid: String?,
        @RequestBody request: RegisterFaceProfileRequestDto
    ): ResponseEntity<ApiResponse<FaceProfileResponseDto>> {
        val caller = accessGuard.requireCaller(firebaseUid)
        val studentId = requireStudentCaller(caller)
        return ResponseEntity.ok(ApiResponse.success(faceProfileService.register(studentId, request.embeddings)))
    }

    /**
     * DELETE /face-profiles/{studentId}. The one operation naming a student in the path:
     * a STUDENT may only name their own identifier, an ADMIN may name any (R4.2, R4.6,
     * R4.7), enforced by [FaceProfileAccessGuard.requireOwnOrAdminStudentId].
     */
    @DeleteMapping("/{studentId}")
    fun deleteProfile(
        @AuthenticationPrincipal firebaseUid: String?,
        @PathVariable studentId: Long
    ): ResponseEntity<ApiResponse<FaceProfileDeletionResponseDto>> {
        val caller = accessGuard.requireCaller(firebaseUid)
        val resolvedStudentId = accessGuard.requireOwnOrAdminStudentId(caller, studentId)
        val reason = if (caller.role == UserRole.ADMIN) PurgeReason.ADMIN_REQUEST else PurgeReason.STUDENT_REQUEST
        return ResponseEntity.ok(
            ApiResponse.success(faceProfileService.deleteProfile(resolvedStudentId, reason))
        )
    }

    /**
     * GET /face-profiles/class-sections/{classSectionId}/enrolled-embeddings. TEACHER must
     * own the Class_Section, ADMIN is unrestricted, STUDENT is always forbidden (R5.4-R5.7,
     * R11.5-R11.7), enforced by [FaceProfileAccessGuard.requireClassSectionAccess].
     */
    @GetMapping("/class-sections/{classSectionId}/enrolled-embeddings")
    fun enrolledEmbeddings(
        @AuthenticationPrincipal firebaseUid: String?,
        @PathVariable classSectionId: Long
    ): ResponseEntity<ApiResponse<EnrolledEmbeddingSetResponseDto>> {
        val caller = accessGuard.requireCaller(firebaseUid)
        accessGuard.requireClassSectionAccess(caller, classSectionId)
        return ResponseEntity.ok(ApiResponse.success(faceProfileService.enrolledEmbeddingSet(classSectionId)))
    }

    /**
     * R1.1-R1.4, R2.1, R2.8, R11.3, R11.4: resolves [caller]'s own student identifier for
     * the two operations -- consent and registration -- that take no studentId from the
     * request at all (Settled Decision D). A TEACHER or ADMIN caller holds no student
     * profile and is rejected with [ForbiddenException], matching
     * [FaceProfileAccessGuard.requireOwnOrAdminStudentId]'s own "no student profile"
     * rejection for the same underlying condition.
     */
    private fun requireStudentCaller(caller: CallerContext): Long {
        if (caller.role != UserRole.STUDENT) {
            throw ForbiddenException("Only a student may perform this operation on their own Face_Profile")
        }
        return studentRepository.findByUserId(caller.userId)
            .orElseThrow { ForbiddenException("No student profile for this account") }
            .id ?: throw ForbiddenException("No student profile for this account")
    }
}
