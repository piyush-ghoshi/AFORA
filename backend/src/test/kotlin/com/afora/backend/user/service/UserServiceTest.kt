package com.afora.backend.user.service

import com.afora.backend.common.exception.BadRequestException
import com.afora.backend.common.exception.ConflictException
import com.afora.backend.user.dto.SyncUserRequest
import com.afora.backend.user.entity.Student
import com.afora.backend.user.entity.Teacher
import com.afora.backend.user.entity.User
import com.afora.backend.user.entity.UserRole
import com.afora.backend.user.repository.StudentRepository
import com.afora.backend.user.repository.TeacherRepository
import com.afora.backend.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Sign-up rules: a new account needs a roll number or employee id, and gets its
 * Student/Teacher row in the same step.
 */
class UserServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var studentRepository: StudentRepository
    private lateinit var teacherRepository: TeacherRepository
    private lateinit var service: UserService

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        studentRepository = mockk()
        teacherRepository = mockk()
        service = UserService(userRepository, studentRepository, teacherRepository)

        every { userRepository.findByFirebaseUid(any()) } returns Optional.empty()
        every { userRepository.existsByEmail(any()) } returns false
        every { studentRepository.existsByRollNumber(any()) } returns false
        every { teacherRepository.existsByEmployeeId(any()) } returns false
        every { userRepository.save(any()) } answers { firstArg<User>().also { if (it.id == null) it.id = 10 } }
        every { studentRepository.save(any()) } answers { firstArg<Student>().also { it.id = 20 } }
        every { teacherRepository.save(any()) } answers { firstArg<Teacher>().also { it.id = 30 } }
    }

    private fun request(role: String = "STUDENT", idNumber: String? = "CS-2023-048", uid: String = "uid-1") =
        SyncUserRequest(
            firebaseUid = uid,
            email = "alex@univ.edu",
            firstName = "Alex",
            lastName = "Morgan",
            role = role,
            idNumber = idNumber
        )

    // -- new accounts ----------------------------------------------------------

    @Test
    fun `a new student gets a user row and a student row with the roll number`() {
        val response = service.syncUser(request())

        assertTrue(response.isNewUser)
        assertEquals("STUDENT", response.user.role)
        assertEquals("CS-2023-048", response.user.student?.rollNumber)
        assertEquals(20L, response.user.student?.id)
        assertEquals(UserService.UNASSIGNED, response.user.student?.department)
        assertNull(response.user.teacher)
    }

    @Test
    fun `a new teacher gets a teacher row with the employee id`() {
        val response = service.syncUser(request(role = "teacher", idNumber = "FAC-104"))

        assertEquals("TEACHER", response.user.role)
        assertEquals("FAC-104", response.user.teacher?.employeeId)
        assertEquals(30L, response.user.teacher?.id)
        assertNull(response.user.student)
    }

    @Test
    fun `the id number is trimmed before it is stored`() {
        val saved = slot<Student>()
        every { studentRepository.save(capture(saved)) } answers { saved.captured.also { it.id = 20 } }

        service.syncUser(request(idNumber = "  CS-2023-048  "))

        assertEquals("CS-2023-048", saved.captured.rollNumber)
    }

    @Test
    fun `a new account without an id number is rejected and nothing is written`() {
        listOf(null, "", "   ").forEach { blank ->
            val ex = assertThrows<BadRequestException> { service.syncUser(request(idNumber = blank)) }
            assertTrue(ex.message!!.contains("roll number", ignoreCase = true))
        }
        verify(exactly = 0) { userRepository.save(any()) }
        verify(exactly = 0) { studentRepository.save(any()) }
    }

    @Test
    fun `a duplicate roll number is a conflict and nothing is written`() {
        every { studentRepository.existsByRollNumber("CS-2023-048") } returns true

        assertThrows<ConflictException> { service.syncUser(request()) }

        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `a duplicate employee id is a conflict`() {
        every { teacherRepository.existsByEmployeeId("FAC-104") } returns true

        assertThrows<ConflictException> { service.syncUser(request(role = "TEACHER", idNumber = "FAC-104")) }
    }

    @Test
    fun `a duplicate email is a conflict`() {
        every { userRepository.existsByEmail("alex@univ.edu") } returns true

        assertThrows<ConflictException> { service.syncUser(request()) }
    }

    @Test
    fun `admin and unknown roles cannot self-register`() {
        assertThrows<BadRequestException> { service.syncUser(request(role = "ADMIN")) }
        assertThrows<BadRequestException> { service.syncUser(request(role = "PRINCIPAL")) }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    // -- existing accounts -------------------------------------------------------

    private fun existingStudentUser(): User =
        User(id = 10, firebaseUid = "uid-1", email = "alex@univ.edu", firstName = "Alex", lastName = "Morgan", role = UserRole.STUDENT)

    @Test
    fun `an existing user signs in without an id number and keeps the stored role`() {
        val user = existingStudentUser()
        val student = Student(id = 20, user = user, rollNumber = "CS-2023-048", department = "CS", batch = "2023")
        every { userRepository.findByFirebaseUid("uid-1") } returns Optional.of(user)
        every { studentRepository.findByUserId(10) } returns Optional.of(student)

        // The client claims TEACHER; the database is authoritative.
        val response = service.syncUser(request(role = "TEACHER", idNumber = null))

        assertFalse(response.isNewUser)
        assertEquals("STUDENT", response.user.role)
        assertEquals("CS-2023-048", response.user.student?.rollNumber)
        verify(exactly = 0) { studentRepository.save(any()) }
        assertNotNull(user.lastLoginAt)
    }

    @Test
    fun `an existing user with no profile gets one when an id number is supplied`() {
        val user = existingStudentUser()
        every { userRepository.findByFirebaseUid("uid-1") } returns Optional.of(user)
        every { studentRepository.findByUserId(10) } returns Optional.empty()

        val response = service.syncUser(request(idNumber = "CS-2023-048"))

        assertFalse(response.isNewUser)
        assertEquals("CS-2023-048", response.user.student?.rollNumber)
    }

    @Test
    fun `an existing user with no profile and no id number is returned without one`() {
        val user = existingStudentUser()
        every { userRepository.findByFirebaseUid("uid-1") } returns Optional.of(user)
        every { studentRepository.findByUserId(10) } returns Optional.empty()

        val response = service.syncUser(request(idNumber = null))

        assertNull(response.user.student)
        verify(exactly = 0) { studentRepository.save(any()) }
    }
}