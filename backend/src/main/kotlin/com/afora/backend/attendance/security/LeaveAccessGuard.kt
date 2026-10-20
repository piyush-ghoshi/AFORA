package com.afora.backend.attendance.security

import com.afora.backend.attendance.entity.LeaveRequest
import com.afora.backend.attendance.repository.LeaveRequestRepository
import com.afora.backend.common.exception.ForbiddenException
import com.afora.backend.common.exception.ResourceNotFoundException
import com.afora.backend.common.exception.UnauthorizedException
import com.afora.backend.user.entity.Teacher
import com.afora.backend.user.entity.User
import com.afora.backend.user.entity.UserRole
import com.afora.backend.user.repository.StudentRepository
import com.afora.backend.user.repository.TeacherRepository
import com.afora.backend.user.repository.UserRepository
import org.springframework.stereotype.Component

/**
 * Authorization for the Leave Requests feature.
 *
 * Mirrors the ownership pattern already established by
 * [com.afora.backend.attendance.security.AttendanceAccessGuard] and
 * [com.afora.backend.dashboard.controller.DashboardController]: resolve the
 * caller through [UserRepository.findByFirebaseUid], then compare their own
 * resolved student/teacher identifier against the resource being acted on.
 * ADMIN bypasses every ownership comparison.
 */
@Component
class LeaveAccessGuard(
    private val userRepository: UserRepository,
    private val studentRepository: StudentRepository,
    private val teacherRepository: TeacherRepository,
    private val leaveRequestRepository: LeaveRequestRepository
) {

    /** A null or unknown Firebase UID is unauthorized, never forbidden. */
    fun requireCaller(firebaseUid: String?): User {
        val uid = firebaseUid ?: throw UnauthorizedException("Authentication required")
        return userRepository.findByFirebaseUid(uid)
            .orElseThrow { UnauthorizedException("User not found") }
    }

    /** Submitting and listing one's own leave requests is a STUDENT-only action. */
    fun requireOwnStudentId(user: User): Long {
        if (user.role != UserRole.STUDENT) {
            throw ForbiddenException("Only a student may act on their own leave requests")
        }
        return studentRepository.findByUserId(requireNotNull(user.id))
            .orElseThrow { ForbiddenException("No student profile for this account") }
            .id!!
    }

    /**
     * A teacher may only read their own review queue. Admins are unrestricted.
     * Mirrors [com.afora.backend.dashboard.controller.DashboardController.authorizeTeacherAccess].
     */
    fun authorizeTeacherAccess(user: User, teacherId: Long) {
        if (user.role == UserRole.ADMIN) return
        if (user.role != UserRole.TEACHER) {
            throw ForbiddenException("Only the teacher or an admin may view this review queue")
        }
        val ownId = teacherRepository.findByUserId(requireNotNull(user.id))
            .orElseThrow { ForbiddenException("No teacher profile for this account") }
            .id
        if (ownId != teacherId) {
            throw ForbiddenException("Cannot view another teacher's review queue")
        }
    }

    /**
     * The resolved context for an approve/reject action: the loaded
     * [LeaveRequest], plus the [Teacher] to record as the reviewer. A TEACHER
     * caller always owns the request's class section at this point (checked
     * below), so that section's own teacher is the reviewer. An ADMIN caller
     * has no corresponding teacher, so [reviewingTeacher] is null -- the
     * entity's `reviewedByTeacher` column stays null for an admin review.
     */
    data class ReviewContext(val leaveRequest: LeaveRequest, val reviewingTeacher: Teacher?)

    /**
     * A TEACHER may review a request only if they own its class section. A
     * request with no class section (spans all of the student's classes) has
     * no single owning teacher, so only ADMIN may review it. STUDENT is
     * always forbidden.
     */
    fun requireReviewAccess(user: User, leaveRequestId: Long): ReviewContext {
        val leaveRequest = leaveRequestRepository.findById(leaveRequestId)
            .orElseThrow { ResourceNotFoundException("Leave request $leaveRequestId not found") }

        if (user.role == UserRole.ADMIN) {
            return ReviewContext(leaveRequest, reviewingTeacher = null)
        }
        if (user.role != UserRole.TEACHER) {
            throw ForbiddenException("Only a teacher or an admin may review a leave request")
        }

        val section = leaveRequest.classSection
            ?: throw ForbiddenException("Only an admin may review a leave request spanning all classes")

        val ownTeacherId = teacherRepository.findByUserId(requireNotNull(user.id))
            .orElseThrow { ForbiddenException("No teacher profile for this account") }
            .id
        if (section.teacher?.id != ownTeacherId) {
            throw ForbiddenException("Cannot review another teacher's leave request")
        }

        return ReviewContext(leaveRequest, reviewingTeacher = section.teacher)
    }

    /** A STUDENT may cancel only their own leave request. Admins are unrestricted. */
    fun requireCancelAccess(user: User, leaveRequestId: Long): LeaveRequest {
        val leaveRequest = leaveRequestRepository.findById(leaveRequestId)
            .orElseThrow { ResourceNotFoundException("Leave request $leaveRequestId not found") }

        if (user.role == UserRole.ADMIN) {
            return leaveRequest
        }
        if (user.role != UserRole.STUDENT) {
            throw ForbiddenException("Only the requesting student or an admin may cancel a leave request")
        }

        val ownStudentId = studentRepository.findByUserId(requireNotNull(user.id))
            .orElseThrow { ForbiddenException("No student profile for this account") }
            .id
        if (leaveRequest.student?.id != ownStudentId) {
            throw ForbiddenException("Cannot cancel another student's leave request")
        }

        return leaveRequest
    }
}