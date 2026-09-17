package com.academia.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Temporary attendance candidate (before teacher confirmation).
 * NOT persisted to backend - used during review phase only.
 */
@Serializable
data class AttendanceCandidate(
    val studentId: Long,
    val source: CandidateSource,
    val confidence: Double? = null,  // Only for FACE_RECOGNITION
    val category: ConfidenceCategory? = null,  // Only for FACE_RECOGNITION
    val status: AttendanceStatus = AttendanceStatus.PRESENT,
    val timestamp: String,  // ISO 8601 datetime
    val metadata: Map<String, String>? = null  // Frame info, detection details
)

/**
 * Source of attendance candidate.
 */
@Serializable
enum class CandidateSource {
    FACE_RECOGNITION,
    MANUAL,
    MANUAL_CORRECTION  // Added during review
}

/**
 * Confidence categorization for face recognition results.
 * IMPORTANT: Thresholds are CONFIGURABLE, not hard-coded.
 */
@Serializable
enum class ConfidenceCategory {
    HIGH,        // Auto-accept candidate
    MEDIUM,      // Needs review
    LOW,         // Needs review
    VERY_LOW,    // Needs review or reject
    UNKNOWN      // No match found
}
