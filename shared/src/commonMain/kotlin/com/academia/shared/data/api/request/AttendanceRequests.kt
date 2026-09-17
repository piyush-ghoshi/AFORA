package com.academia.shared.data.api.request

import com.academia.shared.domain.model.AttendanceCandidate
import kotlinx.serialization.Serializable

/**
 * Request to start a lecture session.
 */
@Serializable
data class StartLectureSessionRequest(
    val timetableSlotId: Long? = null,
    val subjectId: Long,
    val classSectionId: Long,
    val semesterId: Long,
    val date: String,  // ISO 8601 date
    val startTime: String,  // ISO 8601 time
    val endTime: String,  // ISO 8601 time
    val roomNumber: String? = null
)

/**
 * Request to confirm attendance.
 */
@Serializable
data class ConfirmAttendanceRequest(
    val lectureSessionId: Long,
    val attendanceCandidates: List<AttendanceCandidate>,
    val submissionId: String,  // Client-generated UUID for idempotency
    val method: String  // CAMERA, MANUAL, HYBRID
)
