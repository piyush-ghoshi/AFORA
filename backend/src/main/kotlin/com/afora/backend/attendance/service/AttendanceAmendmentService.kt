package com.afora.backend.attendance.service

import com.afora.backend.academic.entity.SessionStatus
import com.afora.backend.academic.repository.LectureSessionRepository
import com.afora.backend.attendance.dto.AmendAttendanceRequestDto
import com.afora.backend.attendance.dto.AmendmentResultResponse
import com.afora.backend.attendance.dto.AuditEntry
import com.afora.backend.attendance.dto.AuditHistoryResponse
import com.afora.backend.attendance.entity.AttendanceAuditEntry
import com.afora.backend.attendance.entity.AttendanceStatus
import com.afora.backend.attendance.repository.AttendanceAuditEntryRepository
import com.afora.backend.attendance.repository.AttendanceRecordRepository
import com.afora.backend.attendance.security.CallerContext
import com.afora.backend.common.exception.BadRequestException
import com.afora.backend.common.exception.ConflictException
import com.afora.backend.common.exception.ForbiddenException
import com.afora.backend.common.exception.ResourceNotFoundException
import com.afora.backend.user.entity.UserRole
import com.afora.backend.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Owns requirement 9: amending one or more Attendance_Records of a FINALIZED
 * Lecture_Session, with an append-only audit trail, and reading that trail
 * back (9.16).
 *
 * Same error-details caveat as [AttendanceConfirmationService]: structured
 * `details` keys (e.g. missingRecordStudentIds) are not attachable to the
 * response without extending GlobalExceptionHandler, which this feature does
 * not modify. Offending ids are embedded in the exception message instead.
 */
