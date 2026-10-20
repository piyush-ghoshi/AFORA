package com.afora.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Final attendance record (persisted after teacher confirmation).
 *
 * `markedByTeacherId` is nullable: an ADMIN-performed confirmation records
 * the acting ADMIN in the Attendance_Audit_Entry instead, leaving this field
 * null (R11.6, R5.17). `method` is nullable to match the nullable
 * `attendance_records.method` column (R11.7). `isCorrected` is supplied by
 * the backend, derived from the presence of at least one Attendance_Audit_Entry
 * referencing this record whose previous and new status differ (R11.9) — the
 * client does not compute it.
 */
@Serializable
data class AttendanceRecord(
    val id: Long,
    val lectureSessionId: Long,
    val studentId: Long,
    val status: AttendanceStatus,
    val method: AttendanceMethod? = null,
    val confidenceScore: Double? = null,  // Only for camera-based recognition
    val markedByTeacherId: Long? = null,
    val markedAt: String,  // ISO 8601 datetime
    val notes: String? = null,
    val isCorrected: Boolean = false
)

/**
 * Attendance status for a student in a lecture.
 *
 * Order and membership are normative and must match the backend
 * `AttendanceStatus` enum value for value (R11.5).
 */
@Serializable
enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    LATE,
    ON_LEAVE,
    EXCUSED
}
