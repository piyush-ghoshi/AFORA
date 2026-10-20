package com.afora.shared.data.api

import kotlinx.serialization.Serializable

/**
 * Shared KMP wire types for the Face_Recognition feature, field for field and
 * type for type with `backend/.../facerecognition/dto/FaceProfileDto.kt`
 * (`.kiro/specs/face-recognition/design.md` → "Wire Contract"), following the
 * [AttendanceWire]/[LeaveWire] precedent: one `@Serializable` type per
 * payload, no domain mirror, no `@SerialName` anywhere. Timestamps are plain
 * `String` (`ISO_LOCAL_DATE_TIME`, no zone) on this side and `LocalDateTime`
 * on the backend side, matching the existing convention exactly.
 */

// ── Consent ──────────────────────────────────────────────────────────────────

/**
 * Response for POST /api/face-profiles/consent. No request body (R1.3).
 */
@Serializable
data class FaceConsentResponse(
    val studentId: Long,
    val consentedAt: String
)

// ── Register ─────────────────────────────────────────────────────────────────

/**
 * Request body for POST /api/face-profiles/embeddings.
 *
 * No `studentId` field — derived entirely from the authenticated caller
 * (Settled Decision D).
 */
@Serializable
data class RegisterFaceProfileRequest(
    val embeddings: List<List<Double>> = emptyList()
)

/**
 * Response for POST /api/face-profiles/embeddings.
 *
 * Never includes the vectors themselves (R3.4) — counts only.
 */
@Serializable
data class FaceProfileResponse(
    val studentId: Long,
    val embeddingCount: Int,
    val dimensions: Int,
    val registeredAt: String
)

// ── Delete ───────────────────────────────────────────────────────────────────

/**
 * Response for DELETE /api/face-profiles/{studentId}. No request body.
 */
@Serializable
data class FaceProfileDeletionResponse(
    val studentId: Long,
    val reason: String,
    val deletedAt: String
)

// ── Enrolled embedding set ───────────────────────────────────────────────────

/**
 * Response for GET /api/face-profiles/class-sections/{classSectionId}/enrolled-embeddings.
 *
 * `entries` is `[]`, never null, when no ACTIVE-enrolled student in the
 * class section holds a registered Face_Profile (R5.8). The three threshold
 * fields let the device's own HIGH/MEDIUM/unmatched classification and the
 * backend's recomputation read from one source of truth (Settled Decision B);
 * the device never sends threshold values to the server, it only receives
 * them.
 */
@Serializable
data class EnrolledEmbeddingSetResponse(
    val classSectionId: Long,
    val entries: List<EnrolledEmbeddingEntry> = emptyList(),
    val highConfidenceThreshold: Double,
    val mediumConfidenceThreshold: Double,
    val unmatchedFloorThreshold: Double
)

/**
 * One enrolled student's embeddings within an `EnrolledEmbeddingSetResponse`
 * (R5.9), so the On_Device_Matcher can report which student a match belongs to.
 */
@Serializable
data class EnrolledEmbeddingEntry(
    val studentId: Long,
    val rollNumber: String,
    val fullName: String,
    val embeddings: List<List<Double>> = emptyList()
)
