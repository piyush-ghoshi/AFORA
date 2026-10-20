package com.afora.shared.data.api

import com.afora.shared.domain.model.AttendanceMethod
import com.afora.shared.domain.model.AttendanceStatus
import com.afora.shared.domain.model.SessionStatus
import kotlinx.serialization.Serializable

/**
 * Shared KMP wire types for the Manual_Marking_Flow (attendance-marking feature).
 *
 * One `@Serializable` type per payload, matching the normative Wire Contract
 * (`.kiro/specs/attendance-marking/design.md` → "The Wire Contract") field for
 * field and type for type with the backend DTOs in
 * `backend/.../attendance/dto/AttendanceDto.kt`. No domain mirror, no
 * `@SerialName` anywhere — the serialized enum name is the constant name.
 *
 * Scalar conventions: `semesterId`-shaped UUID identifiers, session dates,
 * times and timestamps are all `String` on the shared side (R13.10, R13.11).
 * Entity identifiers stay `Long`. Enum fields keep the typed enum on this
 * side, unlike the backend DTO's request-side `status: String` (R5.8).
 */

// ── Roster ─────────────────────────────────────────────────────────────────

/**
 * Response for GET /api/attendance/sessions/{sessionId}/roster.
 *
 * `totalStudents` stays null until activation. `amendmentWindowOpen` is
 * computed server-side from the single injected Clock; the client never
 * evaluates the 168-hour rule itself (R12.17).
 */
@Serializable
data class RosterResponse(
    val sessionId: Long,
    val classSectionId: Long,
    val subjectId: Long,
    val subjectCode: String,
    val subjectName: String,
    val sessionDate: String,
    val startTime: String,
    val endTime: String,
    val sessionStatus: SessionStatus,
    val attendanceMethod: AttendanceMethod? = null,
    val totalStudents: Int? = null,
    val presentCount: Int,
    val absentCount: Int,
    val onLeaveCount: Int,
    val startedAt: String? = null,
    val completedAt: String? = null,
    val amendmentWindowOpen: Boolean,
    val entries: List<RosterEntry> = emptyList()
)

/**
 * One roster row. `storedStatus` is null exactly when no attendance record
 * exists for this student yet (R1.4, R1.5). `suggestedStatus` is ON_LEAVE
 * where an approved leave request covers the session date, otherwise
 * PRESENT (R4.1, R4.2).
 */
@Serializable
data class RosterEntry(
    val studentId: Long,
    val rollNumber: String,
    val fullName: String,
    val storedStatus: AttendanceStatus? = null,
    val suggestedStatus: AttendanceStatus,
    val attendanceRecordId: Long? = null,
    val leaveRequestId: Long? = null,
    val leaveReason: String? = null,
    val isCorrected: Boolean
)

// ── Activate / Cancel ───────────────────────────────────────────────────────

/**
 * Response for POST .../activate and POST .../cancel. Neither has a request
 * body.
 */
@Serializable
data class SessionSummary(
    val sessionId: Long,
    val sessionStatus: SessionStatus,
    val sessionDate: String,
    val startTime: String,
    val endTime: String,
    val attendanceMethod: AttendanceMethod? = null,
    val totalStudents: Int? = null,
    val presentCount: Int,
    val absentCount: Int,
    val onLeaveCount: Int,
    val startedAt: String? = null,
    val completedAt: String? = null
)

// ── Confirm ──────────────────────────────────────────────────────────────────

/**
 * Request body for POST .../confirm.
 *
 * No `sessionId` field — the session identifier lives only in the path (so
 * the fingerprint of R6.3 has exactly one source for it). No `method` field
 * either — R5.9 fixes method = MANUAL server-side.
 */
@Serializable
data class ConfirmAttendanceRequest(
    val submissionId: String,
    val markUnspecifiedAsAbsent: Boolean = false,
    val entries: List<SubmittedAttendanceEntry> = emptyList(),
    val method: String? = null
)

