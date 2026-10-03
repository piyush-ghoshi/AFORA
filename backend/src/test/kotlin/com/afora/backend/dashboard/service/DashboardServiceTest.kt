package com.afora.backend.dashboard.service

import com.afora.backend.academic.entity.ClassSection
import com.afora.backend.academic.entity.LectureSession
import com.afora.backend.academic.entity.Semester
import com.afora.backend.academic.entity.SessionStatus
import com.afora.backend.academic.entity.Subject
import com.afora.backend.academic.entity.TimetableSlot
import com.afora.backend.academic.repository.LectureSessionRepository
import com.afora.backend.academic.repository.SemesterRepository
import com.afora.backend.academic.repository.TimetableSlotRepository
import com.afora.backend.attendance.entity.LeaveStatus
import com.afora.backend.attendance.repository.AttendanceRecordRepository
import com.afora.backend.attendance.repository.LeaveRequestRepository
import com.afora.backend.user.entity.Teacher
import com.afora.backend.user.entity.User
import com.afora.backend.user.entity.UserRole
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for DashboardService (Phase A8.4 real-repository implementation).
 */
class DashboardServiceTest {

    private lateinit var timetableSlotRepository: TimetableSlotRepository
    private lateinit var lectureSessionRepository: LectureSessionRepository
    private lateinit var attendanceRecordRepository: AttendanceRecordRepository
    private lateinit var leaveRequestRepository: LeaveRequestRepository
    private lateinit var semesterRepository: SemesterRepository
    private lateinit var service: DashboardService

    private val studentId = 1L
    private val teacherId = 20L
    private val currentSemester: UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174002")

    private lateinit var teacher: Teacher
    private lateinit var subject: Subject
    private lateinit var section: ClassSection

    @BeforeEach
    fun setUp() {
        timetableSlotRepository = mockk()
        lectureSessionRepository = mockk()
        attendanceRecordRepository = mockk()
        leaveRequestRepository = mockk()
        semesterRepository = mockk()
        service = DashboardService(
            timetableSlotRepository,
            lectureSessionRepository,
            attendanceRecordRepository,
            leaveRequestRepository,
            semesterRepository
        )

        teacher = Teacher(
            id = teacherId,
            user = User(
                id = 99L,
                firebaseUid = "uid_t",
                email = "t@afora.edu",
                firstName = "John",
                lastName = "Doe",
                role = UserRole.TEACHER
            ),
            employeeId = "EMP001",
            department = "Computer Science"
        )
        subject = Subject(id = 101L, code = "CS101", name = "Data Structures", credits = 4, department = "CS")
        section = ClassSection(
            id = 10L,
            name = "CS101 Section A",
            subject = subject,
            teacher = teacher,
            semesterId = currentSemester,
            batch = "2024",
            section = "A",
            roomNumber = "A-101"
        )

        every { semesterRepository.findFirstByIsCurrentTrue() } returns
            Semester(id = currentSemester, name = "Fall 2024", isCurrent = true)
    }

    private fun slot(id: Long, day: Int, start: String) = TimetableSlot(
        id = id,
        classSection = section,
        dayOfWeek = day,
        startTime = LocalTime.parse(start),
        endTime = LocalTime.parse(start).plusMinutes(90),
        roomNumber = "A-101"
    )

    private fun session(id: Long, start: String, status: SessionStatus = SessionStatus.SCHEDULED) =
        LectureSession(
            id = id,
            classSection = section,
            semesterId = currentSemester,
            sessionDate = LocalDate.of(2026, 9, 17),
            startTime = LocalTime.parse(start),
            endTime = LocalTime.parse(start).plusMinutes(90),
            roomNumber = "A-101",
            status = status,
            totalStudents = 60,
            presentCount = 55,
            absentCount = 5
        )

    private fun stubStudentCounts(attended: Long, countable: Long, pendingLeave: Int = 0) {
        every { attendanceRecordRepository.countAttended(studentId, currentSemester) } returns attended
        every { attendanceRecordRepository.countCountable(studentId, currentSemester) } returns countable
        every { leaveRequestRepository.countByStudentIdAndStatus(studentId, LeaveStatus.PENDING) } returns pendingLeave
        every { timetableSlotRepository.findActiveForStudent(studentId) } returns emptyList()
    }

    // -- student dashboard ---------------------------------------------------

    @Test
    fun `attendance percentage is derived from the counts`() {
        stubStudentCounts(attended = 45, countable = 52)

        val result = service.getStudentDashboard(studentId, null)

        assertEquals(86.54, result.attendancePercentage)
        assertEquals(45, result.presentCount)
        assertEquals(52, result.totalCount)
    }

    @Test
    fun `attendance percentage is zero when nothing countable has been held`() {
        stubStudentCounts(attended = 0, countable = 0)

        val result = service.getStudentDashboard(studentId, null)

        assertEquals(0.0, result.attendancePercentage, "must not divide by zero")
    }

    @Test
    fun `full attendance reports one hundred percent`() {
        stubStudentCounts(attended = 30, countable = 30)
        assertEquals(100.0, service.getStudentDashboard(studentId, null).attendancePercentage)
    }

    @Test
    fun `pending leave count is passed through`() {
        stubStudentCounts(attended = 10, countable = 10, pendingLeave = 3)
        assertEquals(3, service.getStudentDashboard(studentId, null).pendingLeaveRequests)
    }

