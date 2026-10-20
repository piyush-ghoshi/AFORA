package com.afora.backend.attendance.dto

import com.afora.backend.academic.entity.AttendanceMethod
import com.afora.backend.academic.entity.SessionStatus
import com.afora.backend.attendance.entity.AttendanceStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// ── Roster ─────────────────────────────────────────────────────────────────

/**
 * Response for GET /api/attendance/sessions/{sessionId}/roster.
 *
 * `totalStudents` stays null until activation (R7.7). `amendmentWindowOpen` is
 * computed server-side from the single injected Clock:
 * sessionStatus == FINALIZED && completedAt != null && HOURS.between(completedAt, now) <= 168.
 * The client never evaluates the 168-hour rule itself (R12.17).
 */
data class RosterResponse(
    val sessionId: Long,
    val classSectionId: Long,
    val subjectId: Long,
    val subjectCode: String,
    val subjectName: String,
    val sessionDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val sessionStatus: SessionStatus,
    val attendanceMethod: AttendanceMethod?,
    val totalStudents: Int?,
    val presentCount: Int,
    val absentCount: Int,
    val onLeaveCount: Int,
    val startedAt: LocalDateTime?,
    val completedAt: LocalDateTime?,
    val amendmentWindowOpen: Boolean,
    val entries: List<RosterEntry> = emptyList()
)

/**
 * One roster row. `storedStatus` is null exactly when no attendance record
 * exists for this student yet (R1.4, R1.5). `suggestedStatus` is ON_LEAVE
 * where an approved leave request covers the session date, otherwise
 * PRESENT (R4.1, R4.2).
 */
data class RosterEntry(
    val studentId: Long,
    val rollNumber: String,
    val fullName: String,
    val storedStatus: AttendanceStatus?,
    val suggestedStatus: AttendanceStatus,
    val attendanceRecordId: Long?,
    val leaveRequestId: Long?,
    val leaveReason: String?,
    val isCorrected: Boolean
)

// ── Activate / Cancel ───────────────────────────────────────────────────────

/**
 * Response for POST .../activate and POST .../cancel. Neither has a request
 * body.
 */
data class SessionSummary(
    val sessionId: Long,
    val sessionStatus: SessionStatus,
    val sessionDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val attendanceMethod: AttendanceMethod?,
    val totalStudents: Int?,
    val presentCount: Int,
    val absentCount: Int,
    val onLeaveCount: Int,
    val startedAt: LocalDateTime?,
    val completedAt: LocalDateTime?
)

// ── Confirm ──────────────────────────────────────────────────────────────────

/**
 * Request body for POST .../confirm.
 *
 * No `sessionId` field — the session identifier lives only in the path (so
 * the fingerprint of R6.3 has exactly one source for it). No `method` field
 * either — R5.9 fixes method = MANUAL server-side.
 */
data class ConfirmAttendanceRequestDto(
    val submissionId: String,
    val markUnspecifiedAsAbsent: Boolean = false,
    val entries: List<SubmittedAttendanceEntry> = emptyList(),
    val method: String? = null
)

/**
 * `status` is declared String, not the typed enum (R5.8): a typed enum would
 * make Jackson throw HttpMessageNotReadableException on an unknown name,
 * which GlobalExceptionHandler does not handle and would surface as HTTP 500.
 * String + service-side parsing yields a BadRequestException naming the
 * offending value instead.
 */
data class SubmittedAttendanceEntry(
    val studentId: Long,
    val status: String,
    val notes: String? = null,
    val confidenceScore: Double? = null,
    val teacherConfirmed: Boolean = false
)

/**
 * Response for POST .../confirm.
 *
 * presentCount counts PRESENT and LATE (R7.2); onLeaveCount counts ON_LEAVE
 * and EXCUSED (R7.4) — neither is a count of just the one named status.
 * presentCount + absentCount + onLeaveCount == totalMarked always (R7.6).
 * `replayed` is true only on the idempotent-replay path of R6.5.
 */
data class AttendanceConfirmationResponse(
    val sessionId: Long,
    val submissionId: String,
    val sessionStatus: SessionStatus,
    val totalMarked: Int,
    val presentCount: Int,
    val absentCount: Int,
    val onLeaveCount: Int,
    val defaultedStudentIds: List<Long> = emptyList(),
    val confirmedAt: LocalDateTime,
    val replayed: Boolean
)

// ── Amend ────────────────────────────────────────────────────────────────────

/**
 * Request body for POST .../amend.
 */
data class AmendAttendanceRequestDto(
    val reason: String,
    val entries: List<AmendmentEntry> = emptyList()
)

/**
 * Same `status` asymmetry as [SubmittedAttendanceEntry], same reason (R5.8).
 */
data class AmendmentEntry(
    val studentId: Long,
    val status: String
)

/**
 * Response for POST .../amend.
 *
 * `amendedCount` counts only status-changing entries; no-ops land in
 * `unchangedCount` and produce no audit entry (R9.13).
 * `auditEntryIds.size == amendedCount` always.
 */
data class AmendmentResultResponse(
    val sessionId: Long,
    val sessionStatus: SessionStatus,
    val amendedCount: Int,
    val unchangedCount: Int,
    val presentCount: Int,
    val absentCount: Int,
    val onLeaveCount: Int,
    val auditEntryIds: List<Long> = emptyList(),
    val amendedAt: LocalDateTime
)

// ── Audit history ────────────────────────────────────────────────────────────

/**
 * Response for GET .../audit-history. `entries` is ordered by `changedAt`
 * ascending (R9.16).
 */
data class AuditHistoryResponse(
    val sessionId: Long,
    val entries: List<AuditEntry> = emptyList()
)

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
    val changedAt: LocalDateTime
)

// ── Student Attendance Detail (Phase 4) ──────────────────────────────────────

/**
 * Response for GET /api/attendance/student/{studentId}.
 *
 * Overall attendance across all enrolled subjects for the given semester,
 * plus per-subject breakdown and per-session record list.
 */
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

data class SessionAttendanceEntry(
    val sessionId: Long,
    val sessionDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val status: AttendanceStatus,
    val isCorrected: Boolean,
    val notes: String?
)