@Service
class AttendanceAmendmentService(
    private val lectureSessionRepository: LectureSessionRepository,
    private val attendanceRecordRepository: AttendanceRecordRepository,
    private val attendanceAuditEntryRepository: AttendanceAuditEntryRepository,
    private val sessionCounterRecalculator: SessionCounterRecalculator,
    private val userRepository: UserRepository,
    private val clock: Clock
) {

    /** R9.1-R9.3: the window a TEACHER may amend within; ADMIN is unbounded. */
    private val amendmentWindowHours: Long = 168

    /**
     * Amends [sessionId]'s records per [request]. Order is normative
     * (design.md, amendment transaction): lock -> FINALIZED gate -> blank
     * reason -> window -> load named records -> per-entry no-op skip or
     * write-plus-audit -> flush + recompute. The session's status is never
     * written here -- it simply stays FINALIZED (3.9) because this method
     * does not touch that field.
     */
    @Transactional
    fun amend(
        sessionId: Long,
        caller: CallerContext,
        request: AmendAttendanceRequestDto
    ): AmendmentResultResponse {
        // 1. Same lock as confirm, so an amendment cannot interleave with a confirmation.
        val session = lectureSessionRepository.findByIdForUpdate(sessionId)
            ?: throw ResourceNotFoundException("Lecture session $sessionId not found")

        // 2. R9.12.
        if (session.status != SessionStatus.FINALIZED) {
            throw ConflictException(
                "Lecture session $sessionId is not FINALIZED (current status: ${session.status}); " +
                    "only a finalized session can be amended"
            )
        }

        // 3. R9.4, R9.5: Kotlin's isBlank() is Unicode-aware.
        if (request.reason.isBlank()) {
            throw BadRequestException("An amendment reason is required and must contain a non-whitespace character")
        }

        // 4. R9.1, R9.2, R9.3.
        if (caller.role == UserRole.TEACHER) {
            val completedAt = requireNotNull(session.completedAt) {
                "FINALIZED lecture session $sessionId has no completedAt"
            }
            val elapsedHours = ChronoUnit.HOURS.between(completedAt, LocalDateTime.now(clock))
            if (elapsedHours > amendmentWindowHours) {
                throw ForbiddenException(
                    "The $amendmentWindowHours-hour amendment window for lecture session $sessionId has passed; " +
                        "ask an administrator to make this correction"
                )
            }
        }

        // 5. R9.11: a named student with no record is a 404.
        val requestedStudentIds = request.entries.map { it.studentId }
        val recordsByStudentId = attendanceRecordRepository
            .findByLectureSessionIdAndStudentIdIn(sessionId, requestedStudentIds)
            .mapNotNull { record -> record.student?.id?.let { it to record } }
            .toMap()
        val missingIds = (requestedStudentIds.toSet() - recordsByStudentId.keys).sorted()
        if (missingIds.isNotEmpty()) {
            throw ResourceNotFoundException(
                "No attendance record exists for lecture session $sessionId and student(s): $missingIds"
            )
        }

        // 6. Per entry: no-op skips (R9.13) write nothing and no audit entry; a real change
        // writes status + updatedAt while preserving markedAt/markedByTeacher (R9.7, R9.8)
        // and appends exactly one audit entry (R9.6).
        val now = LocalDateTime.now(clock)
        val amendedRecords = mutableListOf<com.afora.backend.attendance.entity.AttendanceRecord>()
        val auditEntries = mutableListOf<AttendanceAuditEntry>()

        for (entry in request.entries) {
            val record = requireNotNull(recordsByStudentId[entry.studentId])
            val newStatus = try {
                AttendanceStatus.valueOf(entry.status)
            } catch (e: IllegalArgumentException) {
                throw BadRequestException(
                    "Invalid attendance status '${entry.status}' for student ${entry.studentId}; " +
                        "must be one of ${AttendanceStatus.entries.joinToString()}"
                )
            }

            if (newStatus == record.status) {
                continue // R9.13: no-op -- no write, no audit entry.
            }

            amendedRecords += record.copy(status = newStatus, updatedAt = now)
            auditEntries += AttendanceAuditEntry(
                attendanceRecordId = record.id,
                lectureSessionId = sessionId,
                studentId = entry.studentId,
                previousStatus = record.status,
                newStatus = newStatus,
                changedByUserId = caller.userId,
                reason = request.reason,
                changedAt = now
            )
        }

        // 7. Explicit flush, then recompute from the stored records.
        if (amendedRecords.isNotEmpty()) {
            attendanceRecordRepository.saveAllAndFlush(amendedRecords)
            attendanceAuditEntryRepository.saveAll(auditEntries)
        }
        val counters = sessionCounterRecalculator.recompute(sessionId)

        // 8. Session status is deliberately never written here (3.9): FINALIZED stands.

        return AmendmentResultResponse(
            sessionId = sessionId,
            sessionStatus = SessionStatus.FINALIZED,
            amendedCount = amendedRecords.size,
            unchangedCount = request.entries.size - amendedRecords.size,
            presentCount = counters.present,
            absentCount = counters.absent,
            onLeaveCount = counters.onLeave,
            auditEntryIds = auditEntries.map { it.id },
            amendedAt = now
        )
    }

    /** R9.16: the full audit trail for a session, change timestamp ascending. */
    @Transactional(readOnly = true)
    fun auditHistory(sessionId: Long): AuditHistoryResponse {
        val entries = attendanceAuditEntryRepository.findByLectureSessionIdOrderByChangedAtAsc(sessionId)
        return AuditHistoryResponse(
            sessionId = sessionId,
            entries = entries.map { entry ->
                val record = attendanceRecordRepository.findById(entry.attendanceRecordId).orElse(null)
                val changedByName = userRepository.findById(entry.changedByUserId)
                    .map { it.getFullName() }
                    .orElse("Unknown user")
                AuditEntry(
                    auditEntryId = entry.id,
                    attendanceRecordId = entry.attendanceRecordId,
                    studentId = entry.studentId,
                    rollNumber = record?.student?.rollNumber ?: "",
                    previousStatus = entry.previousStatus,
                    newStatus = entry.newStatus,
                    changedByUserId = entry.changedByUserId,
                    changedByName = changedByName,
                    reason = entry.reason,
                    changedAt = entry.changedAt
                )
            }
        )
    }
}