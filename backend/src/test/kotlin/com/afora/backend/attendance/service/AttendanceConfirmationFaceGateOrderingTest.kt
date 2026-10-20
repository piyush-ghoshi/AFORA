package com.afora.backend.attendance.service

import com.afora.backend.academic.entity.ClassSection
import com.afora.backend.academic.entity.Enrollment
import com.afora.backend.academic.entity.EnrollmentStatus
import com.afora.backend.academic.entity.LectureSession
import com.afora.backend.academic.entity.SessionStatus
import com.afora.backend.academic.entity.Subject
import com.afora.backend.academic.repository.EnrollmentRepository
import com.afora.backend.academic.repository.LectureSessionRepository
import com.afora.backend.attendance.dto.ConfirmAttendanceRequestDto
import com.afora.backend.attendance.dto.SubmittedAttendanceEntry
import com.afora.backend.attendance.repository.AttendanceRecordRepository
import com.afora.backend.attendance.repository.AttendanceSubmissionRepository
import com.afora.backend.attendance.security.CallerContext
import com.afora.backend.common.exception.ConflictException
import com.afora.backend.user.entity.Student
import com.afora.backend.user.entity.Teacher
import com.afora.backend.user.entity.User
import com.afora.backend.user.entity.UserRole
import com.afora.backend.user.repository.TeacherRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.transaction.TestTransaction
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests task 11.2 of `.kiro/specs/face-recognition/tasks.md`: the new step 7.5
 * (`FaceMatchReviewGate.require(request.entries)`, added by task 11.1) inside
 * `AttendanceConfirmationService.confirm()`, placed between the existing step 7
 * (the ACTIVE-status gate / replay-or-conflict resolution) and the existing step 8
 * (membership + completeness validation), per design.md's Settled Decision B.
 *
 * Harness choice: a real `@DataJpaTest` against H2 with `AttendanceConfirmationService`
 * instantiated directly from the `@DataJpaTest`-autowired repository beans (plus a real
 * [SessionCounterRecalculator], [ObjectMapper] and [Clock]) -- the same technique
 * `FaceProfileServiceDeletionAtomicityTest` (task 6.3) uses for `FaceProfileService`.
 * MockK repository mocks are deliberately not used here: this task is specifically about
 * proving the gate runs inside the same transaction and behind the same pessimistic lock
 * (`findByIdForUpdate`, step 1) as the rest of `confirm()`, which a mocked repository
 * cannot exercise -- `findByIdForUpdate` is a `@Lock(PESSIMISTIC_WRITE)` query that must
 * run for real against a real database to prove anything.
 *
 * No dedicated confirm-flow test class existed in the repository before this task;
 * `StudentAttendanceServiceTest.kt` and `LeaveServiceTest.kt` (the only two existing files
 * in this package) never call `confirm()`. `AttendanceConfirmationService` is not pulled
 * in as a Spring bean here, so its `@Transactional` annotation itself has no AOP effect in
 * this test -- the pessimistic-lock query and the gate check still execute against the
 * real database inside the one open test transaction (or, for the third test below, a
 * genuinely committed/rolled-back transaction pair), which is what each assertion below
 * actually needs.
 */
@DataJpaTest
@ActiveProfiles("test")
class AttendanceConfirmationFaceGateOrderingTest {

    @Autowired private lateinit var em: TestEntityManager
    @Autowired private lateinit var lectureSessionRepository: LectureSessionRepository
    @Autowired private lateinit var enrollmentRepository: EnrollmentRepository
    @Autowired private lateinit var attendanceRecordRepository: AttendanceRecordRepository
    @Autowired private lateinit var attendanceSubmissionRepository: AttendanceSubmissionRepository
    @Autowired private lateinit var teacherRepository: TeacherRepository

    private val clock: Clock = Clock.systemDefaultZone()

