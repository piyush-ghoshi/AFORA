package com.afora.backend.attendance.service

import com.afora.backend.academic.repository.ClassSectionRepository
import com.afora.backend.attendance.dto.LeaveClassOptionResponse
import com.afora.backend.attendance.dto.LeaveRequestResponse
import com.afora.backend.attendance.dto.SubmitLeaveRequestDto
import com.afora.backend.attendance.entity.LeaveRequest
import com.afora.backend.attendance.entity.LeaveStatus
import com.afora.backend.attendance.repository.LeaveRequestRepository
import com.afora.backend.attendance.security.LeaveAccessGuard
import com.afora.backend.common.exception.BadRequestException
import com.afora.backend.common.exception.ConflictException
import com.afora.backend.common.exception.ResourceNotFoundException
import com.afora.backend.user.entity.Teacher
import com.afora.backend.user.repository.StudentRepository
import com.afora.backend.notification.dto.PushNotificationPayload
import com.afora.backend.notification.service.NotificationPushService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class LeaveService(
    private val leaveRequestRepository: LeaveRequestRepository,
    private val studentRepository: StudentRepository,
    private val classSectionRepository: ClassSectionRepository,
    private val clock: Clock,
    @Autowired(required = false)
    private val notificationPushService: NotificationPushService? = null
) {

    /**
     * Submit a new leave request for [studentId].
     *
     * - `toDate` must not be before `fromDate` (the entity's own DB-level
     *   check constraint `chk_leave_date_order` enforces this too, but a
     *   service-level check yields a 400 with a clear message instead of a
     *   500 from a constraint violation).
     * - `reason` must not be blank.
     * - A non-null `classSectionId` must reference a class section that
     *   exists.
     * - Always created as PENDING.
     */
    @Transactional
    fun submit(studentId: Long, request: SubmitLeaveRequestDto): LeaveRequestResponse {
        if (request.toDate.isBefore(request.fromDate)) {
            throw BadRequestException("toDate (${request.toDate}) must not be before fromDate (${request.fromDate})")
        }
        if (request.reason.isBlank()) {
            throw BadRequestException("reason must not be blank")
        }

        val student = studentRepository.findById(studentId)
            .orElseThrow { ResourceNotFoundException("Student $studentId not found") }

        val section = request.classSectionId?.let { sectionId ->
            classSectionRepository.findById(sectionId)
                .orElseThrow { ResourceNotFoundException("Class section $sectionId not found") }
        }

        val now = LocalDateTime.now(clock)
        val saved = leaveRequestRepository.save(
            LeaveRequest(
                student = student,
                classSection = section,
                fromDate = request.fromDate,
                toDate = request.toDate,
                reason = request.reason,
                status = LeaveStatus.PENDING,
                createdAt = now,
                updatedAt = now
            )
        )
        return saved.toResponse()
    }

    /** The student's active classes, for the leave-scope picker. Ordered by subject code. */
    @Transactional(readOnly = true)
    fun listClassOptions(studentId: Long): List<LeaveClassOptionResponse> =
        classSectionRepository.findActiveByStudentId(studentId)
            .mapNotNull { section ->
                val subject = section.subject ?: return@mapNotNull null
                LeaveClassOptionResponse(
                    classSectionId = section.id,
                    subjectCode = subject.code,
                    subjectName = subject.name,
                    sectionName = section.name
                )
            }
            .sortedBy { it.subjectCode }

    /** A single leave request by id, with no ownership filtering -- the caller already checked. */
    @Transactional(readOnly = true)
    fun get(leaveRequestId: Long): LeaveRequestResponse {
        val found = leaveRequestRepository.findById(leaveRequestId)
            .orElseThrow { ResourceNotFoundException("Leave request $leaveRequestId not found") }
        return found.toResponse()
    }

    /** A student's own requests, optionally filtered to one status. */
    @Transactional(readOnly = true)
    fun listForStudent(studentId: Long, status: LeaveStatus?): List<LeaveRequestResponse> {
        val requests = if (status != null) {
            leaveRequestRepository.findByStudentIdAndStatus(studentId, status)
        } else {
            leaveRequestRepository.findByStudentId(studentId)
        }
        return requests.map { it.toResponse() }
    }

    /** A teacher's PENDING review queue, oldest first. */
    @Transactional(readOnly = true)
    fun listPendingForTeacher(teacherId: Long): List<LeaveRequestResponse> =
        leaveRequestRepository.findPendingForTeacher(teacherId).map { it.toResponse() }

    /**
     * Approve a PENDING request. Only a PENDING request may be approved --
     * re-approving an already-decided request is a conflict, not a silent
     * no-op, because leave review is a one-time decision.
     */
    @Transactional
    fun approve(leaveRequestId: Long, reviewingTeacher: Teacher?, notes: String?): LeaveRequestResponse =
        review(leaveRequestId, LeaveStatus.APPROVED, reviewingTeacher, notes)

    /** Reject a PENDING request. `notes` is required (validated non-blank) so the student knows why. */
    @Transactional
    fun reject(leaveRequestId: Long, reviewingTeacher: Teacher?, notes: String?): LeaveRequestResponse {
        if (notes.isNullOrBlank()) {
            throw BadRequestException("notes is required when rejecting a leave request")
        }
        return review(leaveRequestId, LeaveStatus.REJECTED, reviewingTeacher, notes)
    }

    private fun review(
        leaveRequestId: Long,
        newStatus: LeaveStatus,
        reviewingTeacher: Teacher?,
        notes: String?
    ): LeaveRequestResponse {
        val existing = leaveRequestRepository.findById(leaveRequestId)
            .orElseThrow { ResourceNotFoundException("Leave request $leaveRequestId not found") }

        if (existing.status != LeaveStatus.PENDING) {
            throw ConflictException(
                "Leave request $leaveRequestId is already ${existing.status}; only a PENDING request may be reviewed"
            )
        }

        val now = LocalDateTime.now(clock)
        val reviewed = existing.copy(
            status = newStatus,
            reviewedByTeacher = reviewingTeacher,
            reviewedAt = now,
            reviewNotes = notes,
            updatedAt = now
        )
        val saved = leaveRequestRepository.save(reviewed)

        try {
            val studentUserId = existing.student?.user?.id
            if (studentUserId != null && notificationPushService != null) {
                notificationPushService.sendToUser(
                    userId = studentUserId,
                    payload = PushNotificationPayload(
                        title = "Leave Request $newStatus",
                        body = "Your leave request (${existing.fromDate} to ${existing.toDate}) has been $newStatus.",
                        channelId = "channel_leaves",
                        data = mapOf(
                            "type" to "LEAVE_STATUS",
                            "status" to newStatus.name,
                            "requestId" to leaveRequestId.toString()
                        )
                    )
                )
            }
        } catch (_: Exception) {
            // Push notification failure should not roll back the review transaction
        }

        return saved.toResponse()
    }

    /**
     * Cancel a request. Only a PENDING request may be cancelled -- an
     * already-approved or already-rejected request has been acted on and is
     * no longer the student's alone to withdraw.
     */
    @Transactional
    fun cancel(leaveRequestId: Long): LeaveRequestResponse {
        val existing = leaveRequestRepository.findById(leaveRequestId)
            .orElseThrow { ResourceNotFoundException("Leave request $leaveRequestId not found") }

        if (existing.status != LeaveStatus.PENDING) {
            throw ConflictException(
                "Leave request $leaveRequestId is already ${existing.status}; only a PENDING request may be cancelled"
            )
        }

        val cancelled = existing.copy(
            status = LeaveStatus.CANCELLED,
            updatedAt = LocalDateTime.now(clock)
        )
        return leaveRequestRepository.save(cancelled).toResponse()
    }

    private fun LeaveRequest.toResponse(): LeaveRequestResponse = LeaveRequestResponse(
        id = id,
        studentId = requireNotNull(student?.id) { "leave request $id has no student" },
        studentName = student?.user?.getFullName() ?: "",
        rollNumber = student?.rollNumber ?: "",
        classSectionId = classSection?.id,
        classSectionName = classSection?.name,
        fromDate = fromDate,
        toDate = toDate,
        reason = reason,
        status = status,
        reviewedByTeacherId = reviewedByTeacher?.id,
        reviewedByTeacherName = reviewedByTeacher?.user?.getFullName(),
        reviewedAt = reviewedAt,
        reviewNotes = reviewNotes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}