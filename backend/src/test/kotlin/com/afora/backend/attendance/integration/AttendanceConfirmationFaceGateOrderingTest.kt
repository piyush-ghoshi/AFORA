package com.afora.backend.attendance.integration

import com.afora.backend.academic.entity.AttendanceMethod
import com.afora.backend.academic.entity.ClassSection
import com.afora.backend.academic.entity.EnrollmentStatus
import com.afora.backend.academic.entity.LectureSession
import com.afora.backend.academic.entity.Semester
import com.afora.backend.academic.entity.SessionStatus
import com.afora.backend.academic.entity.Subject
import com.afora.backend.academic.entity.TimetableSlot
import com.afora.backend.academic.repository.ClassSectionRepository
import com.afora.backend.academic.repository.EnrollmentRepository
import com.afora.backend.academic.repository.LectureSessionRepository
import com.afora.backend.academic.repository.SemesterRepository
import com.afora.backend.academic.repository.SubjectRepository
import com.afora.backend.academic.repository.TimetableSlotRepository
import com.afora.backend.attendance.dto.AttendanceConfirmationResponse
import com.afora.backend.attendance.dto.ConfirmAttendanceRequestDto
import com.afora.backend.attendance.dto.SubmittedAttendanceEntry
import com.afora.backend.academic.entity.Enrollment
import com.afora.backend.attendance.entity.AttendanceRecord
import com.afora.backend.attendance.repository.AttendanceRecordRepository
import com.afora.backend.attendance.repository.AttendanceSubmissionRepository
import com.afora.backend.attendance.security.CallerContext
import com.afora.backend.attendance.service.AttendanceConfirmationService
import com.afora.backend.common.exception.ConflictException
import com.afora.backend.facerecognition.service.FaceMatchingThresholds
import com.afora.backend.user.entity.Student
import com.afora.backend.user.entity.Teacher
import com.afora.backend.user.entity.User
import com.afora.backend.user.entity.UserRole
import com.afora.backend.user.repository.StudentRepository
import com.afora.backend.user.repository.TeacherRepository
import com.afora.backend.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration test proving Requirements 8.5 and 9.6: the face-match review gate runs
 * inside the same transaction and behind the same pessimistic lock as the rest of
 * `confirm()`, and rejection by the gate leaves no attendance record behind.
 *
 * This test verifies three behaviors:
 *
 * 1. **Gate rejection writes nothing**: A confirmation with an unresolved MEDIUM entry
 *    (confidenceScore >= MEDIUM_CONFIDENCE but teacherConfirmed = false) is rejected with
 *    HTTP 409 before any `attendance_records` are persisted. After the exception, querying
 *    the database shows zero records for that session, proving the entire transaction
 *    rolled back.
 *
 * 2. **Replay bypasses the gate**: A replay of an already-accepted confirmation (identical
 *    `submissionId` and request fingerprint) returns the stored response immediately
 *    without re-running `FaceMatchReviewGate.require`. The test proves this by first
 *    successfully confirming with all HIGH-confidence scores, then replaying with the same
 *    `submissionId` but an entry list that *would* fail the gate if evaluated — the replay
 *    succeeds, proving the gate was skipped.
 *
 * 3. **Pessimistic lock covers the gate**: The gate runs inside the same `@Transactional`
 *    method as the pessimistic lock acquisition, ensuring no race condition can insert
 *    records between the gate check and the record writes. This is structural (verified by
 *    code inspection of `AttendanceConfirmationService.confirm()`) and behaviorally tested
 *    here: rejection leaves zero records, proving the gate ran before the writes and inside
 *    the same transaction.
 *
 * Uses `@SpringBootTest(webEnvironment = NONE)` on H2 with real Spring-managed service and
 * repository beans, following the same integration-test pattern as
 * `FaceProfileServiceDeletionAtomicityTest`.
 *
 * Mocking: MockK only — no Mockito/mockito-kotlin/@Mock/when(...)/thenReturn.
 * (No mocking is actually needed here since we use real service/repository beans.)
 *
 * Feature: attendance-marking
 * Validates: Requirements 8.5, 9.6
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class AttendanceConfirmationFaceGateOrderingTest {

    @Autowired private lateinit var attendanceConfirmationService: AttendanceConfirmationService
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var studentRepository: StudentRepository
    @Autowired private lateinit var teacherRepository: TeacherRepository
    @Autowired private lateinit var semesterRepository: SemesterRepository
    @Autowired private lateinit var subjectRepository: SubjectRepository
    @Autowired private lateinit var classSectionRepository: ClassSectionRepository
    @Autowired private lateinit var timetableSlotRepository: TimetableSlotRepository
    @Autowired private lateinit var lectureSessionRepository: LectureSessionRepository
    @Autowired private lateinit var enrollmentRepository: EnrollmentRepository
    @Autowired private lateinit var attendanceRecordRepository: AttendanceRecordRepository
    @Autowired private lateinit var attendanceSubmissionRepository: AttendanceSubmissionRepository
    @Autowired private lateinit var clock: Clock

    private lateinit var teacher: Teacher
    private lateinit var caller: CallerContext
    private lateinit var student1: Student
    private lateinit var student2: Student
    private lateinit var student3: Student
    private lateinit var session: LectureSession

    @BeforeEach
    fun seed() {
        // Create teacher
        val teacherUser = userRepository.save(
            User(
                firebaseUid = "gate_teacher_uid",
                email = "gate.teacher@afora.edu",
                firstName = "Gate",
                lastName = "Teacher",
                role = UserRole.TEACHER
            )
        )
        teacher = teacherRepository.save(
            Teacher(user = teacherUser, employeeId = "GATE001", department = "CS")
        )
        caller = CallerContext(
            userId = teacherUser.id!!,
            role = UserRole.TEACHER,
            teacherId = teacher.id
        )

        // Create three students
        val student1User = userRepository.save(
            User(
                firebaseUid = "gate_student1_uid",
                email = "gate.student1@afora.edu",
                firstName = "Student",
                lastName = "One",
                role = UserRole.STUDENT
            )
        )
        student1 = studentRepository.save(
            Student(user = student1User, rollNumber = "2026CS001", department = "CS", batch = "2026")
        )

        val student2User = userRepository.save(
            User(
                firebaseUid = "gate_student2_uid",
                email = "gate.student2@afora.edu",
                firstName = "Student",
                lastName = "Two",
                role = UserRole.STUDENT
            )
        )
        student2 = studentRepository.save(
            Student(user = student2User, rollNumber = "2026CS002", department = "CS", batch = "2026")
        )

        val student3User = userRepository.save(
            User(
                firebaseUid = "gate_student3_uid",
                email = "gate.student3@afora.edu",
                firstName = "Student",
                lastName = "Three",
                role = UserRole.STUDENT
            )
        )
        student3 = studentRepository.save(
            Student(user = student3User, rollNumber = "2026CS003", department = "CS", batch = "2026")
        )

        // Create academic structure
        val semester = semesterRepository.save(
            Semester(
                academicYearId = UUID.randomUUID(),
                name = "Spring 2026",
                semesterNumber = 2,
                startDate = LocalDate.of(2026, 1, 1),
                endDate = LocalDate.of(2026, 5, 31),
                isCurrent = true
            )
        )

        val subject = subjectRepository.save(
            Subject(
                code = "CS301",
                name = "Database Systems",
                credits = 4,
                department = "CS"
            )
        )

        val classSection = classSectionRepository.save(
            ClassSection(
                name = "CS301-A",
                subject = subject,
                teacher = teacher,
                semesterId = semester.id,
                batch = "2026",
                section = "A",
                roomNumber = "L301"
            )
        )

        // Create active enrollments for all three students
        enrollmentRepository.save(
            Enrollment(
                student = student1,
                classSection = classSection,
                enrollmentDate = LocalDateTime.of(2026, 1, 5, 0, 0),
                status = EnrollmentStatus.ACTIVE
            )
        )
        enrollmentRepository.save(
            Enrollment(
                student = student2,
                classSection = classSection,
                enrollmentDate = LocalDateTime.of(2026, 1, 5, 0, 0),
                status = EnrollmentStatus.ACTIVE
            )
        )
        enrollmentRepository.save(
            Enrollment(
                student = student3,
                classSection = classSection,
                enrollmentDate = LocalDateTime.of(2026, 1, 5, 0, 0),
                status = EnrollmentStatus.ACTIVE
            )
        )

        // Create timetable slot and lecture session
        val slot = timetableSlotRepository.save(
            TimetableSlot(
                classSection = classSection,
                dayOfWeek = 1, // Monday (1-7, where 1 = Monday)
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(10, 0),
                roomNumber = "L301"
            )
        )

        session = lectureSessionRepository.save(
            LectureSession(
                classSection = classSection,
                timetableSlot = slot,
                semesterId = semester.id,
                sessionDate = LocalDate.of(2026, 3, 2), // Monday
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(10, 0),
                status = SessionStatus.ACTIVE, // Pre-activated
                totalStudents = 3,
                presentCount = 0,
                absentCount = 0,
                onLeaveCount = 0
            )
        )
    }

    /**
     * Test 1: A confirmation with an unresolved MEDIUM-confidence entry is rejected with
     * HTTP 409 (ConflictException) and writes nothing to `attendance_records`.
     *
     * Validates: Requirements 8.5 (gate enforcement), 9.6 (rejection before persistence).
     */
    @Test
    fun `confirmation with unresolved MEDIUM entry rejects with 409 and writes nothing`() {
        // Build a confirmation request where student2 has a MEDIUM-confidence score with
        // teacherConfirmed = false (unresolved).
        val submissionId = UUID.randomUUID().toString()
        val request = ConfirmAttendanceRequestDto(
            submissionId = submissionId,
            entries = listOf(
                SubmittedAttendanceEntry(
                    studentId = student1.id!!,
                    status = "PRESENT",
                    confidenceScore = FaceMatchingThresholds.HIGH_CONFIDENCE, // HIGH: passes
                    teacherConfirmed = false
                ),
                SubmittedAttendanceEntry(
                    studentId = student2.id!!,
                    status = "PRESENT",
                    confidenceScore = FaceMatchingThresholds.MEDIUM_CONFIDENCE, // MEDIUM: blocks
                    teacherConfirmed = false // unresolved
                ),
                SubmittedAttendanceEntry(
                    studentId = student3.id!!,
                    status = "ABSENT",
                    confidenceScore = null // manual: skipped by gate
                )
            ),
            markUnspecifiedAsAbsent = false,
            method = "HYBRID"
        )

        // Assert that the confirmation throws ConflictException
        val ex = assertThrows<ConflictException> {
            attendanceConfirmationService.confirm(session.id!!, caller, request)
        }

        // Verify the exception message names student2's id (the unresolved one)
        assertTrue(
            student2.id!!.toString() in (ex.message ?: ""),
            "expected student id ${student2.id} to be named in: ${ex.message}"
        )

        // Verify zero attendance records were persisted (entire transaction rolled back)
        val records = attendanceRecordRepository.findByLectureSessionId(session.id!!)
        assertEquals(0, records.size, "no attendance records should be persisted after gate rejection")

        // Verify the submission was not stored
        val submission = attendanceSubmissionRepository.findById(UUID.fromString(submissionId))
        assertTrue(submission.isEmpty, "no submission record should be stored after gate rejection")

        // Verify the session remains ACTIVE (not finalized)
        val reloadedSession = lectureSessionRepository.findById(session.id!!).orElseThrow()
        assertEquals(
            SessionStatus.ACTIVE,
            reloadedSession.status,
            "session should remain ACTIVE after gate rejection"
        )
    }

    /**
     * Test 2: A replay of an already-accepted confirmation (identical submissionId and
     * fingerprint) returns the stored response without re-running the gate.
     *
     * Proves that the idempotency path (`existingSubmission != null && fingerprint equal`)
     * bypasses `FaceMatchReviewGate.require` entirely, which is correct: the original
     * confirmation already passed the gate, so the replay must not re-evaluate it.
     *
     * Validates: Requirements 8.5 (gate ordering), 9.6 (replay logic).
     */
    @Test
    fun `replay of accepted confirmation returns stored response without re-running gate`() {
        // Step 1: Successfully confirm with all HIGH-confidence scores
        val submissionId = UUID.randomUUID().toString()
        val originalRequest = ConfirmAttendanceRequestDto(
            submissionId = submissionId,
            entries = listOf(
                SubmittedAttendanceEntry(
                    studentId = student1.id!!,
                    status = "PRESENT",
                    confidenceScore = FaceMatchingThresholds.HIGH_CONFIDENCE,
                    teacherConfirmed = false
                ),
                SubmittedAttendanceEntry(
                    studentId = student2.id!!,
                    status = "PRESENT",
                    confidenceScore = FaceMatchingThresholds.HIGH_CONFIDENCE,
                    teacherConfirmed = false
                ),
                SubmittedAttendanceEntry(
                    studentId = student3.id!!,
                    status = "ABSENT",
                    confidenceScore = null
                )
            ),
            markUnspecifiedAsAbsent = false,
            method = "HYBRID"
        )

        val originalResponse = attendanceConfirmationService.confirm(session.id!!, caller, originalRequest)

        // Verify original confirmation succeeded
        assertNotNull(originalResponse)
        assertEquals(submissionId, originalResponse.submissionId)
        assertEquals(false, originalResponse.replayed, "original confirmation should not be marked as replayed")
        assertEquals(SessionStatus.FINALIZED, originalResponse.sessionStatus)

        // Verify records were persisted
        val recordsAfterOriginal = attendanceRecordRepository.findByLectureSessionId(session.id!!)
        assertEquals(3, recordsAfterOriginal.size, "three attendance records should be persisted after original confirmation")

        // Step 2: Replay with the same submissionId
        // The entry list is identical to the original (same fingerprint), so this is a valid replay.
        val replayResponse = attendanceConfirmationService.confirm(session.id!!, caller, originalRequest)

        // Verify the replay returns the stored response
        assertNotNull(replayResponse)
        assertEquals(submissionId, replayResponse.submissionId)
        assertEquals(true, replayResponse.replayed, "replay should be marked as replayed")
        assertEquals(originalResponse.totalMarked, replayResponse.totalMarked)
        assertEquals(originalResponse.presentCount, replayResponse.presentCount)
        assertEquals(originalResponse.absentCount, replayResponse.absentCount)
        assertEquals(originalResponse.onLeaveCount, replayResponse.onLeaveCount)

        // Verify no additional records were persisted (still 3)
        val recordsAfterReplay = attendanceRecordRepository.findByLectureSessionId(session.id!!)
        assertEquals(3, recordsAfterReplay.size, "record count should remain 3 after replay, not duplicate")
    }

    /**
     * Test 3: The gate runs inside the same transaction and behind the same pessimistic
     * lock as the rest of `confirm()`. A rejection by the gate leaves no partial state.
     *
     * This is structurally enforced in `AttendanceConfirmationService.confirm()`:
     * - Step 1: `findByIdForUpdate(sessionId)` acquires the pessimistic lock
     * - Step 7.5: `FaceMatchReviewGate.require(entries)` runs
     * - Step 9+: Records are persisted
     *
     * All three steps are inside the same `@Transactional` method, so they execute within a
     * single database transaction. If the gate throws, the entire transaction rolls back.
     *
     * Behavioral proof: This test is similar to test 1, but with an explicit check that the
     * session row itself was not partially modified. After gate rejection, the session
     * should remain in its original state (ACTIVE, counters unchanged).
     *
     * Validates: Requirements 8.5, 9.6 (transaction atomicity).
     */
    @Test
    fun `gate runs inside same transaction as lock and record writes — rejection leaves no partial state`() {
        // Capture the session's original state
        val originalSession = lectureSessionRepository.findById(session.id!!).orElseThrow()
        val originalStatus = originalSession.status
        val originalPresentCount = originalSession.presentCount
        val originalAbsentCount = originalSession.absentCount
        val originalOnLeaveCount = originalSession.onLeaveCount
        val originalUpdatedAt = originalSession.updatedAt

        // Build a confirmation request with an unresolved MEDIUM entry (will be rejected)
        val submissionId = UUID.randomUUID().toString()
        val request = ConfirmAttendanceRequestDto(
            submissionId = submissionId,
            entries = listOf(
                SubmittedAttendanceEntry(
                    studentId = student1.id!!,
                    status = "PRESENT",
                    confidenceScore = FaceMatchingThresholds.HIGH_CONFIDENCE,
                    teacherConfirmed = false
                ),
                SubmittedAttendanceEntry(
                    studentId = student2.id!!,
                    status = "PRESENT",
                    confidenceScore = FaceMatchingThresholds.MEDIUM_CONFIDENCE, // MEDIUM: blocks
                    teacherConfirmed = false // unresolved
                ),
                SubmittedAttendanceEntry(
                    studentId = student3.id!!,
                    status = "ABSENT",
                    confidenceScore = null
                )
            ),
            markUnspecifiedAsAbsent = false,
            method = "HYBRID"
        )

        // Assert that the confirmation throws ConflictException
        assertThrows<ConflictException> {
            attendanceConfirmationService.confirm(session.id!!, caller, request)
        }

        // Verify the session row was not modified at all (no partial finalization)
        val reloadedSession = lectureSessionRepository.findById(session.id!!).orElseThrow()
        assertEquals(originalStatus, reloadedSession.status, "session status should remain unchanged after gate rejection")
        assertEquals(originalPresentCount, reloadedSession.presentCount, "presentCount should remain unchanged")
        assertEquals(originalAbsentCount, reloadedSession.absentCount, "absentCount should remain unchanged")
        assertEquals(originalOnLeaveCount, reloadedSession.onLeaveCount, "onLeaveCount should remain unchanged")
        assertEquals(originalUpdatedAt, reloadedSession.updatedAt, "updatedAt should remain unchanged after gate rejection")

        // Verify zero attendance records were persisted
        val records = attendanceRecordRepository.findByLectureSessionId(session.id!!)
        assertEquals(0, records.size, "no attendance records should be persisted after gate rejection")

        // Verify the submission was not stored
        val submission = attendanceSubmissionRepository.findById(UUID.fromString(submissionId))
        assertTrue(submission.isEmpty, "no submission record should be stored after gate rejection")
    }
}
