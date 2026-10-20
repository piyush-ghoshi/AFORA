package com.afora.backend.attendance.service

import com.afora.backend.academic.entity.AttendanceMethod
import com.afora.backend.academic.entity.EnrollmentStatus
import com.afora.backend.academic.entity.LectureSession
import com.afora.backend.academic.entity.SessionStatus
import com.afora.backend.academic.repository.EnrollmentRepository
import com.afora.backend.academic.repository.LectureSessionRepository
import com.afora.backend.attendance.dto.SessionSummary
import com.afora.backend.attendance.security.CallerContext
import com.afora.backend.common.exception.BadRequestException
import com.afora.backend.common.exception.ForbiddenException
import com.afora.backend.user.entity.UserRole
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Owns requirements 2, 3.7 and 7.7: activating and cancelling a
 * Lecture_Session in the Manual_Marking_Flow.
 *
 * This service does not perform the ownership authorization check itself —
 * that happens in the controller via [com.afora.backend.attendance.security.AttendanceAccessGuard]
 * before this service is called. It does need the caller's role to apply the
 * TEACHER-only 14-day activation window (criteria 2.5, 2.6, 2.7).
 *
 * All dates and timestamps come from the single injected [Clock] (R2.10).
 */
@Service
class SessionLifecycleService(
    private val lectureSessionRepository: LectureSessionRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val clock: Clock
) {

    /**
     * Activation, in this normative order:
     *
     * 1. Already-ACTIVE no-op return (criterion 2.8) — **before** the window
     *    checks, so re-activating an old session still succeeds even past the
     *    14-day window.
     * 2. [SessionTransitions.require] against ACTIVE — throws [com.afora.backend.common.exception.ConflictException]
     *    if the transition is illegal (3.1, 3.2).
     * 3. Future-date check — [BadRequestException] if the session date is
     *    after the current server date (2.4). Deliberately before the
     *    role/window check: a future-dated session is malformed regardless
     *    of who asks (400-before-403 ordering).
     * 4. TEACHER 14-day window check — [ForbiddenException] if a TEACHER
     *    tries to activate a session dated more than 14 days before the
     *    current server date (2.5, 2.6). Skipped entirely for ADMIN (2.7).
     * 5. Write status = ACTIVE, startedAt (2.2), attendanceMethod = MANUAL
     *    (2.3), totalStudents = count of ACTIVE enrollments (7.7),
     *    updatedAt.
     */
    @Transactional
    fun activate(session: LectureSession, caller: CallerContext): SessionSummary {
        if (session.status == SessionStatus.ACTIVE) {
            return session.toSummary()
        }

        SessionTransitions.require(session.status, SessionStatus.ACTIVE)

        val today = LocalDate.now(clock)
        if (session.sessionDate.isAfter(today)) {
            throw BadRequestException(
                "Cannot activate lecture session ${session.id}: session date " +
                    "${session.sessionDate} is after the current date $today"
            )
        }

        if (caller.role == UserRole.TEACHER) {
            val daysBeforeToday = ChronoUnit.DAYS.between(session.sessionDate, today)
            if (daysBeforeToday > 14) {
                throw ForbiddenException(
                    "Only an administrator may activate lecture session ${session.id}: " +
                        "it is dated more than 14 days before the current date"
                )
            }
        }

        val classSectionId = requireNotNull(session.classSection?.id) {
            "Lecture session ${session.id} has no class section"
        }
        val totalStudents = enrollmentRepository.countByClassSectionIdAndStatus(
            classSectionId,
            EnrollmentStatus.ACTIVE
        )

        val activated = session.copy(
            status = SessionStatus.ACTIVE,
            startedAt = LocalDateTime.now(clock),
            attendanceMethod = AttendanceMethod.MANUAL,
            totalStudents = totalStudents,
            updatedAt = LocalDateTime.now(clock)
        )

        return lectureSessionRepository.save(activated).toSummary()
    }

    /**
     * [SessionTransitions.require] against CANCELLED, then write ONLY
     * `status` and `updatedAt`. Touches no attendance record and no counter —
     * that is how criteria 3.8 and 8.6 are satisfied, by writing less. The
     * counter recalculator is deliberately not called here.
     */
    @Transactional
    fun cancel(session: LectureSession, caller: CallerContext): SessionSummary {
        SessionTransitions.require(session.status, SessionStatus.CANCELLED)

        val cancelled = session.copy(
            status = SessionStatus.CANCELLED,
            updatedAt = LocalDateTime.now(clock)
        )

        return lectureSessionRepository.save(cancelled).toSummary()
    }

    private fun LectureSession.toSummary(): SessionSummary = SessionSummary(
        sessionId = id,
        sessionStatus = status,
        sessionDate = sessionDate,
        startTime = startTime,
        endTime = endTime,
        attendanceMethod = attendanceMethod,
        totalStudents = totalStudents,
        presentCount = presentCount,
        absentCount = absentCount,
        onLeaveCount = onLeaveCount,
        startedAt = startedAt,
        completedAt = completedAt
    )
}
