package com.afora.backend.academic.repository

import com.afora.backend.academic.entity.ClassSection
import com.afora.backend.academic.entity.Enrollment
import com.afora.backend.academic.entity.EnrollmentStatus
import com.afora.backend.academic.entity.Subject
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
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for the Phase A8.1 academic repositories.
 *
 * Runs against H2 (test profile) with Hibernate generating the schema from
 * the entity mappings. This exercises the custom JPQL queries for real
 * rather than only checking that they parse at bootstrap.
 */
@DataJpaTest
@ActiveProfiles("test")
class AcademicRepositoryTest {

    @Autowired private lateinit var em: TestEntityManager
    @Autowired private lateinit var subjectRepository: SubjectRepository
    @Autowired private lateinit var classSectionRepository: ClassSectionRepository
    @Autowired private lateinit var enrollmentRepository: EnrollmentRepository

    private val semesterId: UUID = UUID.fromString("11111111-2222-3333-4444-555555555555")

    private var teacherId: Long = 0
    private var studentId: Long = 0
    private var otherStudentId: Long = 0
    private var sectionId: Long = 0
    private var subjectId: Long = 0

    @BeforeEach
    fun seed() {
        // Teacher
        val teacherUser = em.persistAndFlush(
            User(
                firebaseUid = "uid_teacher",
                email = "teacher@afora.edu",
                firstName = "Ada",
                lastName = "Lovelace",
                role = UserRole.TEACHER
            )
        )
        val teacher = em.persistAndFlush(
            Teacher(
                user = teacherUser,
                employeeId = "EMP001",
                department = "Computer Science",
                designation = "Professor"
            )
        )
        teacherId = teacher.id!!

        // Enrolled student
        val studentUser = em.persistAndFlush(
            User(
                firebaseUid = "uid_student",
                email = "student@afora.edu",
                firstName = "Alan",
                lastName = "Turing",
                role = UserRole.STUDENT
            )
        )
        val student = em.persistAndFlush(
            Student(
                user = studentUser,
                rollNumber = "CS2024001",
                department = "Computer Science",
                batch = "2024"
            )
        )
        studentId = student.id!!

        // A second student used to prove status filtering
        val otherUser = em.persistAndFlush(
            User(
                firebaseUid = "uid_student2",
                email = "student2@afora.edu",
                firstName = "Grace",
                lastName = "Hopper",
                role = UserRole.STUDENT
            )
        )
        val otherStudent = em.persistAndFlush(
            Student(
                user = otherUser,
                rollNumber = "CS2024002",
                department = "Computer Science",
                batch = "2024"
            )
        )
        otherStudentId = otherStudent.id!!

        // Subject + section
        val subject = em.persistAndFlush(
            Subject(
                code = "CS101",
                name = "Data Structures",
                credits = 4,
                department = "Computer Science"
            )
        )
        subjectId = subject.id

        val section = em.persistAndFlush(
            ClassSection(
                name = "CS101 Section A",
                subject = subject,
                teacher = teacher,
                semesterId = semesterId,
                batch = "2024",
                section = "A",
                roomNumber = "A-101"
            )
        )
        sectionId = section.id

        // Active enrollment for student, DROPPED for otherStudent
        em.persistAndFlush(
            Enrollment(student = student, classSection = section, status = EnrollmentStatus.ACTIVE)
        )
        em.persistAndFlush(
            Enrollment(student = otherStudent, classSection = section, status = EnrollmentStatus.DROPPED)
        )
        em.clear()
    }

    // -- SubjectRepository ---------------------------------------------------

    @Test
    fun `findByCode returns the subject`() {
        val found = subjectRepository.findByCode("CS101")
        assertNotNull(found)
        assertEquals("Data Structures", found.name)
        assertEquals(4, found.credits)
    }

    @Test
    fun `findByCode returns null for unknown code`() {
        assertEquals(null, subjectRepository.findByCode("NOPE999"))
    }

    @Test
    fun `findByDepartmentAndIsActiveTrue returns active subjects`() {
        val found = subjectRepository.findByDepartmentAndIsActiveTrue("Computer Science")
        assertEquals(1, found.size)
        assertEquals("CS101", found.first().code)
    }

    // -- ClassSectionRepository ----------------------------------------------

    @Test
    fun `findByTeacherIdAndIsActiveTrue returns the teachers section`() {
        val sections = classSectionRepository.findByTeacherIdAndIsActiveTrue(teacherId)
        assertEquals(1, sections.size)
        assertEquals("CS101 Section A", sections.first().name)
    }

    @Test
    fun `findBySubjectId returns sections for the subject`() {
        assertEquals(1, classSectionRepository.findBySubjectId(subjectId).size)
    }

    @Test
    fun `findBySemesterId returns sections in the semester`() {
        assertEquals(1, classSectionRepository.findBySemesterId(semesterId).size)
    }

    @Test
    fun `findActiveByStudentId returns sections the student is actively enrolled in`() {
        val sections = classSectionRepository.findActiveByStudentId(studentId)
        assertEquals(1, sections.size)
        assertEquals(sectionId, sections.first().id)
    }

    @Test
    fun `findActiveByStudentId excludes dropped enrollments`() {
        val sections = classSectionRepository.findActiveByStudentId(otherStudentId)
        assertTrue(sections.isEmpty(), "DROPPED enrollment must not be returned")
    }

    // -- EnrollmentRepository ------------------------------------------------

    @Test
    fun `countActiveEnrollmentsByStudentId counts only active`() {
        assertEquals(1L, enrollmentRepository.countActiveEnrollmentsByStudentId(studentId))
        assertEquals(0L, enrollmentRepository.countActiveEnrollmentsByStudentId(otherStudentId))
    }

    @Test
    fun `countByClassSectionIdAndStatus separates active from dropped`() {
        assertEquals(1, enrollmentRepository.countByClassSectionIdAndStatus(sectionId, EnrollmentStatus.ACTIVE))
        assertEquals(1, enrollmentRepository.countByClassSectionIdAndStatus(sectionId, EnrollmentStatus.DROPPED))
        assertEquals(0, enrollmentRepository.countByClassSectionIdAndStatus(sectionId, EnrollmentStatus.COMPLETED))
    }

    @Test
    fun `existsByStudentIdAndClassSectionId reflects enrollment`() {
        assertTrue(enrollmentRepository.existsByStudentIdAndClassSectionId(studentId, sectionId))
        assertFalse(enrollmentRepository.existsByStudentIdAndClassSectionId(999_999L, sectionId))
    }

    @Test
    fun `findByStudentIdAndStatus filters by status`() {
        assertEquals(1, enrollmentRepository.findByStudentIdAndStatus(studentId, EnrollmentStatus.ACTIVE).size)
        assertEquals(0, enrollmentRepository.findByStudentIdAndStatus(studentId, EnrollmentStatus.DROPPED).size)
    }

    @Test
    fun `findByClassSectionId returns all enrollments regardless of status`() {
        assertEquals(2, enrollmentRepository.findByClassSectionId(sectionId).size)
    }
}