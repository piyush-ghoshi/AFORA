package com.afora.backend.facerecognition.dto

import java.time.LocalDateTime

/**
 * Backend DTOs for the Face_Profile_Service / Face_Profile_Controller wire
 * contract (`.kiro/specs/face-recognition/design.md` → "Wire Contract"),
 * field for field with the shared `@Serializable` types in
 * `shared/.../data/api/FaceProfileWire.kt`, following the same backend/shared
 * timestamp asymmetry already established by `AttendanceDto.kt`/`AttendanceWire.kt`
 * and `LeaveDto.kt`/`LeaveWire.kt`: `LocalDateTime` here, plain `String` there.
 */

// ── Consent ──────────────────────────────────────────────────────────────────

/**
 * Response for POST /api/face-profiles/consent. No request body (R1.3).
 */
data class FaceConsentResponseDto(
    val studentId: Long,
    val consentedAt: LocalDateTime
)

// ── Register ─────────────────────────────────────────────────────────────────

/**
 * Request body for POST /api/face-profiles/embeddings.
 *
 * No `studentId` field — derived entirely from the authenticated caller
 * (Settled Decision D). `embeddings` is expected non-empty; zero vectors is
 * rejected by `FaceProfileService` with `BadRequestException` (R2.6).
 */
data class RegisterFaceProfileRequestDto(
    val embeddings: List<List<Double>> = emptyList()
)

/**
 * Response for POST /api/face-profiles/embeddings.
 *
 * Never includes the vectors themselves (R3.4) — counts only.
 */
data class FaceProfileResponseDto(
    val studentId: Long,
    val embeddingCount: Int,
    val dimensions: Int,
    val registeredAt: LocalDateTime
)

// ── Delete ───────────────────────────────────────────────────────────────────

/**
 * Response for DELETE /api/face-profiles/{studentId}. No request body.
 */
data class FaceProfileDeletionResponseDto(
    val studentId: Long,
    val reason: String,
    val deletedAt: LocalDateTime
)

// ── Enrolled embedding set ───────────────────────────────────────────────────

/**
 * Response for GET /api/face-profiles/class-sections/{classSectionId}/enrolled-embeddings.
 *
 * `entries` is `[]`, never null, when no ACTIVE-enrolled student in the
 * class section holds a registered Face_Profile (R5.8). The three threshold
 * fields are plain wire values sourced from `FaceMatchingThresholds` at the
 * service layer — this DTO carries no logic of its own (Settled Decision B).
 */
data class EnrolledEmbeddingSetResponseDto(
    val classSectionId: Long,
    val entries: List<EnrolledEmbeddingEntryDto> = emptyList(),
    val highConfidenceThreshold: Double,
    val mediumConfidenceThreshold: Double,
    val unmatchedFloorThreshold: Double
)

/**
 * One enrolled student's embeddings within an `EnrolledEmbeddingSetResponseDto`
 * (R5.9), so the On_Device_Matcher can report which student a match belongs to.
 */
data class EnrolledEmbeddingEntryDto(
    val studentId: Long,
    val rollNumber: String,
    val fullName: String,
    val embeddings: List<List<Double>> = emptyList()
)
