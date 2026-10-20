package com.afora.shared.data.api

import com.afora.shared.domain.model.LeaveStatus
import kotlinx.serialization.Serializable

/**
 * Shared KMP wire types for the Leave Requests feature, field for field and
 * type for type with `backend/.../attendance/dto/LeaveDto.kt`, following the
 * [AttendanceWire] precedent: dates and timestamps are `String` on this side,
 * entity identifiers stay `Long`, and `status` keeps the typed [LeaveStatus]
 * enum since kotlinx.serialization cannot encode an undeclared constant.
 */

/** Request body for POST /api/leave/requests. A null [classSectionId] spans all of the student's classes. */
@Serializable
data class SubmitLeaveRequest(
    val fromDate: String,
    val toDate: String,
    val reason: String,
    val classSectionId: Long? = null
)

/** Request body for POST .../approve and POST .../reject. */
@Serializable
data class ReviewLeaveRequest(
    val notes: String? = null
)

/** Response shape returned by every leave endpoint. */
@Serializable
data class LeaveRequestResponse(
    val id: Long,
    val studentId: Long,
    val studentName: String,
    val rollNumber: String,
    val classSectionId: Long? = null,
    val classSectionName: String? = null,
    val fromDate: String,
    val toDate: String,
    val reason: String,
    val status: LeaveStatus,
    val reviewedByTeacherId: Long? = null,
    val reviewedByTeacherName: String? = null,
    val reviewedAt: String? = null,
    val reviewNotes: String? = null,
    val createdAt: String,
    val updatedAt: String
)

/** One class the student is enrolled in, offered as a leave-scope choice (GET /api/leave/classes). */
@Serializable
data class LeaveClassOption(
    val classSectionId: Long,
    val subjectCode: String,
    val subjectName: String,
    val sectionName: String
)