    @Test
    fun `null semester falls back to the current semester`() {
        stubStudentCounts(attended = 1, countable = 1)

        service.getStudentDashboard(studentId, null)

        verify { attendanceRecordRepository.countAttended(studentId, currentSemester) }
    }

    @Test
    fun `explicit semester overrides the current semester`() {
        val explicit = UUID.fromString("99999999-8888-7777-6666-555555555555")
        every { attendanceRecordRepository.countAttended(studentId, explicit) } returns 5
        every { attendanceRecordRepository.countCountable(studentId, explicit) } returns 10
        every { leaveRequestRepository.countByStudentIdAndStatus(studentId, LeaveStatus.PENDING) } returns 0
        every { timetableSlotRepository.findActiveForStudent(studentId) } returns emptyList()

        val result = service.getStudentDashboard(studentId, explicit)

        assertEquals(50.0, result.attendancePercentage)
        verify(exactly = 0) { semesterRepository.findFirstByIsCurrentTrue() }
    }

    @Test
    fun `upcoming classes are ordered starting from today`() {
        val today = LocalDate.now().dayOfWeek.value
        val tomorrow = (today % 7) + 1
        // Deliberately supply tomorrow first to prove the service re-orders.
        every { attendanceRecordRepository.countAttended(studentId, currentSemester) } returns 1
        every { attendanceRecordRepository.countCountable(studentId, currentSemester) } returns 1
        every { leaveRequestRepository.countByStudentIdAndStatus(studentId, LeaveStatus.PENDING) } returns 0
        every { timetableSlotRepository.findActiveForStudent(studentId) } returns
            listOf(slot(2L, tomorrow, "11:00"), slot(1L, today, "09:00"))

        val classes = service.getStudentDashboard(studentId, null).upcomingClasses

        assertEquals(2, classes.size)
        assertEquals(today, classes.first().dayOfWeek, "today's class must come first")
        assertEquals(tomorrow, classes[1].dayOfWeek)
    }

    @Test
    fun `timetable slot maps subject and teacher detail`() {
        val today = LocalDate.now().dayOfWeek.value
        every { attendanceRecordRepository.countAttended(studentId, currentSemester) } returns 1
        every { attendanceRecordRepository.countCountable(studentId, currentSemester) } returns 1
        every { leaveRequestRepository.countByStudentIdAndStatus(studentId, LeaveStatus.PENDING) } returns 0
        every { timetableSlotRepository.findActiveForStudent(studentId) } returns listOf(slot(1L, today, "09:00"))

        val slot = service.getStudentDashboard(studentId, null).upcomingClasses.single()

        assertEquals("CS101", slot.subjectCode)
        assertEquals("Data Structures", slot.subjectName)
        assertEquals("John Doe", slot.teacherName)
        assertEquals(teacherId, slot.teacherId)
        assertEquals("09:00", slot.startTime, "timetable times use HH:mm")
        assertEquals("10:30", slot.endTime)
        assertEquals("A-101", slot.roomNumber)
    }

    // -- teacher dashboard ---------------------------------------------------

    @Test
    fun `teacher schedule is returned for the requested date`() {
        val date = LocalDate.of(2026, 9, 17)
        every { lectureSessionRepository.findByTeacherAndDate(teacherId, date) } returns
            listOf(session(1L, "09:00"), session(2L, "11:00"))
        every { leaveRequestRepository.countPendingForTeacher(teacherId) } returns 5

        val result = service.getTeacherDashboard(teacherId, date)

        assertEquals(2, result.todaySchedule.size)
        assertEquals(5, result.pendingLeaveRequests)
    }

    @Test
    fun `teacher dashboard defaults to today when no date given`() {
        val today = LocalDate.now()
        every { lectureSessionRepository.findByTeacherAndDate(teacherId, today) } returns emptyList()
        every { leaveRequestRepository.countPendingForTeacher(teacherId) } returns 0

        service.getTeacherDashboard(teacherId, null)

        verify { lectureSessionRepository.findByTeacherAndDate(teacherId, today) }
    }

    @Test
    fun `lecture session maps to the wire contract`() {
        val date = LocalDate.of(2026, 9, 17)
        every { lectureSessionRepository.findByTeacherAndDate(teacherId, date) } returns
            listOf(session(7L, "14:00", SessionStatus.FINALIZED))
        every { leaveRequestRepository.countPendingForTeacher(teacherId) } returns 0

        val s = service.getTeacherDashboard(teacherId, date).todaySchedule.single()

        assertEquals(7L, s.id)
        assertEquals(101L, s.subjectId)
        assertEquals(10L, s.classSectionId)
        assertEquals(teacherId, s.teacherId)
        assertEquals(currentSemester.toString(), s.semesterId, "semesterId is the UUID as text")
        assertEquals(date, s.date)
        assertEquals("14:00:00", s.startTime, "session times use HH:mm:ss")
        assertEquals("15:30:00", s.endTime)
        assertEquals("FINALIZED", s.status)
        assertEquals(55, s.presentCount)
        assertEquals(5, s.absentCount)
    }

    @Test
    fun `pending queries reports zero until queries are modelled`() {
        val today = LocalDate.now()
        every { lectureSessionRepository.findByTeacherAndDate(teacherId, today) } returns emptyList()
        every { leaveRequestRepository.countPendingForTeacher(teacherId) } returns 2

        val result = service.getTeacherDashboard(teacherId, null)

        assertEquals(0, result.pendingQueries)
        assertTrue(result.todaySchedule.isEmpty())
    }
}