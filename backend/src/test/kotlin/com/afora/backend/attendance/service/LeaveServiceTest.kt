package com.afora.backend.attendance.service

import com.afora.backend.academic.entity.ClassSection
import com.afora.backend.academic.entity.Subject
import com.afora.backend.academic.repository.ClassSectionRepository
import com.afora.backend.attendance.dto.SubmitLeaveRequestDto
import com.afora.backend.attendance.entity.LeaveRequest
import com.afora.backend.attendance.entity.LeaveStatus
import com.afora.backend.attendance.repository.LeaveRequestRepository
import com.afora.backend.common.exception.BadRequestException
import com.afora.backend.common.exception.ConflictException
import com.afora.backend.common.exception.ResourceNotFoundException
import com.afora.backend.user.entity.Student
import com.afora.backend.user.entity.Teacher
import com.afora.backend.user.entity.User
import com.afora.backend.user.entity.UserRole
import com.afora.backend.user.repository.StudentRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for [LeaveService]: the Leave Requests feature's submit, list,
 * approve, reject and cancel operations. Mirrors [com.afora.backend.dashboard.service.DashboardServiceTest]'s
 * mockk-per-collaborator style.
 */
class LeaveServiceTest {

    private lateinit var leaveRequestRepository: LeaveRequestRepository
    private lateinit var studentRepository: StudentRepository
    private lateinit var classSectionRepository: ClassSectionRepository
    private lateinit var clock: Clock
    private lateinit var service: LeaveService

    private val studentId = 1L
    private val fixedNow = Instant.parse("2026-09-15T10:00:00Z")

    private lateinit var student: Student
    private lateinit var teacher: Teacher
    private lateinit var section: ClassSection

    @BeforeEach
    fun setUp() {
        leaveRequestRepository = mockk()
        studentRepository = mockk()
        classSectionRepository = mockk()
        clock = Clock.fixed(fixedNow, ZoneOffset.UTC)
        service = LeaveService(leaveRequestRepository, studentRepository, classSectionRepository, clock)

        val sUser = User(id = 10, firebaseUid = "s1", email = "s1@afora.edu", firstName = "Alice", lastName = "Johnson", role = UserRole.STUDENT)
        student = Student(id = studentId, user = sUser, rollNumber = "2024CS001", department = "CS", batch = "2024")

        val tUser = User(id = 20, firebaseUid = "t1", email = "t1@afora.edu", firstName = "Dana", lastName = "Smith", role = UserRole.TEACHER)
        teacher = Teacher(id = 30, user = tUser, employeeId = "EMP001", department = "CS")

        section = ClassSection(id = 40, name = "CS101 A", teacher = teacher)
    }

    private fun pendingRequest(id: Long = 100) = LeaveRequest(
        id = id,
        student = student,
        classSection = section,
        fromDate = LocalDate.of(2026, 9, 21),
        toDate = LocalDate.of(2026, 9, 21),
        reason = "Medical appointment",
        status = LeaveStatus.PENDING
    )

    // -- submit ---------------------------------------------------------------

    @Test
    fun `submit creates a PENDING request and returns it`() {
        every { studentRepository.findById(studentId) } returns Optional.of(student)
        every { classSectionRepository.findById(40L) } returns Optional.of(section)
        val savedSlot = slot<LeaveRequest>()
        every { leaveRequestRepository.save(capture(savedSlot)) } answers { savedSlot.captured.copy(id = 100) }

        val response = service.submit(
            studentId,
            SubmitLeaveRequestDto(
                fromDate = LocalDate.of(2026, 9, 21),
                toDate = LocalDate.of(2026, 9, 21),
                reason = "Medical appointment",
                classSectionId = 40
            )
        )

        assertEquals(100L, response.id)
        assertEquals(LeaveStatus.PENDING, response.status)
        assertEquals(studentId, response.studentId)
        assertEquals("Alice Johnson", response.studentName)
        assertEquals(LeaveStatus.PENDING, savedSlot.captured.status)
    }

    @Test
    fun `submit with a null classSectionId spans all of the student's classes`() {
        every { studentRepository.findById(studentId) } returns Optional.of(student)
        val savedSlot = slot<LeaveRequest>()
        every { leaveRequestRepository.save(capture(savedSlot)) } answers { savedSlot.captured.copy(id = 101) }

        service.submit(
            studentId,
            SubmitLeaveRequestDto(
                fromDate = LocalDate.of(2026, 9, 21),
                toDate = LocalDate.of(2026, 9, 21),
                reason = "Family event",
                classSectionId = null
            )
        )

        assertNull(savedSlot.captured.classSection)
        verify(exactly = 0) { classSectionRepository.findById(any()) }
    }

    @Test
    fun `submit rejects toDate before fromDate`() {
        val ex = assertThrows<BadRequestException> {
            service.submit(
                studentId,
                SubmitLeaveRequestDto(
                    fromDate = LocalDate.of(2026, 9, 21),
                    toDate = LocalDate.of(2026, 9, 20),
                    reason = "x"
                )
            )
        }
        assertEquals(true, ex.message?.contains("toDate"))
    }

    @Test
    fun `submit rejects a blank reason`() {
        assertThrows<BadRequestException> {
            service.submit(
                studentId,
                SubmitLeaveRequestDto(
                    fromDate = LocalDate.of(2026, 9, 21),
                    toDate = LocalDate.of(2026, 9, 21),
                    reason = "   "
                )
            )
        }
    }

    @Test
    fun `submit against an unknown student raises ResourceNotFoundException`() {
        every { studentRepository.findById(studentId) } returns Optional.empty()
        assertThrows<ResourceNotFoundException> {
            service.submit(
                studentId,
                SubmitLeaveRequestDto(
                    fromDate = LocalDate.of(2026, 9, 21),
                    toDate = LocalDate.of(2026, 9, 21),
                    reason = "x"
                )
            )
        }
    }

