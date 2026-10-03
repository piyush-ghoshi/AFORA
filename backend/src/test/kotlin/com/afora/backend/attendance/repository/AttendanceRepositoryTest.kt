package com.afora.backend.attendance.repository

import com.afora.backend.academic.entity.AttendanceMethod
import com.afora.backend.academic.entity.ClassSection
import com.afora.backend.academic.entity.Enrollment
import com.afora.backend.academic.entity.EnrollmentStatus
import com.afora.backend.academic.entity.LectureSession
import com.afora.backend.academic.entity.SessionStatus
import com.afora.backend.academic.entity.Subject
import com.afora.backend.academic.entity.TimetableSlot
import com.afora.backend.academic.repository.LectureSessionRepository
import com.afora.backend.academic.repository.TimetableSlotRepository
import com.afora.backend.attendance.entity.AttendanceRecord
import com.afora.backend.attendance.entity.AttendanceStatus
import com.afora.backend.attendance.entity.LeaveRequest
import com.afora.backend.attendance.entity.LeaveStatus
import com.afora.backend.user.entity.Student
import com.afora.backend.user.entity.Teacher
import com.afora.backend.user.entity.User
import com.afora.backend.user.entity.UserRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for the Phase A8.2 / A8.3 repositories against H2.
 *
 * These exercise the aggregate queries the dashboard depends on, including the
 * neutral-status rule for leave (ON_LEAVE must not count against a student).
 */
@DataJpaTest
@ActiveProfiles("test")
class AttendanceRepositoryTest {

    @Autowired private lateinit var em: TestEntityManager
    @Autowired private lateinit var timetableSlotRepository: TimetableSlotRepository
    @Autowired private lateinit var lectureSessionRepository: LectureSessionRepository
    @Autowired private lateinit var attendanceRepository: AttendanceRecordRepository
    @Autowired private lateinit var leaveRepository: LeaveRequestRepository

    private val semesterA: UUID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001")
    private val semesterB: UUID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002")

    private var teacherId = 0L
    private var otherTeacherId = 0L
    private var studentId = 0L
    private var sectionId = 0L
    private var pastSessionId = 0L

    private fun newUser(uid: String, role: UserRole) = em.persistAndFlush(
        User(firebaseUid = uid, email = "$uid@afora.edu", firstName = "F", lastName = "L", role = role)
    )

    @BeforeEach
    fun seed() {
        val tUser = newUser("t1", UserRole.TEACHER)
        val teacher = em.persistAndFlush(
            Teacher(user = tUser, employeeId = "EMP001", department = "CS")
        )
        teacherId = teacher.id!!

        val t2User = newUser("t2", UserRole.TEACHER)
        val otherTeacher = em.persistAndFlush(
            Teacher(user = t2User, employeeId = "EMP002", department = "CS")
        )
        otherTeacherId = otherTeacher.id!!

        val sUser = newUser("s1", UserRole.STUDENT)
        val student = em.persistAndFlush(
            Student(user = sUser, rollNumber = "2024CS001", department = "CS", batch = "2024")
        )
        studentId = student.id!!

        val subject = em.persistAndFlush(Subject(code = "CS101", name = "Data Structures", department = "CS"))
        val section = em.persistAndFlush(
            ClassSection(
                name = "CS101 A", subject = subject, teacher = teacher,
                semesterId = semesterA, batch = "2024", section = "A", roomNumber = "A-101"
            )
        )
        sectionId = section.id

        em.persistAndFlush(
            Enrollment(student = student, classSection = section, status = EnrollmentStatus.ACTIVE)
        )

        // Monday 09:00 weekly slot
        em.persistAndFlush(
            TimetableSlot(
                classSection = section, dayOfWeek = 1,
                startTime = LocalTime.of(9, 0), endTime = LocalTime.of(10, 30), roomNumber = "A-101"
            )
        )

        // Two held sessions and one upcoming
        val past1 = em.persistAndFlush(
            LectureSession(
                classSection = section, semesterId = semesterA,
                sessionDate = LocalDate.of(2026, 9, 7),
                startTime = LocalTime.of(9, 0), endTime = LocalTime.of(10, 30),
                status = SessionStatus.FINALIZED, totalStudents = 1
            )
        )
        pastSessionId = past1.id
        val past2 = em.persistAndFlush(
            LectureSession(
                classSection = section, semesterId = semesterA,
                sessionDate = LocalDate.of(2026, 9, 14),
                startTime = LocalTime.of(9, 0), endTime = LocalTime.of(10, 30),
                status = SessionStatus.FINALIZED, totalStudents = 1
            )
        )
        val past3 = em.persistAndFlush(
            LectureSession(
                classSection = section, semesterId = semesterA,
                sessionDate = LocalDate.of(2026, 9, 21),
                startTime = LocalTime.of(9, 0), endTime = LocalTime.of(10, 30),
                status = SessionStatus.FINALIZED, totalStudents = 1
            )
        )
        em.persistAndFlush(
            LectureSession(
                classSection = section, semesterId = semesterA,
                sessionDate = LocalDate.of(2026, 9, 28),
                startTime = LocalTime.of(9, 0), endTime = LocalTime.of(10, 30),
                status = SessionStatus.SCHEDULED, totalStudents = 1
            )
        )

        // PRESENT, ABSENT, ON_LEAVE across the three held sessions
        em.persistAndFlush(
            AttendanceRecord(
                lectureSession = past1, student = student,
                status = AttendanceStatus.PRESENT, method = AttendanceMethod.MANUAL
            )
        )
        em.persistAndFlush(
            AttendanceRecord(
                lectureSession = past2, student = student,
                status = AttendanceStatus.ABSENT, method = AttendanceMethod.MANUAL
            )
        )
        em.persistAndFlush(
            AttendanceRecord(
                lectureSession = past3, student = student,
                status = AttendanceStatus.ON_LEAVE, method = AttendanceMethod.MANUAL
            )
        )

        em.persistAndFlush(
            LeaveRequest(
                student = student, classSection = section,
                fromDate = LocalDate.of(2026, 9, 21), toDate = LocalDate.of(2026, 9, 21),
                reason = "Medical", status = LeaveStatus.PENDING
            )
        )
        em.persistAndFlush(
            LeaveRequest(
                student = student, classSection = section,
                fromDate = LocalDate.of(2026, 9, 1), toDate = LocalDate.of(2026, 9, 1),
                reason = "Old", status = LeaveStatus.APPROVED
            )
        )
        em.clear()
    }

