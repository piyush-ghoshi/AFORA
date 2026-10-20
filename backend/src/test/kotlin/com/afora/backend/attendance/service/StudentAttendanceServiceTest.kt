package com.afora.backend.attendance.service

import com.afora.backend.academic.entity.ClassSection
import com.afora.backend.academic.entity.LectureSession
import com.afora.backend.academic.entity.Semester
import com.afora.backend.academic.entity.Subject
import com.afora.backend.academic.repository.SemesterRepository
import com.afora.backend.attendance.entity.AttendanceRecord
import com.afora.backend.attendance.entity.AttendanceStatus
import com.afora.backend.attendance.repository.AttendanceRecordRepository
import com.afora.backend.common.exception.NotFoundException
import com.afora.backend.user.entity.Student
import com.afora.backend.user.entity.User
import com.afora.backend.user.entity.UserRole
import com.afora.backend.user.repository.StudentRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StudentAttendanceServiceTest {

    private lateinit var studentRepository: StudentRepository
    private lateinit var attendanceRecordRepository: AttendanceRecordRepository
    private lateinit var semesterRepository: SemesterRepository
    private lateinit var service: StudentAttendanceService

    private val studentId = 1L
    private val semesterId = UUID.randomUUID()
    private val sUser = User(id = 10, firebaseUid = "s1", email = "s1@afora.edu", firstName = "Alice", lastName = "Johnson", role = UserRole.STUDENT)
    private val student = Student(id = studentId, user = sUser, rollNumber = "2024CS001", department = "CS", batch = "2024")
    private val subject = Subject(id = 101L, code = "CS101", name = "Computer Science I")
    private val classSection = ClassSection(id = 10L, name = "Sec A", subject = subject)

    @BeforeEach
    fun setUp() {
        studentRepository = mockk()
        attendanceRecordRepository = mockk()
        semesterRepository = mockk()
        service = StudentAttendanceService(studentRepository, attendanceRecordRepository, semesterRepository)
    }

    @Test
    fun `throws NotFoundException when student does not exist`() {
        every { studentRepository.findById(studentId) } returns Optional.empty()

        assertThrows<NotFoundException> {
            service.getStudentAttendance(studentId, semesterId)
        }
    }

    @Test
    fun `computes attendance percentages correctly and excludes leave from total lectures`() {
        every { studentRepository.findById(studentId) } returns Optional.of(student)

        val session1 = LectureSession(
            id = 1L,
            classSection = classSection,
            semesterId = semesterId,
            sessionDate = LocalDate.parse("2026-10-01"),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 0)
        )
        val session2 = LectureSession(
            id = 2L,
            classSection = classSection,
            semesterId = semesterId,
            sessionDate = LocalDate.parse("2026-10-02"),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 0)
        )
        val session3 = LectureSession(
            id = 3L,
            classSection = classSection,
            semesterId = semesterId,
            sessionDate = LocalDate.parse("2026-10-03"),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 0)
        )
        val session4 = LectureSession(
            id = 4L,
            classSection = classSection,
            semesterId = semesterId,
            sessionDate = LocalDate.parse("2026-10-04"),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 0)
        )

        val records = listOf(
            AttendanceRecord(id = 1001L, lectureSession = session1, student = student, status = AttendanceStatus.PRESENT),
            AttendanceRecord(id = 1002L, lectureSession = session2, student = student, status = AttendanceStatus.PRESENT),
            AttendanceRecord(id = 1003L, lectureSession = session3, student = student, status = AttendanceStatus.ABSENT),
            AttendanceRecord(id = 1004L, lectureSession = session4, student = student, status = AttendanceStatus.ON_LEAVE)
        )

        every { attendanceRecordRepository.findByStudentAndSemester(studentId, semesterId) } returns records
        every { attendanceRecordRepository.isCorrected(1001L) } returns false
        every { attendanceRecordRepository.isCorrected(1002L) } returns false
        every { attendanceRecordRepository.isCorrected(1003L) } returns true
        every { attendanceRecordRepository.isCorrected(1004L) } returns false

        val result = service.getStudentAttendance(studentId, semesterId)

        assertEquals(studentId, result.studentId)
        assertEquals(semesterId.toString(), result.semesterId)
        // 3 countable lectures (2 present, 1 absent), 1 on leave
        assertEquals(3, result.totalLectures)
        assertEquals(2, result.presentCount)
        assertEquals(1, result.absentCount)
        assertEquals(1, result.onLeaveCount)
        // 2 / 3 * 100 = 66.67%
        assertEquals(66.67, result.overallPercentage)

        assertEquals(1, result.subjects.size)
        val subSummary = result.subjects[0]
        assertEquals("CS101", subSummary.subjectCode)
        assertEquals(3, subSummary.totalLectures)
        assertEquals(2, subSummary.presentCount)
        assertEquals(1, subSummary.absentCount)
        assertEquals(1, subSummary.onLeaveCount)
        assertEquals(66.67, subSummary.attendancePercentage)
        assertEquals(4, subSummary.sessions.size)

        // Session 3 was marked corrected
        val s3 = subSummary.sessions.find { it.sessionId == 3L }
        assertTrue(s3!!.isCorrected)
        assertEquals(AttendanceStatus.ABSENT, s3.status)

        // Session 1 was not corrected
        val s1 = subSummary.sessions.find { it.sessionId == 1L }
        assertFalse(s1!!.isCorrected)
    }

    @Test
    fun `defaults to current semester when semesterId is null`() {
        val currentSem = Semester(id = semesterId, name = "Fall 2026", isCurrent = true)
        every { studentRepository.findById(studentId) } returns Optional.of(student)
        every { semesterRepository.findFirstByIsCurrentTrue() } returns currentSem
        every { attendanceRecordRepository.findByStudentAndSemester(studentId, semesterId) } returns emptyList()

        val result = service.getStudentAttendance(studentId, null)

        assertEquals(semesterId.toString(), result.semesterId)
        assertEquals(0, result.totalLectures)
        assertEquals(0.0, result.overallPercentage)
    }
}