    private val objectMapper: ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    private fun service() = AttendanceConfirmationService(
        lectureSessionRepository,
        enrollmentRepository,
        attendanceRecordRepository,
        attendanceSubmissionRepository,
        teacherRepository,
        SessionCounterRecalculator(attendanceRecordRepository, lectureSessionRepository, clock),
        objectMapper,
        clock,
        null
    )

    /** One teacher, one student, one ACTIVE class section and one ACTIVE lecture session. */
    private data class Fixture(
        val sessionId: Long,
        val classSectionId: Long,
        val subjectId: Long,
        val studentId: Long,
        val studentUserId: Long,
        val teacherId: Long,
        val teacherUserId: Long
    )

    private fun seedFixture(suffix: String): Fixture {
        val tUser = em.persistAndFlush(
            User(
                firebaseUid = "gate_teacher_$suffix",
                email = "gate_teacher_$suffix@afora.edu",
                firstName = "Gate",
                lastName = "Teacher",
                role = UserRole.TEACHER
            )
        )
        val teacher = em.persistAndFlush(
            Teacher(user = tUser, employeeId = "EMPGATE$suffix", department = "CS")
        )

        val sUser = em.persistAndFlush(
            User(
                firebaseUid = "gate_student_$suffix",
                email = "gate_student_$suffix@afora.edu",
                firstName = "Gate",
                lastName = "Student",
                role = UserRole.STUDENT
            )
        )
        val student = em.persistAndFlush(
            Student(user = sUser, rollNumber = "GATE$suffix", department = "CS", batch = "2025")
        )

        val subject = em.persistAndFlush(
            Subject(code = "GATE$suffix", name = "Gate Ordering $suffix", department = "CS")
        )
        val section = em.persistAndFlush(
            ClassSection(
                name = "Gate Section $suffix",
                subject = subject,
                teacher = teacher,
                semesterId = UUID.randomUUID(),
                batch = "2025",
                section = "A",
                roomNumber = "R-$suffix"
            )
        )
        em.persistAndFlush(
            Enrollment(student = student, classSection = section, status = EnrollmentStatus.ACTIVE)
        )
        val session = em.persistAndFlush(
            LectureSession(
                classSection = section,
                semesterId = section.semesterId,
                sessionDate = LocalDate.of(2026, 11, 1),
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(10, 0),
                status = SessionStatus.ACTIVE
            )
        )
        em.flush()
        em.clear()

        return Fixture(
            sessionId = session.id,
            classSectionId = section.id,
            subjectId = subject.id,
            studentId = student.id!!,
            studentUserId = sUser.id!!,
            teacherId = teacher.id!!,
            teacherUserId = tUser.id!!
        )
    }

    private fun callerFor(fx: Fixture) =
        CallerContext(userId = fx.teacherUserId, role = UserRole.TEACHER, teacherId = fx.teacherId)

    /** Removes everything [seedFixture] created, in FK-safe order. */
    private fun cleanUp(fx: Fixture) {
        em.entityManager.createQuery("DELETE FROM AttendanceSubmission s WHERE s.lectureSessionId = :id")
            .setParameter("id", fx.sessionId)
            .executeUpdate()
        em.entityManager.createQuery("DELETE FROM AttendanceRecord r WHERE r.lectureSession.id = :id")
            .setParameter("id", fx.sessionId)
            .executeUpdate()
        em.entityManager.createQuery("DELETE FROM LectureSession ls WHERE ls.id = :id")
            .setParameter("id", fx.sessionId)
            .executeUpdate()
        em.entityManager.createQuery("DELETE FROM Enrollment e WHERE e.classSection.id = :id")
            .setParameter("id", fx.classSectionId)
            .executeUpdate()
        em.entityManager.createQuery("DELETE FROM ClassSection cs WHERE cs.id = :id")
            .setParameter("id", fx.classSectionId)
            .executeUpdate()
        em.entityManager.createQuery("DELETE FROM Subject s WHERE s.id = :id")
            .setParameter("id", fx.subjectId)
            .executeUpdate()
        em.entityManager.createQuery("DELETE FROM Student s WHERE s.id = :id")
            .setParameter("id", fx.studentId)
            .executeUpdate()
        em.entityManager.createQuery("DELETE FROM Teacher t WHERE t.id = :id")
            .setParameter("id", fx.teacherId)
            .executeUpdate()
        em.entityManager.createQuery("DELETE FROM User u WHERE u.id = :id")
            .setParameter("id", fx.studentUserId)
            .executeUpdate()
        em.entityManager.createQuery("DELETE FROM User u WHERE u.id = :id")
            .setParameter("id", fx.teacherUserId)
            .executeUpdate()
        em.flush()
    }

