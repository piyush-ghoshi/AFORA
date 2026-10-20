package com.afora.backend.attendance.dto

import com.afora.backend.attendance.entity.LeaveStatus
import java.time.LocalDate
import java.time.LocalDateTime

// ── Submit ───────────────────────────────────────────────────────────────────

/**
 * Request body for POST /api/leave/requests.
 *
 * `classSectionId` is optional: a null value means the request spans all of
 * the student's classes, mirroring the entity's own [com.afora.backend.attendance.entity.LeaveRequest.classSection]
 * nullability.
 */
data class SubmitLeaveRequestDto(
    val fromDate: LocalDate,
    val toDate: LocalDate,
    val reason: String,
    val classSectionId: Long? = null
)

// ── Review (approve / reject) ───────────────────────────────────────────────

/**
 * Request body for POST .../approve and POST .../reject.
 *
 * `notes` is optional for approve, required (validated non-blank in the
 * service) for reject.
 */
data class ReviewLeaveRequestDto(
    val notes: String? = null
)

// ── Response ─────────────────────────────────────────────────────────────────

/**
 * Response shape for every leave endpoint: submit, the two list endpoints,
 * approve, reject and cancel all return this same projection.
 */
data class LeaveRequestResponse(
    val id: Long,
    val studentId: Long,
    val studentName: String,
    val rollNumber: String,
    val classSectionId: Long?,
    val classSectionName: String?,
    val fromDate: LocalDate,
    val toDate: LocalDate,
    val reason: String,
    val status: LeaveStatus,
    val reviewedByTeacherId: Long?,
    val reviewedByTeacherName: String?,
    val reviewedAt: LocalDateTime?,
    val reviewNotes: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

// -- Class options (leave scope picker) -----------------------------------

/**
 * One class the calling student is actively enrolled in, offered as a
 * leave-scope choice. GET /api/leave/classes.
 */
data class LeaveClassOptionResponse(
    val classSectionId: Long,
    val subjectCode: String,
    val subjectName: String,
    val sectionName: String
)
