package com.afora.backend.attendance.service

import com.afora.backend.academic.entity.AttendanceMethod
import com.afora.backend.academic.entity.EnrollmentStatus
import com.afora.backend.academic.entity.SessionStatus
import com.afora.backend.academic.repository.EnrollmentRepository
import com.afora.backend.academic.repository.LectureSessionRepository
import com.afora.backend.attendance.dto.AttendanceConfirmationResponse
import com.afora.backend.attendance.dto.ConfirmAttendanceRequestDto
import com.afora.backend.attendance.entity.AttendanceRecord
import com.afora.backend.attendance.entity.AttendanceStatus
import com.afora.backend.attendance.entity.AttendanceSubmission
import com.afora.backend.attendance.repository.AttendanceRecordRepository
import com.afora.backend.attendance.repository.AttendanceSubmissionRepository
import com.afora.backend.attendance.security.CallerContext
import com.afora.backend.common.exception.BadRequestException
import com.afora.backend.common.exception.ConflictException
import com.afora.backend.common.exception.ResourceNotFoundException
import com.afora.backend.facerecognition.service.FaceMatchReviewGate
import com.afora.backend.user.entity.UserRole
import com.afora.backend.user.repository.TeacherRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

/**
 * Owns requirements 5 and 6: persisting a teacher's (or admin's) attendance
 * confirmation for one Lecture_Session, idempotently, and finalizing it.
 *
 * NOTE on error details (read before changing error messages): the design
 * states GlobalExceptionHandler is not modified by this feature, but the
 * existing BadRequestException/ConflictException/ResourceNotFoundException
 * constructors only carry a message string -- there is no path today to
 * attach a structured `details` map (e.g. omittedStudentIds: [104, 107]) to
 * the HTTP response without touching that handler. This service therefore
 * embeds every offending id directly in the exception message text, which
 * satisfies "every error message is written to be complete on its own,
 * without the details" from the design's Wire Contract section. If a later
 * task needs a genuine structured `details` map on the wire, extending
 * GlobalExceptionHandler is the honest fix -- flagging rather than silently
 * declaring compliance with a details-map contract that cannot exist yet.
 */
import com.afora.backend.notification.dto.PushNotificationPayload
import com.afora.backend.notification.service.NotificationPushService
import org.springframework.beans.factory.annotation.Autowired