    @Test
    fun `an unresolved MEDIUM entry is rejected with 409 and writes nothing`() {
        val fx = seedFixture("rej")
        val request = ConfirmAttendanceRequestDto(
            submissionId = UUID.randomUUID().toString(),
            markUnspecifiedAsAbsent = true,
            // 0.70 sits in [MEDIUM_CONFIDENCE, HIGH_CONFIDENCE) with teacherConfirmed = false,
            // i.e. an unresolved MEDIUM match (R8.1, R8.2).
            entries = listOf(
                SubmittedAttendanceEntry(
                    studentId = fx.studentId,
                    status = "PRESENT",
                    confidenceScore = 0.70,
                    teacherConfirmed = false
                )
            ),
            method = "CAMERA"
        )

        assertThrows<ConflictException> {
            service().confirm(fx.sessionId, callerFor(fx), request)
        }

        assertTrue(
            attendanceRecordRepository.findByLectureSessionId(fx.sessionId).isEmpty(),
            "the gate rejects before step 9 builds any AttendanceRecord"
        )
        assertTrue(
            attendanceSubmissionRepository.findById(UUID.fromString(request.submissionId)).isEmpty,
            "the gate rejects before step 12 persists the AttendanceSubmission"
        )
        val sessionAfter = lectureSessionRepository.findById(fx.sessionId).orElseThrow()
        assertEquals(
            SessionStatus.ACTIVE,
            sessionAfter.status,
            "a rejected confirmation must not finalize the Lecture_Session"
        )
    }

    @Test
    fun `a replay of an already-accepted confirmation returns the stored response without re-running the gate`() {
        val fx = seedFixture("replay")
        val svc = service()
        val submissionId = UUID.randomUUID().toString()

        // First call: confidenceScore 0.70 is MEDIUM, but teacherConfirmed = true, so the
        // gate passes (R8.2) and the confirmation succeeds and finalizes the session.
        val firstRequest = ConfirmAttendanceRequestDto(
            submissionId = submissionId,
            markUnspecifiedAsAbsent = true,
            entries = listOf(
                SubmittedAttendanceEntry(
                    studentId = fx.studentId,
                    status = "PRESENT",
                    confidenceScore = 0.70,
                    teacherConfirmed = true
                )
            ),
            method = "CAMERA"
        )
        val firstResponse = svc.confirm(fx.sessionId, callerFor(fx), firstRequest)
        assertFalse(firstResponse.replayed)
        assertEquals(SessionStatus.FINALIZED, firstResponse.sessionStatus)

        // Second call: identical submissionId, identical status/notes per student (so
        // RequestFingerprint.of -- which covers only sessionId/studentId/status/notes, not
        // confidenceScore or teacherConfirmed -- computes the SAME fingerprint), but this
        // time teacherConfirmed = false. If FaceMatchReviewGate.require(...) ran again on
        // this payload it would throw ConflictException (same 0.70 score, now unresolved).
        // The replay path (existing step 6) must return the stored response before step 7.5
        // is ever reached, so this call must succeed.
        val secondRequest = ConfirmAttendanceRequestDto(
            submissionId = submissionId,
            markUnspecifiedAsAbsent = true,
            entries = listOf(
                SubmittedAttendanceEntry(
                    studentId = fx.studentId,
                    status = "PRESENT",
                    confidenceScore = 0.70,
                    teacherConfirmed = false
                )
            ),
            method = "CAMERA"
        )
        val secondResponse = svc.confirm(fx.sessionId, callerFor(fx), secondRequest)

        assertTrue(secondResponse.replayed, "a fingerprint-identical resubmission must be a replay")
        assertEquals(firstResponse.sessionId, secondResponse.sessionId)
        assertEquals(firstResponse.totalMarked, secondResponse.totalMarked)
        assertEquals(firstResponse.presentCount, secondResponse.presentCount)
        assertEquals(firstResponse.absentCount, secondResponse.absentCount)
        assertEquals(firstResponse.onLeaveCount, secondResponse.onLeaveCount)
        assertEquals(firstResponse.confirmedAt, secondResponse.confirmedAt)

        // Exactly one AttendanceRecord exists -- the replay did not write a second one.
        assertEquals(1, attendanceRecordRepository.findByLectureSessionId(fx.sessionId).size)
    }

