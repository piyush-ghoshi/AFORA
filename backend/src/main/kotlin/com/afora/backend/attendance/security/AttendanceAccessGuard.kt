package com.afora.backend.attendance.security

import com.afora.backend.academic.entity.LectureSession
import com.afora.backend.academic.repository.LectureSessionRepository
import com.afora.backend.common.exception.ForbiddenException
import com.afora.backend.common.exception.ResourceNotFoundException
import com.afora.backend.common.exception.UnauthorizedException
import com.afora.backend.user.entity.UserRole
import com.afora.backend.user.repository.StudentRepository
import com.afora.backend.user.repository.TeacherRepository
import com.afora.backend.user.repository.UserRepository
import org.springframework.stereotype.Component

/**
 * Owns Requirement 10 entirely. Every one of the six attendance-marking
 * operations (roster read, activation, confirmation, cancellation, amendment
 * and audit-history) calls this guard, and nothing else, before delegating
 * to the Attendance_Service facade.
 *
 * Mirrors the ownership-authorization pattern already established in
 * [com.afora.backend.dashboard.controller.DashboardController]: resolve the
 * caller through [UserRepository.findByFirebaseUid], then resolve the
 * caller's own teacher identifier through [TeacherRepository.findByUserId],
 * then compare.
 */
@Component
class AttendanceAccessGuard(
    private val userRepository: UserRepository,
    private val studentRepository: StudentRepository,
    private val teacherRepository: TeacherRepository,
    private val lectureSessionRepository: LectureSessionRepository
) {

    /**
     * R10.1, R10.2, R10.3, R10.4, R10.5, R10.6.
     *
     * - A null [firebaseUid] (no verified Firebase identifier) is unauthorized.
     * - An unknown [firebaseUid] (no matching `users` row) is unauthorized.
     * - STUDENT is forbidden for every attendance-marking operation.
     * - TEACHER must own a `teachers` row for their own user id, or is forbidden.
     * - ADMIN is unrestricted and carries no teacher identifier.
     *
     * Note `.orElseThrow {}` — NOT elvis (`?:`). Elvis does not compile against
     * `Optional<User>` and has already produced one bug in this project.
     */
    fun requireCaller(firebaseUid: String?): CallerContext {
        val uid = firebaseUid ?: throw UnauthorizedException("Authentication required")
        val user = userRepository.findByFirebaseUid(uid)
            .orElseThrow { UnauthorizedException("User not found") }
        val userId = user.id ?: throw UnauthorizedException("User not found")

        return when (user.role) {
            UserRole.ADMIN -> CallerContext(userId = userId, role = UserRole.ADMIN, teacherId = null)
            UserRole.STUDENT -> throw ForbiddenException("Students may not perform attendance-marking operations")
            UserRole.TEACHER -> {
                val teacherId = teacherRepository.findByUserId(userId)
                    .orElseThrow { ForbiddenException("No teacher profile for this account") }
                    .id
                CallerContext(userId = userId, role = UserRole.TEACHER, teacherId = teacherId)
            }
        }
    }

    /**
     * R10.3, R10.7, R1.8.
     *
     * Loads the Lecture_Session, walks to its Class_Section, and compares the
     * owning teacher identifier against the caller's own teacher identifier.
     * ADMIN bypasses the comparison entirely.
     *
     * Ordering is normative: callers must invoke [requireCaller] before this
     * method, so an unauthenticated request against a nonexistent session
     * resolves to 401, never 404.
     */
    fun requireSessionAccess(caller: CallerContext, sessionId: Long): LectureSession {
        val session = lectureSessionRepository.findById(sessionId)
            .orElseThrow { ResourceNotFoundException("Lecture session $sessionId not found") }

        if (caller.role == UserRole.ADMIN) {
            return session
        }

        val owningTeacherId = session.classSection?.teacher?.id
        if (owningTeacherId == null || owningTeacherId != caller.teacherId) {
            throw ForbiddenException("Cannot act on another teacher's lecture session")
        }

        return session
    }
}

/**
 * The resolved identity of a caller who has passed [AttendanceAccessGuard.requireCaller].
 *
 * @property teacherId the caller's own `teacher.id`; null for ADMIN.
 */
data class CallerContext(
    val userId: Long,
    val role: UserRole,
    val teacherId: Long?
)
