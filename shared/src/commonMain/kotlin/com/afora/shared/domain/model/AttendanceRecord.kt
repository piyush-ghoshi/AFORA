package com.afora.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Final attendance record (persisted after teacher confirmation).
 */
@Serializable
data class AttendanceRecord(
    val id: Long,
    val lectureSessionId: Long,
    val studentId: Long,
    val status: AttendanceStatus,
    val method: AttendanceMethod,
    val confidenceScore: Double? = null,  // Only for camera-based recognition
    val markedByTeacherId: Long,
    val markedAt: String,  // ISO 8601 datetime
    val remarks: String? = null,
    val isCorrected: Boolean = false
)

/**
 * Attendance status for a student in a lecture.
 */
@Serializable
enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    ON_LEAVE,
    LATE
}