    @Test
    fun `the gate runs behind the same pessimistic lock and a rejection leaves no attendance record in the database`() {
        val fx = seedFixture("lock")

        // Commit the seed data on its own, into a fresh transaction, before the call under
        // test runs. The rollback-proof below must hit the real database to mean anything;
        // otherwise it would only reflect this test's own still-open transaction, not a
        // genuine absence of persisted rows (same technique as
        // FaceProfileServiceDeletionAtomicityTest's induced-failure test).
        TestTransaction.flagForCommit()
        TestTransaction.end()
        TestTransaction.start()

        try {
            val request = ConfirmAttendanceRequestDto(
                submissionId = UUID.randomUUID().toString(),
                markUnspecifiedAsAbsent = true,
                entries = listOf(
                    SubmittedAttendanceEntry(
                        studentId = fx.studentId,
                        status = "PRESENT",
                        confidenceScore = 0.65,
                        teacherConfirmed = false
                    )
                ),
                method = "CAMERA"
            )

            // confirm()'s first statement is findByIdForUpdate -- a real
            // @Lock(PESSIMISTIC_WRITE) query against H2, not a mock -- and the gate check
            // (step 7.5) runs later in the very same method call, before any write. This
            // call therefore exercises the lock-then-gate ordering for real.
            assertThrows<ConflictException> {
                service().confirm(fx.sessionId, callerFor(fx), request)
            }

            // Force a genuine rollback and a fresh persistence context before re-reading,
            // so the assertions below reflect the database, not Hibernate's in-memory
            // session for this test method.
            TestTransaction.flagForRollback()
            TestTransaction.end()
            TestTransaction.start()

            assertTrue(
                attendanceRecordRepository.findByLectureSessionId(fx.sessionId).isEmpty(),
                "no AttendanceRecord may survive a rejection inside the locked confirm() transaction"
            )
            assertTrue(
                attendanceSubmissionRepository.findById(UUID.fromString(request.submissionId)).isEmpty,
                "no AttendanceSubmission may survive a rejection inside the locked confirm() transaction"
            )
            val sessionAfter = lectureSessionRepository.findById(fx.sessionId).orElseThrow()
            assertEquals(
                SessionStatus.ACTIVE,
                sessionAfter.status,
                "the Lecture_Session must remain ACTIVE, never finalized, after a gate rejection"
            )
        } finally {
            // The seed data was committed above, outside the normal @DataJpaTest per-test
            // rollback, specifically so the rollback-proof above is genuine. Clean it back
            // up here, in its own committed transaction, so it leaves no residue for any
            // later test method reusing the same H2 database/Spring context.
            cleanUp(fx)
            TestTransaction.flagForCommit()
            TestTransaction.end()
            TestTransaction.start()
        }
    }
}
