package com.afora.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Lecture session entity - represents a single class period.
 */
@Serializable
data class LectureSession(
    val id: Long,
    val subjectId: Long,
    val classSectionId: Long,
    val teacherId: Long,
    val semesterId: Long,
    val date: String,  // ISO 8601 date (YYYY-MM-DD)
    val startTime: String,  // ISO 8601 time (HH:mm:ss)
    val endTime: String,  // ISO 8601 time (HH:mm:ss)
    val roomNumber: String? = null,
    val status: SessionStatus = SessionStatus.SCHEDULED,
    val attendanceMethod: AttendanceMethod? = null,
    val totalStudents: Int? = null,
    val presentCount: Int = 0,
    val absentCount: Int = 0,
    val onLeaveCount: Int = 0,
    val startedAt: String? = null,  // ISO 8601 datetime
    val completedAt: String? = null,  // ISO 8601 datetime
    val notes: String? = null
)

/**
 * Lecture session status (state machine).
 */
@Serializable
enum class SessionStatus {
    SCHEDULED,
    CREATED,
    ACTIVE,
    SCANNING,
    REVIEW,
    FINALIZING,
    FINALIZED,
    CANCELLED,
    FAILED
}

/**
 * Attendance capture method.
 */
@Serializable
enum class AttendanceMethod {
    CAMERA,
    MANUAL,
    HYBRID  // Mix of camera and manual
}