    // -- class options ------------------------------------------------------------

    @Test
    fun `listClassOptions returns the students active classes ordered by subject code`() {
        val maths = ClassSection(id = 41, name = "MATH210 B", subject = Subject(id = 2, code = "MATH210", name = "Discrete Mathematics"), teacher = teacher)
        val cs = ClassSection(id = 40, name = "CS101 A", subject = Subject(id = 1, code = "CS101", name = "Data Structures"), teacher = teacher)
        every { classSectionRepository.findActiveByStudentId(studentId) } returns listOf(maths, cs)

        val options = service.listClassOptions(studentId)

        assertEquals(listOf("CS101", "MATH210"), options.map { it.subjectCode })
        assertEquals(40L, options.first().classSectionId)
        assertEquals("CS101 A", options.first().sectionName)
    }

    @Test
    fun `listClassOptions skips a section with no subject and returns empty for none`() {
        every { classSectionRepository.findActiveByStudentId(studentId) } returns listOf(
            ClassSection(id = 50, name = "Orphan", subject = null, teacher = teacher)
        )
        assertEquals(0, service.listClassOptions(studentId).size)

        every { classSectionRepository.findActiveByStudentId(2L) } returns emptyList()
        assertEquals(0, service.listClassOptions(2L).size)
    }
    // -- list -------------------------------------------------------------------

    @Test
    fun `listForStudent with no status filter returns every request`() {
        every { leaveRequestRepository.findByStudentId(studentId) } returns listOf(pendingRequest())
        val result = service.listForStudent(studentId, null)
        assertEquals(1, result.size)
    }

    @Test
    fun `listForStudent with a status filter delegates to the filtered query`() {
        every { leaveRequestRepository.findByStudentIdAndStatus(studentId, LeaveStatus.PENDING) } returns listOf(pendingRequest())
        val result = service.listForStudent(studentId, LeaveStatus.PENDING)
        assertEquals(1, result.size)
        verify(exactly = 0) { leaveRequestRepository.findByStudentId(any()) }
    }

    @Test
    fun `listPendingForTeacher delegates to the list-form query`() {
        every { leaveRequestRepository.findPendingForTeacher(30L) } returns listOf(pendingRequest())
        val result = service.listPendingForTeacher(30L)
        assertEquals(1, result.size)
    }

    // -- approve / reject --------------------------------------------------------

    @Test
    fun `approve on a PENDING request sets APPROVED, the reviewer and reviewedAt`() {
        every { leaveRequestRepository.findById(100L) } returns Optional.of(pendingRequest())
        val savedSlot = slot<LeaveRequest>()
        every { leaveRequestRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val response = service.approve(100L, teacher, "Approved, documents verified")

        assertEquals(LeaveStatus.APPROVED, response.status)
        assertEquals(30L, response.reviewedByTeacherId)
        assertEquals("Dana Smith", response.reviewedByTeacherName)
        assertEquals(fixedNow.atZone(ZoneOffset.UTC).toLocalDateTime(), savedSlot.captured.reviewedAt)
    }

    @Test
    fun `approve on an already-decided request is a conflict`() {
        every { leaveRequestRepository.findById(100L) } returns Optional.of(
            pendingRequest().copy(status = LeaveStatus.APPROVED)
        )
        assertThrows<ConflictException> { service.approve(100L, teacher, null) }
    }

    @Test
    fun `reject requires non-blank notes`() {
        every { leaveRequestRepository.findById(100L) } returns Optional.of(pendingRequest())
        assertThrows<BadRequestException> { service.reject(100L, teacher, null) }
        assertThrows<BadRequestException> { service.reject(100L, teacher, "   ") }
    }

    @Test
    fun `reject on a PENDING request sets REJECTED with the given notes`() {
        every { leaveRequestRepository.findById(100L) } returns Optional.of(pendingRequest())
        val savedSlot = slot<LeaveRequest>()
        every { leaveRequestRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val response = service.reject(100L, teacher, "No supporting document")

        assertEquals(LeaveStatus.REJECTED, response.status)
        assertEquals("No supporting document", response.reviewNotes)
    }

    @Test
    fun `an admin review has no reviewing teacher`() {
        every { leaveRequestRepository.findById(100L) } returns Optional.of(pendingRequest())
        val savedSlot = slot<LeaveRequest>()
        every { leaveRequestRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val response = service.approve(100L, reviewingTeacher = null, notes = null)

        assertNull(response.reviewedByTeacherId)
        assertNull(savedSlot.captured.reviewedByTeacher)
    }

    // -- cancel -------------------------------------------------------------------

    @Test
    fun `cancel on a PENDING request sets CANCELLED`() {
        every { leaveRequestRepository.findById(100L) } returns Optional.of(pendingRequest())
        val savedSlot = slot<LeaveRequest>()
        every { leaveRequestRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val response = service.cancel(100L)

        assertEquals(LeaveStatus.CANCELLED, response.status)
    }

    @Test
    fun `cancel on an already-decided request is a conflict`() {
        every { leaveRequestRepository.findById(100L) } returns Optional.of(
            pendingRequest().copy(status = LeaveStatus.REJECTED)
        )
        assertThrows<ConflictException> { service.cancel(100L) }
    }

    @Test
    fun `cancel against an unknown request raises ResourceNotFoundException`() {
        every { leaveRequestRepository.findById(999L) } returns Optional.empty()
        assertThrows<ResourceNotFoundException> { service.cancel(999L) }
    }
}