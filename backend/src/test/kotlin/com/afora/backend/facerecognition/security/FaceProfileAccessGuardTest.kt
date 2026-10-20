package com.afora.backend.facerecognition.security

import com.afora.backend.academic.entity.ClassSection
import com.afora.backend.academic.repository.ClassSectionRepository
import com.afora.backend.attendance.security.CallerContext
import com.afora.backend.common.exception.ForbiddenException
import com.afora.backend.common.exception.UnauthorizedException
import com.afora.backend.user.entity.Student
import com.afora.backend.user.entity.Teacher
import com.afora.backend.user.entity.User
import com.afora.backend.user.entity.UserRole
import com.afora.backend.user.repository.StudentRepository
import com.afora.backend.user.repository.TeacherRepository
import com.afora.backend.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Optional
import kotlin.test.assertEquals

/**
 * Task 7.2 (design.md Settled Decision D, "How this is tested"): one test per bullet in
 * that section's own list -- null UID, unknown UID, STUDENT acting on someone else's
 * studentId, STUDENT calling the embeddings endpoint, TEACHER on a class section they do
 * not own, and ADMIN bypassing every check.
 *
 * **Known-and-accepted duplication risk, stated here per Decision D's own instruction:**
 * [FaceProfileAccessGuard.requireCaller] is a deliberate *duplicate* of
 * `AttendanceAccessGuard.requireCaller()`'s UID-to-[CallerContext] resolution logic, with
 * exactly one rule inverted (STUDENT is permitted here, rejected there). Because this is a
 * duplication rather than a shared/reused implementation, a future bug fix applied to one
 * guard's UID-resolution logic could silently fail to be applied to the other. A future
 * reader fixing a bug in `AttendanceAccessGuard.requireCaller()` should check whether
 * `FaceProfileAccessGuard.requireCaller()` needs the identical fix, and vice versa.
 *
 * Plain MockK-based unit tests only -- per Decision D, `FaceProfileAccessGuard` is a plain
 * `@Component` with no transactional or locking concerns, so no `@DataJpaTest` or Spring
 * context is needed here.
 */
class FaceProfileAccessGuardTest {

    private fun buildGuard(
        userRepository: UserRepository = mockk(),
        studentRepository: StudentRepository = mockk(),
        teacherRepository: TeacherRepository = mockk(),
        classSectionRepository: ClassSectionRepository = mockk()
    ) = FaceProfileAccessGuard(
        userRepository = userRepository,
        studentRepository = studentRepository,
        teacherRepository = teacherRepository,
        classSectionRepository = classSectionRepository
    )

    @Test
    fun `null Firebase UID is unauthorized`() {
        val userRepository = mockk<UserRepository>()
        val guard = buildGuard(userRepository = userRepository)

        assertThrows<UnauthorizedException> { guard.requireCaller(null) }

        verify(exactly = 0) { userRepository.findByFirebaseUid(any()) }
    }

    @Test
    fun `unknown Firebase UID is unauthorized`() {
        val userRepository = mockk<UserRepository>()
        every { userRepository.findByFirebaseUid("unknown-uid") } returns Optional.empty()
        val guard = buildGuard(userRepository = userRepository)

        assertThrows<UnauthorizedException> { guard.requireCaller("unknown-uid") }
    }

    @Test
    fun `STUDENT acting on someone else's studentId is forbidden`() {
        val studentRepository = mockk<StudentRepository>()
        val callerUser = User(
            id = 1L,
            firebaseUid = "student-uid",
            email = "student@afora.edu",
            firstName = "Stu",
            lastName = "Dent",
            role = UserRole.STUDENT
        )
        val ownStudent = Student(id = 5L, user = callerUser, rollNumber = "R1", department = "CS", batch = "2025")
        every { studentRepository.findByUserId(1L) } returns Optional.of(ownStudent)
        val guard = buildGuard(studentRepository = studentRepository)

        val caller = CallerContext(userId = 1L, role = UserRole.STUDENT, teacherId = null)

        assertThrows<ForbiddenException> {
            guard.requireOwnOrAdminStudentId(caller, pathStudentId = 999L)
        }
    }

    @Test
    fun `STUDENT calling the embeddings endpoint is always forbidden`() {
        val classSectionRepository = mockk<ClassSectionRepository>()
        val guard = buildGuard(classSectionRepository = classSectionRepository)

        val caller = CallerContext(userId = 1L, role = UserRole.STUDENT, teacherId = null)

        assertThrows<ForbiddenException> {
            guard.requireClassSectionAccess(caller, classSectionId = 10L)
        }

        verify(exactly = 0) { classSectionRepository.findById(any()) }
    }

    @Test
    fun `TEACHER on a class section they do not own is forbidden`() {
        val classSectionRepository = mockk<ClassSectionRepository>()
        val otherTeacherUser = User(
            id = 50L,
            firebaseUid = "other-teacher-uid",
            email = "other@afora.edu",
            firstName = "Other",
            lastName = "Teacher",
            role = UserRole.TEACHER
        )
        val owningTeacher = Teacher(id = 99L, user = otherTeacherUser, employeeId = "E99", department = "CS")
        val section = ClassSection(id = 10L, teacher = owningTeacher)
        every { classSectionRepository.findById(10L) } returns Optional.of(section)
        val guard = buildGuard(classSectionRepository = classSectionRepository)

        // caller is a different teacher (teacherId = 20L), not the section's owner (99L)
        val caller = CallerContext(userId = 2L, role = UserRole.TEACHER, teacherId = 20L)

        assertThrows<ForbiddenException> {
            guard.requireClassSectionAccess(caller, classSectionId = 10L)
        }
    }

    @Test
    fun `ADMIN bypasses every check`() {
        val studentRepository = mockk<StudentRepository>()
        val classSectionRepository = mockk<ClassSectionRepository>()
        val guard = buildGuard(
            studentRepository = studentRepository,
            classSectionRepository = classSectionRepository
        )

        val caller = CallerContext(userId = 3L, role = UserRole.ADMIN, teacherId = null)

        val resolvedStudentId = guard.requireOwnOrAdminStudentId(caller, pathStudentId = 555L)
        assertEquals(555L, resolvedStudentId)
        verify(exactly = 0) { studentRepository.findByUserId(any()) }

        guard.requireClassSectionAccess(caller, classSectionId = 10L)
        verify(exactly = 0) { classSectionRepository.findById(any()) }
    }
}
