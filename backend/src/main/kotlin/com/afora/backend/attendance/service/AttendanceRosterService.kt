package com.afora.backend.attendance.service

import com.afora.backend.academic.entity.LectureSession
import com.afora.backend.academic.entity.SessionStatus
import com.afora.backend.academic.repository.EnrollmentRepository
import com.afora.backend.attendance.dto.RosterEntry
import com.afora.backend.attendance.dto.RosterResponse
import com.afora.backend.attendance.entity.AttendanceStatus
import com.afora.backend.attendance.entity.LeaveRequest
import com.afora.backend.attendance.repository.AttendanceAuditEntryRepository
import com.afora.backend.attendance.repository.AttendanceRecordRepository
import com.afora.backend.attendance.repository.LeaveRequestRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Owns requirements 1 and 4: building the roster view for one Lecture_Session.
 *
 * Read-only; `@Transactional(readOnly = true)`. Criterion 4.7 ("no attendance
 * record derives from a suggestion alone") is structural here: this service
 * calls no save/persist method anywhere.
 */
@Service
@Transactional(readOnly = true)
class AttendanceRosterService(
    private val enrollmentRepository: EnrollmentRepository,
    private val attendanceRecordRepository: AttendanceRecordRepository,
    private val leaveRequestRepository: LeaveRequestRepository,
    private val auditEntryRepository: AttendanceAuditEntryRepository,
    private val clock: Clock
) {

    /** The number of hours an amendment window stays open after completion (R9.1, R9.2, R12.17). */
    private val amendmentWindowHours: Long = 168

    /**
     * Builds the [RosterResponse] for [session].
     *
     * Flow (R1, R4):
     * 1. The ACTIVE enrollment set for the session's class section.
     * 2. Index stored [com.afora.backend.attendance.entity.AttendanceRecord]
     *    values by student id, giving `storedStatus` or null (R1.4, R1.5).
     * 3. Approved leave covering the session date, for the suggested status
     *    (R4.1, R4.2).
     * 4. `isCorrected` from the audit entries that changed a record's status
     *    (R9.14, R11.9).
     * 5. Sort by roll number ascending (R1.6) and project the session header
     *    (R1.7).
     */
    fun roster(session: LectureSession): RosterResponse {
        val section = requireNotNull(session.classSection) { "lecture session ${session.id} has no class section" }
        val subject = requireNotNull(section.subject) { "class section ${section.id} has no subject" }

        val activeEnrollments = enrollmentRepository.findActiveRosterByClassSectionId(section.id)

        val recordsByStudentId = attendanceRecordRepository
            .findByLectureSessionId(session.id)
            .filter { it.student != null }
            .associateBy { it.student!!.id }

        val studentIds = activeEnrollments.mapNotNull { it.student?.id }

        // R4.1: lowest-id APPROVED leave request wins per student, since
        // findApprovedCovering already orders by id ascending.
        val coveringLeaveByStudentId: Map<Long?, LeaveRequest> = if (studentIds.isEmpty()) {
            emptyMap()
        } else {
            leaveRequestRepository
                .findApprovedCovering(studentIds, session.sessionDate, section.id)
                .mapNotNull { request -> request.student?.id?.let { it to request } }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, requests) -> requests.minBy { it.id } }
        }

        val correctedRecordIds = auditEntryRepository.findStatusChangedRecordIds(session.id).toSet()

        val entries = activeEnrollments.mapNotNull { enrollment ->
            val student = enrollment.student ?: return@mapNotNull null
            val studentId = requireNotNull(student.id) { "student with roll number ${student.rollNumber} has no id" }

            val storedRecord = recordsByStudentId[studentId]
            val coveringLeave = coveringLeaveByStudentId[studentId]
            val suggestedStatus = if (coveringLeave != null) AttendanceStatus.ON_LEAVE else AttendanceStatus.PRESENT

            RosterEntry(
                studentId = studentId,
                rollNumber = student.rollNumber,
                fullName = student.user.getFullName(),
                storedStatus = storedRecord?.status,
                suggestedStatus = suggestedStatus,
                attendanceRecordId = storedRecord?.id,
                leaveRequestId = coveringLeave?.id,
                leaveReason = coveringLeave?.reason,
                isCorrected = storedRecord != null && storedRecord.id in correctedRecordIds
            )
        }.sortedBy { it.rollNumber }

        return RosterResponse(
            sessionId = session.id,
            classSectionId = section.id,
            subjectId = subject.id,
            subjectCode = subject.code,
            subjectName = subject.name,
            sessionDate = session.sessionDate,
            startTime = session.startTime,
            endTime = session.endTime,
            sessionStatus = session.status,
            attendanceMethod = session.attendanceMethod,
            totalStudents = session.totalStudents,
            presentCount = session.presentCount,
            absentCount = session.absentCount,
            onLeaveCount = session.onLeaveCount,
            startedAt = session.startedAt,
            completedAt = session.completedAt,
            amendmentWindowOpen = isAmendmentWindowOpen(session),
            entries = entries
        )
    }

    /**
     * R12.17: computed here, server-side, from the single injected [Clock].
     * The client never evaluates the 168-hour rule itself.
     */
    private fun isAmendmentWindowOpen(session: LectureSession): Boolean {
        val completedAt = session.completedAt ?: return false
        if (session.status != SessionStatus.FINALIZED) return false
        val now = LocalDateTime.now(clock)
        return ChronoUnit.HOURS.between(completedAt, now) <= amendmentWindowHours
    }
}
