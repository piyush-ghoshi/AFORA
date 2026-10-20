package com.afora.backend.attendance.service

import com.afora.backend.academic.repository.LectureSessionRepository
import com.afora.backend.attendance.entity.AttendanceStatus
import com.afora.backend.attendance.repository.AttendanceRecordRepository
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDateTime

/**
 * Keeps a Lecture_Session's three denormalised counters consistent with its
 * stored Attendance_Records (requirement 7).
 *
 * Called from both the confirm transaction (B11) and the amend transaction
 * (B12), always after the caller has already flushed the record writes with
 * `attendanceRecordRepository.saveAllAndFlush(records)`. The flush is the
 * caller's responsibility, not this class's — relying on
 * `FlushModeType.AUTO` here would be exactly the kind of implicit behaviour
 * that produces an intermittent off-by-N counter.
 */
@Component
class SessionCounterRecalculator(
    private val attendanceRecordRepository: AttendanceRecordRepository,
    private val lectureSessionRepository: LectureSessionRepository,
    private val clock: Clock
) {

    companion object {
        /** PRESENT and LATE both count as attended (R7.2). */
        private val PRESENT_STATUSES = setOf(AttendanceStatus.PRESENT, AttendanceStatus.LATE)

        /** ON_LEAVE and EXCUSED are both neutral (R7.4). */
        private val ON_LEAVE_STATUSES = setOf(AttendanceStatus.ON_LEAVE, AttendanceStatus.EXCUSED)
    }

    /**
     * Recomputes `present_count`, `absent_count` and `on_leave_count` by
     * COUNTING the Attendance_Records currently stored for [sessionId] (R7.5)
     * — never by incrementing a previous value — and writes `updated_at`
     * from the injected [clock] (R7.8).
     *
     * Must be called after the record writes for this session have been
     * flushed, so the counts reflect what will actually be committed.
     */
    fun recompute(sessionId: Long): SessionCounters {
        val session = lectureSessionRepository.findById(sessionId)
            .orElseThrow { IllegalStateException("Lecture session $sessionId not found during counter recomputation") }

        val present = attendanceRecordRepository.countByLectureSessionIdAndStatusIn(sessionId, PRESENT_STATUSES)
        val absent = attendanceRecordRepository.countByLectureSessionIdAndStatusIn(sessionId, setOf(AttendanceStatus.ABSENT))
        val onLeave = attendanceRecordRepository.countByLectureSessionIdAndStatusIn(sessionId, ON_LEAVE_STATUSES)
        val total = attendanceRecordRepository.countByLectureSessionId(sessionId)

        // Partition tripwire (R7.6). {PRESENT, LATE}, {ABSENT} and
        // {ON_LEAVE, EXCUSED} partition the five AttendanceStatus values by
        // construction, so this can only fail if that partition is ever
        // broken without updating this class. A genuine programming error,
        // not a user-facing validation — it surfaces as HTTP 500.
        check(present + absent + onLeave == total) {
            "counter partition broken for session $sessionId: $present+$absent+$onLeave != $total"
        }

        lectureSessionRepository.save(
            session.copy(
                presentCount = present,
                absentCount = absent,
                onLeaveCount = onLeave,
                updatedAt = LocalDateTime.now(clock)
            )
        )

        return SessionCounters(present = present, absent = absent, onLeave = onLeave, total = total)
    }
}

/** The three denormalised counters, plus the total they must sum to (R7.6). */
data class SessionCounters(
    val present: Int,
    val absent: Int,
    val onLeave: Int,
    val total: Int
)