@Service
class AttendanceConfirmationService(
    private val lectureSessionRepository: LectureSessionRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val attendanceRecordRepository: AttendanceRecordRepository,
    private val attendanceSubmissionRepository: AttendanceSubmissionRepository,
    private val teacherRepository: TeacherRepository,
    private val sessionCounterRecalculator: SessionCounterRecalculator,
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
    @Autowired(required = false)
    private val notificationPushService: NotificationPushService? = null
) {

    /**
     * Confirms attendance for [sessionId], finalizing it on success.
     *
     * Step order is normative (design.md, "The confirmation transaction"):
     * lock -> parse submissionId -> parse statuses -> reject duplicates ->
     * fingerprint -> submission lookup (replay / conflict) -> ACTIVE gate ->
     * membership + completeness -> build records -> flush + recompute ->
     * finalize -> persist the submission.
     */
    @Transactional
    fun confirm(
        sessionId: Long,
        caller: CallerContext,
        request: ConfirmAttendanceRequestDto
    ): AttendanceConfirmationResponse {
        // 1. R6.11: the pessimistic lock, acquired as the first statement.
        val session = lectureSessionRepository.findByIdForUpdate(sessionId)
            ?: throw ResourceNotFoundException("Lecture session $sessionId not found")

        // 2. R6.1, R6.2.
        val submissionId = try {
            UUID.fromString(request.submissionId)
        } catch (e: IllegalArgumentException) {
            throw BadRequestException("submissionId '${request.submissionId}' is not a valid UUID")
        }

        // 3. R5.8: a name outside the five values is a bad request, never a 500.
        val parsedStatusByEntry = request.entries.associate { entry ->
            val status = try {
                AttendanceStatus.valueOf(entry.status)
            } catch (e: IllegalArgumentException) {
                throw BadRequestException(
                    "Invalid attendance status '${entry.status}' for student ${entry.studentId}; " +
                        "must be one of ${AttendanceStatus.entries.joinToString()}"
                )
            }
            entry.studentId to (status to entry.notes)
        }

        // 4. R5.7.
        val duplicateIds = request.entries.groupingBy { it.studentId }.eachCount()
            .filterValues { it > 1 }.keys
        if (duplicateIds.isNotEmpty()) {
            throw BadRequestException("Duplicate student ids in submission: ${duplicateIds.sorted()}")
        }

        // 5. R6.3.
        val fingerprint = RequestFingerprint.of(sessionId, request.entries)

        // 6. R6.4-R6.7: replay on equal fingerprint, conflict on a changed payload.
        val existingSubmission = attendanceSubmissionRepository.findById(submissionId).orElse(null)
        if (existingSubmission != null) {
            if (existingSubmission.requestFingerprint == fingerprint) {
                val stored = objectMapper.readValue(
                    existingSubmission.confirmationJson,
                    AttendanceConfirmationResponse::class.java
                )
                return stored.copy(replayed = true)
            }
            throw ConflictException(
                "submissionId ${request.submissionId} was already used for a different confirmation payload " +
                    "on lecture session $sessionId"
            )
        }

        // 7. R2.9, R6.8: not ACTIVE conflicts; FINALIZED names the amendment path.
        if (session.status != SessionStatus.ACTIVE) {
            if (session.status == SessionStatus.FINALIZED) {
                throw ConflictException(
                    "Lecture session $sessionId is already finalized; use the amendment endpoint to correct it"
                )
            }
            throw ConflictException(
                "Lecture session $sessionId is not ACTIVE (current status: ${session.status})"
            )
        }

        // 7.5. R8.5, R9.6: server-side enforcement of the Review_Queue gate (Settled Decision B) —
        // recomputed from confidenceScore, never from the client-reported method.
        FaceMatchReviewGate.require(request.entries)

        // 8. R5.1, R5.2, R5.4, R5.5, R5.6: membership and completeness.
        val classSectionId = requireNotNull(session.classSection?.id) {
            "lecture session $sessionId has no class section"
        }
        val activeEnrollments = enrollmentRepository.findActiveRosterByClassSectionId(classSectionId)
        val studentById = activeEnrollments.mapNotNull { enrollment ->
            enrollment.student?.let { student -> student.id?.let { id -> id to student } }
        }.toMap()
        val activeStudentIds = studentById.keys

        val submittedStudentIds = request.entries.map { it.studentId }.toSet()
        val unknownIds = submittedStudentIds - activeStudentIds
        if (unknownIds.isNotEmpty()) {
            throw BadRequestException(
                "Entries submitted for students not actively enrolled in this class section: ${unknownIds.sorted()}"
            )
        }

        val omittedIds = (activeStudentIds - submittedStudentIds).sorted()
        val defaultedStudentIds: List<Long>
        if (omittedIds.isNotEmpty() && !request.markUnspecifiedAsAbsent) {
            throw BadRequestException(
                "Attendance submission omits ${omittedIds.size} enrolled student(s): $omittedIds"
            )
        }
        defaultedStudentIds = if (request.markUnspecifiedAsAbsent) omittedIds else emptyList()

        // 9. R5.9, R5.10, R5.11, R5.12, R5.17.
        val now = LocalDateTime.now(clock)
        val markedByTeacher = if (caller.role == UserRole.TEACHER) {
            teacherRepository.findById(requireNotNull(caller.teacherId) { "TEACHER caller has no teacherId" })
                .orElseThrow { IllegalStateException("Caller's teacher id ${caller.teacherId} was not found") }
        } else {
            null // R5.17: ADMIN confirmations attribute to nobody; the audit trail covers who acted.
        }

        val requestedMethod = when (request.method?.uppercase()) {
            "CAMERA" -> AttendanceMethod.CAMERA
            "HYBRID" -> AttendanceMethod.HYBRID
            else -> AttendanceMethod.MANUAL
        }
        val confidenceByStudent = request.entries.associate { it.studentId to it.confidenceScore }

        val records = activeStudentIds.map { studentId ->
            val student = requireNotNull(studentById[studentId])
            val (status, notes) = parsedStatusByEntry[studentId] ?: (AttendanceStatus.ABSENT to null)
            AttendanceRecord(
                lectureSession = session,
                student = student,
                status = status,
                method = requestedMethod,
                confidenceScore = confidenceByStudent[studentId],
                markedAt = now,
                markedByTeacher = markedByTeacher,
                notes = notes
            )
        }

        // 10. Explicit flush, then recompute from the stored records -- never an increment.
        try {
            attendanceRecordRepository.saveAllAndFlush(records)
        } catch (e: DataIntegrityViolationException) {
            // R6.10: the unique (lecture_session_id, student_id) constraint is the final
            // backstop; it must surface as 409, not the generic 500 GlobalExceptionHandler
            // would otherwise produce for an unhandled DataIntegrityViolationException.
            throw ConflictException(
                "Attendance for lecture session $sessionId has already been recorded for one or more students"
            )
        }
        val counters = sessionCounterRecalculator.recompute(sessionId)

        // 11. R5.13, R5.14: finalize, carrying the just-recomputed counters forward so this
        // save cannot clobber what recompute() already persisted.
        val completedAt = LocalDateTime.now(clock)
        val finalized = session.copy(
            status = SessionStatus.FINALIZED,
            attendanceMethod = requestedMethod,
            completedAt = completedAt,
            presentCount = counters.present,
            absentCount = counters.absent,
            onLeaveCount = counters.onLeave,
            updatedAt = completedAt
        )
        lectureSessionRepository.save(finalized)

        // 12. Build, serialize and persist the submission record for future replay.
        val response = AttendanceConfirmationResponse(
            sessionId = sessionId,
            submissionId = request.submissionId,
            sessionStatus = SessionStatus.FINALIZED,
            totalMarked = counters.total,
            presentCount = counters.present,
            absentCount = counters.absent,
            onLeaveCount = counters.onLeave,
            defaultedStudentIds = defaultedStudentIds,
            confirmedAt = completedAt,
            replayed = false
        )

        val submission = AttendanceSubmission(
            submissionId = submissionId,
            lectureSessionId = sessionId,
            actingUserId = caller.userId,
            requestFingerprint = fingerprint,
            confirmationJson = objectMapper.writeValueAsString(response),
            createdAt = now
        )
        try {
            attendanceSubmissionRepository.save(submission)
        } catch (e: DataIntegrityViolationException) {
            // R6.12: the submissionId primary key is the first guard; this catch keeps a
            // genuine race on that key from surfacing as 500 as well.
            throw ConflictException("submissionId ${request.submissionId} was already used")
        }

        // Trigger push notification to enrolled students
        try {
            if (notificationPushService != null) {
                val subjectName = session.classSection?.subject?.name ?: "Class"
                val activeEnrollments = enrollmentRepository.findByClassSectionIdAndStatus(
                    session.classSection!!.id!!,
                    EnrollmentStatus.ACTIVE
                )
                val studentUserIdMap = activeEnrollments.mapNotNull { enrollment ->
                    val sId = enrollment.student?.id
                    val uId = enrollment.student?.user?.id
                    if (sId != null && uId != null) sId to uId else null
                }.toMap()

                request.entries.forEach { entry ->
                    val studentUserId = studentUserIdMap[entry.studentId]
                    if (studentUserId != null) {
                        notificationPushService.sendToUser(
                            userId = studentUserId,
                            payload = PushNotificationPayload(
                                title = "Attendance: $subjectName",
                                body = "Your attendance for ${session.sessionDate} was marked as ${entry.status}.",
                                channelId = "channel_attendance",
                                data = mapOf(
                                    "type" to "ATTENDANCE_CONFIRMED",
                                    "sessionId" to sessionId.toString(),
                                    "status" to entry.status
                                )
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Notification dispatch errors should not fail attendance confirmation
        }

        return response
    }
}