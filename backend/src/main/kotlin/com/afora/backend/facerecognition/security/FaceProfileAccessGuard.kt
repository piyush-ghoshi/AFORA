package com.afora.backend.facerecognition.security

import com.afora.backend.academic.repository.ClassSectionRepository
import com.afora.backend.attendance.security.CallerContext
import com.afora.backend.common.exception.ForbiddenException
import com.afora.backend.common.exception.ResourceNotFoundException
import com.afora.backend.common.exception.UnauthorizedException
import com.afora.backend.user.entity.UserRole
import com.afora.backend.user.repository.StudentRepository
import com.afora.backend.user.repository.TeacherRepository
import com.afora.backend.user.repository.UserRepository
import org.springframework.stereotype.Component

/**
 * Owns Requirement 11 for every face-profile and embedding-distribution
 * operation defined by this feature.
 *
 * Settled Decision D (face-recognition design): [requireCaller] is a
 * **deliberate duplication**, not a reuse, of
 * [com.afora.backend.attendance.security.AttendanceAccessGuard.requireCaller].
 * The UID-to-[CallerContext] resolution is identical with exactly one rule
 * inverted — STUDENT is permitted here, where the sibling guard rejects it —
 * because a student is the primary caller for consent, registration and
 * self-deletion (requirements 1, 2 and 4). Teaching `AttendanceAccessGuard`
 * a face-recognition-specific exception to its STUDENT rule would couple an
 * attendance-marking-owned file to this feature's role rules, which the
 * single-owner-file discipline that spec itself establishes rules out.
 *
 * The only type actually shared with `AttendanceAccessGuard` is the
 * [CallerContext] data class it is imported from — never the resolution
 * method, and this class never calls into `AttendanceAccessGuard` itself.
 *
 * Known, accepted cost of this duplication: a future bug fix to
 * `AttendanceAccessGuard.requireCaller()`'s resolution logic will not
 * automatically apply here. A fix to one should prompt a check of the other.
 */
@Component
class FaceProfileAccessGuard(
    private val userRepository: UserRepository,
    private val studentRepository: StudentRepository,
    private val teacherRepository: TeacherRepository,
    private val classSectionRepository: ClassSectionRepository
) {

    /**
     * R11.1, R11.2.
     *
     * - A null [firebaseUid] (no verified Firebase identifier) is unauthorized.
     * - An unknown [firebaseUid] (no matching `users` row) is unauthorized.
     * - STUDENT is permitted (the one inversion vs. AttendanceAccessGuard).
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
            UserRole.STUDENT -> CallerContext(userId = userId, role = UserRole.STUDENT, teacherId = null)
            UserRole.TEACHER -> {
                val teacherId = teacherRepository.findByUserId(userId)
                    .orElseThrow { ForbiddenException("No teacher profile for this account") }
                    .id
                CallerContext(userId = userId, role = UserRole.TEACHER, teacherId = teacherId)
            }
        }
    }

    /**
     * R11.3, R11.4.
     *
     * A STUDENT caller may act only on their own resolved student identifier.
     * ADMIN is unrestricted. (TEACHER never reaches this check in this
     * feature's endpoints; any TEACHER caller passed in is rejected the same
     * as any other mismatch, since a TEACHER's own student identifier does
     * not exist.)
     */
    fun requireOwnOrAdminStudentId(caller: CallerContext, pathStudentId: Long): Long {
        if (caller.role == UserRole.ADMIN) {
            return pathStudentId
        }

        val ownStudentId = studentRepository.findByUserId(caller.userId)
            .orElseThrow { ForbiddenException("No student profile for this account") }
            .id

        if (ownStudentId != pathStudentId) {
            throw ForbiddenException("Cannot act on another student's face profile")
        }

        return pathStudentId
    }

    /**
     * R5.5, R5.6, R5.7, R11.5, R11.6, R11.7.
     *
     * - STUDENT is always forbidden from embedding-distribution access (R5.7, R11.7).
     * - TEACHER must own the Class_Section (R5.5, R11.5).
     * - ADMIN is unrestricted (R5.6, R11.6).
     */
    fun requireClassSectionAccess(caller: CallerContext, classSectionId: Long) {
        if (caller.role == UserRole.STUDENT) {
            throw ForbiddenException("Students may not access face-embedding distribution")
        }

        if (caller.role == UserRole.ADMIN) {
            return
        }

        val classSection = classSectionRepository.findById(classSectionId)
            .orElseThrow { ResourceNotFoundException("Class section $classSectionId not found") }

        val owningTeacherId = classSection.teacher?.id
        if (owningTeacherId == null || owningTeacherId != caller.teacherId) {
            throw ForbiddenException("Cannot access another teacher's class section")
        }
    }
}
