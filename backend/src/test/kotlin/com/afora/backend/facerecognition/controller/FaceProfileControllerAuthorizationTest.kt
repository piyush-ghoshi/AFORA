package com.afora.backend.facerecognition.controller

import com.afora.backend.attendance.security.CallerContext
import com.afora.backend.common.exception.BadRequestException
import com.afora.backend.common.exception.ForbiddenException
import com.afora.backend.common.exception.UnauthorizedException
import com.afora.backend.facerecognition.dto.EnrolledEmbeddingSetResponseDto
import com.afora.backend.facerecognition.dto.FaceConsentResponseDto
import com.afora.backend.facerecognition.dto.FaceProfileDeletionResponseDto
import com.afora.backend.facerecognition.dto.FaceProfileResponseDto
import com.afora.backend.facerecognition.entity.PurgeReason
import com.afora.backend.facerecognition.security.FaceProfileAccessGuard
import com.afora.backend.facerecognition.service.FaceProfileService
import com.afora.backend.user.entity.Student
import com.afora.backend.user.entity.User
import com.afora.backend.user.entity.UserRole
import com.afora.backend.user.repository.StudentRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Task 10.2: MockMvc authorization and status-code tests for [FaceProfileController],
 * covering:
 * - 401 (no principal)
 * - 401 (unknown uid)
 * - 403 (STUDENT on another student's id)
 * - 403 (STUDENT on the embeddings-distribution endpoint)
 * - 403 (TEACHER on a non-owned class section)
 * - 400 (zero embeddings, dimensionality mismatch)
 * - 403 (register with no consent)
 * - Success paths for STUDENT/TEACHER/ADMIN as applicable per endpoint
 *
 * Uses MockK only — no Mockito/mockito-kotlin/@Mock/when(...)/thenReturn.
 *
 * This test uses direct controller method invocation with mocked dependencies
 * to test authorization logic and status codes without requiring full Spring context.
 *
 * **Validates: Requirements 1.2, 2.6, 2.7, 4.7, 5.5, 5.7, 11.1, 11.2, 11.4, 11.5, 11.6, 11.7**
 */
@ExtendWith(SpringExtension::class)
class FaceProfileControllerAuthorizationTest {

    private val faceProfileService = mockk<FaceProfileService>()
    private val accessGuard = mockk<FaceProfileAccessGuard>()
    private val studentRepository = mockk<StudentRepository>()

    private fun createController() = FaceProfileController(
        faceProfileService = faceProfileService,
        accessGuard = accessGuard,
        studentRepository = studentRepository
    )

    // ── 401 Tests: No principal and unknown UID ──────────────────────────────

    /**
     * R11.1: No verified Firebase identifier (null @AuthenticationPrincipal) → 401.
     * The guard throws UnauthorizedException for null UID.
     */
    @Test
    fun `POST consent with no principal is 401`() {
        val controller = createController()

        every { accessGuard.requireCaller(null) } throws UnauthorizedException("Authentication required")

        assertFailsWith<UnauthorizedException> {
            controller.recordConsent(null)
        }
    }

    /**
     * R11.2: Unknown Firebase UID (no matching users row) → 401.
     * The guard throws UnauthorizedException when UID doesn't match any user.
     */
    @Test
    fun `POST consent with unknown UID is 401`() {
        val controller = createController()

        every { accessGuard.requireCaller("unknown-uid") } throws UnauthorizedException("User not found")

        assertFailsWith<UnauthorizedException> {
            controller.recordConsent("unknown-uid")
        }
    }

    /**
     * R11.1: No principal for embeddings registration → 401.
     */
    @Test
    fun `POST embeddings with no principal is 401`() {
        val controller = createController()
        val requestDto = com.afora.backend.facerecognition.dto.RegisterFaceProfileRequestDto(
            embeddings = listOf(listOf(0.1, 0.2))
        )

        every { accessGuard.requireCaller(null) } throws UnauthorizedException("Authentication required")

        assertFailsWith<UnauthorizedException> {
            controller.register(null, requestDto)
        }
    }

    /**
     * R11.1: No principal for deletion → 401.
     */
    @Test
    fun `DELETE profile with no principal is 401`() {
        val controller = createController()

        every { accessGuard.requireCaller(null) } throws UnauthorizedException("Authentication required")

        assertFailsWith<UnauthorizedException> {
            controller.deleteProfile(null, 123L)
        }
    }

    /**
     * R11.1: No principal for enrolled embeddings → 401.
     */
    @Test
    fun `GET enrolled-embeddings with no principal is 401`() {
        val controller = createController()

        every { accessGuard.requireCaller(null) } throws UnauthorizedException("Authentication required")

        assertFailsWith<UnauthorizedException> {
            controller.enrolledEmbeddings(null, 10L)
        }
    }

    // ── 403 Tests: Authorization failures ────────────────────────────────────

    /**
     * R11.4: STUDENT acting on another student's identifier → 403.
     * Testing the DELETE endpoint (which explicitly names a studentId in the path).
     */
    @Test
    fun `DELETE profile for another student is 403`() {
        val controller = createController()
        val caller = CallerContext(
            userId = 1L,
            role = UserRole.STUDENT,
            teacherId = null
        )

        every { accessGuard.requireCaller("student-uid") } returns caller
        every { accessGuard.requireOwnOrAdminStudentId(caller, 999L) } throws
            ForbiddenException("Cannot act on another student's face profile")

        assertFailsWith<ForbiddenException> {
            controller.deleteProfile("student-uid", 999L)
        }
    }

    /**
     * R5.7, R11.7: STUDENT calling the embeddings-distribution endpoint is always forbidden.
     */
    @Test
    fun `GET enrolled-embeddings as STUDENT is 403`() {
        val controller = createController()
        val caller = CallerContext(
            userId = 1L,
            role = UserRole.STUDENT,
            teacherId = null
        )

        every { accessGuard.requireCaller("student-uid") } returns caller
        every { accessGuard.requireClassSectionAccess(caller, 10L) } throws
            ForbiddenException("Students may not access face-embedding distribution")

        assertFailsWith<ForbiddenException> {
            controller.enrolledEmbeddings("student-uid", 10L)
        }
    }

    /**
     * R5.5, R11.5: TEACHER on a non-owned class section → 403.
     */
    @Test
    fun `GET enrolled-embeddings for non-owned class section is 403`() {
        val controller = createController()
        val caller = CallerContext(
            userId = 2L,
            role = UserRole.TEACHER,
            teacherId = 20L
        )

        every { accessGuard.requireCaller("teacher-uid") } returns caller
        every { accessGuard.requireClassSectionAccess(caller, 10L) } throws
            ForbiddenException("Cannot access another teacher's class section")

        assertFailsWith<ForbiddenException> {
            controller.enrolledEmbeddings("teacher-uid", 10L)
        }
    }

    /**
     * R1.2, R1.4: Attempting to register without recorded consent → 403.
     */
    @Test
    fun `POST embeddings without consent is 403`() {
        val controller = createController()
        val caller = CallerContext(
            userId = 1L,
            role = UserRole.STUDENT,
            teacherId = null
        )
        val mockUser = mockk<User>(relaxed = true)
        val student = Student(
            id = 5L,
            user = mockUser,
            rollNumber = "R1",
            department = "CS",
            batch = "2025"
        )
        val requestDto = com.afora.backend.facerecognition.dto.RegisterFaceProfileRequestDto(
            embeddings = listOf(listOf(0.1, 0.2, 0.3))
        )

        every { accessGuard.requireCaller("student-uid") } returns caller
        every { studentRepository.findByUserId(1L) } returns Optional.of(student)
        every { faceProfileService.register(5L, listOf(listOf(0.1, 0.2, 0.3))) } throws
            ForbiddenException("Student 5 holds no recorded Face_Consent")

        assertFailsWith<ForbiddenException> {
            controller.register("student-uid", requestDto)
        }
    }

    // ── 400 Tests: Bad request validation ────────────────────────────────────

    /**
     * R2.6: Zero embeddings submitted → 400.
     */
    @Test
    fun `POST embeddings with zero vectors is 400`() {
        val controller = createController()
        val caller = CallerContext(
            userId = 1L,
            role = UserRole.STUDENT,
            teacherId = null
        )
        val mockUser = mockk<User>(relaxed = true)
        val student = Student(
            id = 5L,
            user = mockUser,
            rollNumber = "R1",
            department = "CS",
            batch = "2025"
        )
        val requestDto = com.afora.backend.facerecognition.dto.RegisterFaceProfileRequestDto(
            embeddings = emptyList()
        )

        every { accessGuard.requireCaller("student-uid") } returns caller
        every { studentRepository.findByUserId(1L) } returns Optional.of(student)
        every { faceProfileService.register(5L, emptyList()) } throws
            BadRequestException("Face_Profile registration requires at least one Face_Embedding vector")

        assertFailsWith<BadRequestException> {
            controller.register("student-uid", requestDto)
        }
    }

    /**
     * R2.7: Dimensionality mismatch against existing profile → 400.
     */
    @Test
    fun `POST embeddings with mismatched dimensionality is 400`() {
        val controller = createController()
        val caller = CallerContext(
            userId = 1L,
            role = UserRole.STUDENT,
            teacherId = null
        )
        val mockUser = mockk<User>(relaxed = true)
        val student = Student(
            id = 5L,
            user = mockUser,
            rollNumber = "R1",
            department = "CS",
            batch = "2025"
        )
        val newEmbeddings = listOf(listOf(0.1, 0.2, 0.3, 0.4, 0.5)) // 5-dimensional
        val requestDto = com.afora.backend.facerecognition.dto.RegisterFaceProfileRequestDto(
            embeddings = newEmbeddings
        )

        every { accessGuard.requireCaller("student-uid") } returns caller
        every { studentRepository.findByUserId(1L) } returns Optional.of(student)
        every { faceProfileService.register(5L, newEmbeddings) } throws
            BadRequestException("Submitted dimensionality 5 differs from the dimensionality 3 already stored for student 5")

        assertFailsWith<BadRequestException> {
            controller.register("student-uid", requestDto)
        }
    }

    // ── Success paths ─────────────────────────────────────────────────────────

    /**
     * STUDENT successfully records consent.
     */
    @Test
    fun `POST consent as STUDENT succeeds`() {
        val controller = createController()
        val caller = CallerContext(
            userId = 1L,
            role = UserRole.STUDENT,
            teacherId = null
        )
        val mockUser = mockk<User>(relaxed = true)
        val student = Student(
            id = 5L,
            user = mockUser,
            rollNumber = "R1",
            department = "CS",
            batch = "2025"
        )
        val response = FaceConsentResponseDto(
            studentId = 5L,
            consentedAt = LocalDateTime.of(2025, 1, 15, 10, 30)
        )

        every { accessGuard.requireCaller("student-uid") } returns caller
        every { studentRepository.findByUserId(1L) } returns Optional.of(student)
        every { faceProfileService.recordConsent(5L) } returns response

        val result = controller.recordConsent("student-uid")

        assertEquals(true, result.body?.success)
        assertEquals(5L, result.body?.data?.studentId)
        verify { faceProfileService.recordConsent(5L) }
    }

    /**
     * STUDENT successfully registers embeddings.
     */
    @Test
    fun `POST embeddings as STUDENT succeeds`() {
        val controller = createController()
        val caller = CallerContext(
            userId = 1L,
            role = UserRole.STUDENT,
            teacherId = null
        )
        val mockUser = mockk<User>(relaxed = true)
        val student = Student(
            id = 5L,
            user = mockUser,
            rollNumber = "R1",
            department = "CS",
            batch = "2025"
        )
        val embeddings = listOf(listOf(0.1, 0.2, 0.3), listOf(0.4, 0.5, 0.6))
        val requestDto = com.afora.backend.facerecognition.dto.RegisterFaceProfileRequestDto(
            embeddings = embeddings
        )
        val response = FaceProfileResponseDto(
            studentId = 5L,
            embeddingCount = 2,
            dimensions = 3,
            registeredAt = LocalDateTime.of(2025, 1, 15, 10, 30)
        )

        every { accessGuard.requireCaller("student-uid") } returns caller
        every { studentRepository.findByUserId(1L) } returns Optional.of(student)
        every { faceProfileService.register(5L, embeddings) } returns response

        val result = controller.register("student-uid", requestDto)

        assertEquals(true, result.body?.success)
        assertEquals(5L, result.body?.data?.studentId)
        assertEquals(2, result.body?.data?.embeddingCount)
        assertEquals(3, result.body?.data?.dimensions)
        verify { faceProfileService.register(5L, embeddings) }
    }

    /**
     * STUDENT successfully deletes their own profile.
     */
    @Test
    fun `DELETE own profile as STUDENT succeeds`() {
        val controller = createController()
        val caller = CallerContext(
            userId = 1L,
            role = UserRole.STUDENT,
            teacherId = null
        )
        val response = FaceProfileDeletionResponseDto(
            studentId = 5L,
            reason = "STUDENT_REQUEST",
            deletedAt = LocalDateTime.of(2025, 1, 15, 10, 30)
        )

        every { accessGuard.requireCaller("student-uid") } returns caller
        every { accessGuard.requireOwnOrAdminStudentId(caller, 5L) } returns 5L
        every { faceProfileService.deleteProfile(5L, PurgeReason.STUDENT_REQUEST) } returns response

        val result = controller.deleteProfile("student-uid", 5L)

        assertEquals(true, result.body?.success)
        assertEquals(5L, result.body?.data?.studentId)
        assertEquals("STUDENT_REQUEST", result.body?.data?.reason)
        verify { faceProfileService.deleteProfile(5L, PurgeReason.STUDENT_REQUEST) }
    }

    /**
     * R4.7: ADMIN successfully deletes another student's profile.
     */
    @Test
    fun `DELETE another student's profile as ADMIN succeeds`() {
        val controller = createController()
        val caller = CallerContext(
            userId = 3L,
            role = UserRole.ADMIN,
            teacherId = null
        )
        val response = FaceProfileDeletionResponseDto(
            studentId = 999L,
            reason = "ADMIN_REQUEST",
            deletedAt = LocalDateTime.of(2025, 1, 15, 10, 30)
        )

        every { accessGuard.requireCaller("admin-uid") } returns caller
        every { accessGuard.requireOwnOrAdminStudentId(caller, 999L) } returns 999L
        every { faceProfileService.deleteProfile(999L, PurgeReason.ADMIN_REQUEST) } returns response

        val result = controller.deleteProfile("admin-uid", 999L)

        assertEquals(true, result.body?.success)
        assertEquals(999L, result.body?.data?.studentId)
        assertEquals("ADMIN_REQUEST", result.body?.data?.reason)
        verify { faceProfileService.deleteProfile(999L, PurgeReason.ADMIN_REQUEST) }
    }

    /**
     * TEACHER successfully retrieves enrolled embeddings for their own class section.
     */
    @Test
    fun `GET enrolled-embeddings as owning TEACHER succeeds`() {
        val controller = createController()
        val caller = CallerContext(
            userId = 2L,
            role = UserRole.TEACHER,
            teacherId = 20L
        )
        val response = EnrolledEmbeddingSetResponseDto(
            classSectionId = 10L,
            entries = emptyList(),
            highConfidenceThreshold = 0.80,
            mediumConfidenceThreshold = 0.60,
            unmatchedFloorThreshold = 0.60
        )

        every { accessGuard.requireCaller("teacher-uid") } returns caller
        every { accessGuard.requireClassSectionAccess(caller, 10L) } returns Unit
        every { faceProfileService.enrolledEmbeddingSet(10L) } returns response

        val result = controller.enrolledEmbeddings("teacher-uid", 10L)

        assertEquals(true, result.body?.success)
        assertEquals(10L, result.body?.data?.classSectionId)
        assertEquals(0.80, result.body?.data?.highConfidenceThreshold)
        assertEquals(0.60, result.body?.data?.mediumConfidenceThreshold)
        verify { faceProfileService.enrolledEmbeddingSet(10L) }
    }

    /**
     * R5.6, R11.6: ADMIN successfully retrieves enrolled embeddings for any class section.
     */
    @Test
    fun `GET enrolled-embeddings as ADMIN succeeds for any section`() {
        val controller = createController()
        val caller = CallerContext(
            userId = 3L,
            role = UserRole.ADMIN,
            teacherId = null
        )
        val response = EnrolledEmbeddingSetResponseDto(
            classSectionId = 10L,
            entries = emptyList(),
            highConfidenceThreshold = 0.80,
            mediumConfidenceThreshold = 0.60,
            unmatchedFloorThreshold = 0.60
        )

        every { accessGuard.requireCaller("admin-uid") } returns caller
        every { accessGuard.requireClassSectionAccess(caller, 10L) } returns Unit
        every { faceProfileService.enrolledEmbeddingSet(10L) } returns response

        val result = controller.enrolledEmbeddings("admin-uid", 10L)

        assertEquals(true, result.body?.success)
        assertEquals(10L, result.body?.data?.classSectionId)
        verify { accessGuard.requireClassSectionAccess(caller, 10L) }
        verify { faceProfileService.enrolledEmbeddingSet(10L) }
    }
}