    // -- attendance aggregates ----------------------------------------------

    @Test
    fun `countAttended counts present and late only`() {
        assertEquals(1L, attendanceRepository.countAttended(studentId, semesterA))
    }

    @Test
    fun `countCountable excludes on-leave so approved absence is not punished`() {
        // 3 records exist, but ON_LEAVE is neutral -> denominator is 2
        assertEquals(2L, attendanceRepository.countCountable(studentId, semesterA))
    }

    @Test
    fun `late counts as attended`() {
        val s = em.find(Student::class.java, studentId)
        val extra = em.persistAndFlush(
            LectureSession(
                classSection = em.find(ClassSection::class.java, sectionId),
                semesterId = semesterA,
                sessionDate = LocalDate.of(2026, 10, 5),
                startTime = LocalTime.of(9, 0), endTime = LocalTime.of(10, 30),
                status = SessionStatus.FINALIZED
            )
        )
        em.persistAndFlush(
            AttendanceRecord(lectureSession = extra, student = s, status = AttendanceStatus.LATE)
        )
        em.clear()

        assertEquals(2L, attendanceRepository.countAttended(studentId, semesterA))
        assertEquals(3L, attendanceRepository.countCountable(studentId, semesterA))
    }

    @Test
    fun `a null semester counts across all semesters`() {
        assertEquals(1L, attendanceRepository.countAttended(studentId, null))
    }

    @Test
    fun `another semester yields no records`() {
        assertEquals(0L, attendanceRepository.countAttended(studentId, semesterB))
        assertEquals(0L, attendanceRepository.countCountable(studentId, semesterB))
    }

    @Test
    fun `per session status counts are available`() {
        assertEquals(1, attendanceRepository.countByLectureSessionIdAndStatus(pastSessionId, AttendanceStatus.PRESENT))
        assertEquals(0, attendanceRepository.countByLectureSessionIdAndStatus(pastSessionId, AttendanceStatus.ABSENT))
    }

    // -- sessions ------------------------------------------------------------

    @Test
    fun `countHeldSessionsForStudent counts only finalized sessions`() {
        // 3 FINALIZED, 1 SCHEDULED -> upcoming must not inflate the denominator
        assertEquals(3L, lectureSessionRepository.countHeldSessionsForStudent(studentId, semesterA))
    }

    @Test
    fun `findByTeacherAndDate returns that days sessions`() {
        val sessions = lectureSessionRepository.findByTeacherAndDate(teacherId, LocalDate.of(2026, 9, 7))
        assertEquals(1, sessions.size)
        assertEquals(LocalTime.of(9, 0), sessions.first().startTime)
    }

    @Test
    fun `findByTeacherAndDate is scoped to the teacher`() {
        assertTrue(
            lectureSessionRepository.findByTeacherAndDate(otherTeacherId, LocalDate.of(2026, 9, 7)).isEmpty(),
            "a teacher must not see another teacher's sessions"
        )
    }

    @Test
    fun `sessions can be listed for a date range`() {
        val sessions = lectureSessionRepository
            .findByClassSectionIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
                sectionId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 15)
            )
        assertEquals(2, sessions.size)
    }

    // -- timetable -----------------------------------------------------------

    @Test
    fun `findActiveForStudent returns the students weekly slots`() {
        val slots = timetableSlotRepository.findActiveForStudent(studentId)
        assertEquals(1, slots.size)
        assertEquals(1, slots.first().dayOfWeek)
        // fetch-joined graph must be populated for dashboard mapping
        assertEquals("CS101", slots.first().classSection!!.subject!!.code)
        assertEquals("EMP001", slots.first().classSection!!.teacher!!.employeeId)
    }

    @Test
    fun `findActiveForTeacher returns the teachers slots`() {
        assertEquals(1, timetableSlotRepository.findActiveForTeacher(teacherId).size)
        assertTrue(timetableSlotRepository.findActiveForTeacher(otherTeacherId).isEmpty())
    }

    // -- leave ---------------------------------------------------------------

    @Test
    fun `countByStudentIdAndStatus counts only pending`() {
        assertEquals(1, leaveRepository.countByStudentIdAndStatus(studentId, LeaveStatus.PENDING))
        assertEquals(1, leaveRepository.countByStudentIdAndStatus(studentId, LeaveStatus.APPROVED))
        assertEquals(0, leaveRepository.countByStudentIdAndStatus(studentId, LeaveStatus.REJECTED))
    }

    @Test
    fun `countPendingForTeacher is scoped to sections the teacher owns`() {
        assertEquals(1, leaveRepository.countPendingForTeacher(teacherId))
        assertEquals(0, leaveRepository.countPendingForTeacher(otherTeacherId))
    }
}