/**
 * The shared side keeps the typed enum for `status` because kotlinx.serialization
 * can only encode a declared constant, so a client cannot produce an invalid
 * name in the first place — unlike the backend DTO's `String` (R5.8).
 */
@Serializable
data class SubmittedAttendanceEntry(
    val studentId: Long,
    val status: AttendanceStatus,
    val notes: String? = null,
    val confidenceScore: Double? = null,
    val teacherConfirmed: Boolean = false
)

/**
 * Response for POST .../confirm.
 *
 * `presentCount` counts PRESENT and LATE (R7.2); `onLeaveCount` counts
 * ON_LEAVE and EXCUSED (R7.4) — neither is a count of just the one named
 * status. `presentCount + absentCount + onLeaveCount == totalMarked` always
 * (R7.6). `replayed` is true only on the idempotent-replay path of R6.5.
 */
@Serializable
data class AttendanceConfirmationResponse(
    val sessionId: Long,
    val submissionId: String,
    val sessionStatus: SessionStatus,
    val totalMarked: Int,
    val presentCount: Int,
    val absentCount: Int,
    val onLeaveCount: Int,
    val defaultedStudentIds: List<Long> = emptyList(),
    val confirmedAt: String,
    val replayed: Boolean
)

// ── Amend ────────────────────────────────────────────────────────────────────

/**
 * Request body for POST .../amend.
 */
@Serializable
data class AmendAttendanceRequest(
    val reason: String,
    val entries: List<AmendmentEntry> = emptyList()
)

/**
 * Same `status` asymmetry as [SubmittedAttendanceEntry], same reason (R5.8).
 */
@Serializable
data class AmendmentEntry(
    val studentId: Long,
    val status: AttendanceStatus
)

/**
 * Response for POST .../amend.
 *
 * `amendedCount` counts only status-changing entries; no-ops land in
 * `unchangedCount` and produce no audit entry (R9.13).
 * `auditEntryIds.size == amendedCount` always.
 */
@Serializable
data class AmendmentResultResponse(
    val sessionId: Long,
    val sessionStatus: SessionStatus,
    val amendedCount: Int,
    val unchangedCount: Int,
    val presentCount: Int,
    val absentCount: Int,
    val onLeaveCount: Int,
    val auditEntryIds: List<Long> = emptyList(),
    val amendedAt: String
)

// ── Audit history ────────────────────────────────────────────────────────────

/**
 * Response for GET .../audit-history. `entries` is ordered by `changedAt`
 * ascending (R9.16).
 */
@Serializable
data class AuditHistoryResponse(
    val sessionId: Long,
    val entries: List<AuditEntry> = emptyList()
)

@Serializable
data class AuditEntry(
    val auditEntryId: Long,
    val attendanceRecordId: Long,
    val studentId: Long,
    val rollNumber: String,
    val previousStatus: AttendanceStatus,
    val newStatus: AttendanceStatus,
    val changedByUserId: Long,
    val changedByName: String,
    val reason: String,
    val changedAt: String
)

// ── Student Attendance Detail (Phase 4) ──────────────────────────────────────

/**
 * Response for GET /api/attendance/student/{studentId}.
 */
@Serializable
data class StudentAttendanceResponse(
    val studentId: Long,
    val semesterId: String,
    val overallPercentage: Double,
    val totalLectures: Int,
    val presentCount: Int,
    val absentCount: Int,
    val onLeaveCount: Int,
    val subjects: List<SubjectAttendanceSummary>
)

@Serializable
data class SubjectAttendanceSummary(
    val subjectId: Long,
    val subjectCode: String,
    val subjectName: String,
    val totalLectures: Int,
    val presentCount: Int,
    val absentCount: Int,
    val onLeaveCount: Int,
    val attendancePercentage: Double,
    val sessions: List<SessionAttendanceEntry>
)

@Serializable
data class SessionAttendanceEntry(
    val sessionId: Long,
    val sessionDate: String,    // ISO date string
    val startTime: String,      // HH:mm:ss
    val endTime: String,        // HH:mm:ss
    val status: AttendanceStatus,
    val isCorrected: Boolean,
    val notes: String? = null
)
