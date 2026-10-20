package com.afora.backend.facerecognition.service

import com.afora.backend.academic.entity.ClassSection
import com.afora.backend.academic.entity.Enrollment
import com.afora.backend.academic.entity.EnrollmentStatus
import com.afora.backend.academic.entity.Subject
import com.afora.backend.academic.repository.EnrollmentRepository
import com.afora.backend.facerecognition.entity.FaceEmbedding
import com.afora.backend.facerecognition.repository.FaceConsentRepository
import com.afora.backend.facerecognition.repository.FaceEmbeddingRepository
import com.afora.backend.facerecognition.repository.FaceProfileDeletionRepository
import com.afora.backend.facerecognition.repository.FaceProfileRepository
import com.afora.backend.user.entity.Student
import com.afora.backend.user.entity.Teacher
import com.afora.backend.user.entity.User
import com.afora.backend.user.entity.UserRole
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import java.time.Clock
import java.util.UUID
import kotlin.test.assertEquals

/**
 * **Validates: Requirements 5.1, 5.2, 5.3, 5.8**
 *
 * Exercises [FaceProfileService.enrolledEmbeddingSet] against a real H2 database,
 * following the same `@DataJpaTest` + `@ActiveProfiles("test")` pattern already used by
 * `FaceRecognitionRepositoriesTest.kt` and the sibling `AttendanceRepositoryTest.kt` /
 * `AcademicRepositoryTest.kt` (the latter is where the `Enrollment`-with-different-
 * `EnrollmentStatus` seeding idiom via `TestEntityManager.persistAndFlush` is copied
 * from). `FaceProfileService` itself is instantiated directly from the `@DataJpaTest`-
 * autowired repository beans plus a real [Clock], exactly as
 * `FaceProfileServiceDeletionAtomicityTest.kt` already does, so this is deliberately not
 * a MockK-based unit test: the property under test is about the real interaction between
 * [EnrollmentRepository.findActiveRosterByClassSectionId] and [FaceEmbeddingRepository],
 * which a mocked repository cannot exercise.
 *
 * **On running the real assembly logic against H2 one hundred times per test:** each
 * `checkAll` iteration builds a brand-new, uniquely-identified `ClassSection` (a fresh
 * random `semesterId` for every iteration keeps the entity's own
 * `(subject_id, semester_id, section)` unique constraint satisfied) plus a small number
 * of brand-new students, rather than reusing and tearing down one fixed roster between
 * iterations. That keeps every iteration fully isolated from every other -- no rollback,
 * no cleanup step, and no risk of one iteration's leftover rows leaking into another's
 * assertions -- at the cost of the H2 database accumulating rows across the whole test
 * run. With the roster generator capped at 6 students per iteration (see [rosterArb]),
 * one hundred iterations insert at most a few hundred small rows total, which H2
 * (in-memory, same JVM) handles in well under a second per iteration; the test method
 * itself still runs inside one `@DataJpaTest` transaction that rolls back afterwards, so
 * nothing persists beyond this test. 100 iterations is therefore kept as specified in the
 * design's property-testing convention (Binding Convention 10) rather than reduced.
 */
@DataJpaTest
@ActiveProfiles("test")
class EnrolledEmbeddingSetPropertyTest {

    @Autowired private lateinit var em: TestEntityManager
    @Autowired private lateinit var faceConsentRepository: FaceConsentRepository
    @Autowired private lateinit var faceProfileRepository: FaceProfileRepository
    @Autowired private lateinit var faceEmbeddingRepository: FaceEmbeddingRepository
    @Autowired private lateinit var faceProfileDeletionRepository: FaceProfileDeletionRepository
    @Autowired private lateinit var enrollmentRepository: EnrollmentRepository

    private val clock: Clock = Clock.systemDefaultZone()

    private lateinit var teacher: Teacher
    private lateinit var subject: Subject
    private var iterationCounter = 0

    private fun service() = FaceProfileService(
        faceConsentRepository,
        faceProfileRepository,
        faceEmbeddingRepository,
        faceProfileDeletionRepository,
        enrollmentRepository,
        clock
    )

    /** One ACTIVE/DROPPED/COMPLETED enrollment, carrying zero or more Face_Embedding rows. */
    private data class StudentCase(val status: EnrollmentStatus, val embeddingCount: Int)

    private val studentCaseArb: Arb<StudentCase> = Arb.bind(
        Arb.enum<EnrollmentStatus>(),
        Arb.int(0..3)
    ) { status, embeddingCount -> StudentCase(status, embeddingCount) }

    /** 0..6 students per class section, deliberately including the empty roster (R5.8). */
    private val rosterArb: Arb<List<StudentCase>> = Arb.list(studentCaseArb, 0..6)

    @BeforeEach
    fun seedSharedFixtures() {
        val teacherUser = em.persistAndFlush(
            User(
                firebaseUid = "uid_teacher_eeset",
                email = "teacher_eeset@afora.edu",
                firstName = "Eeset",
                lastName = "Teacher",
                role = UserRole.TEACHER
            )
        )
        teacher = em.persistAndFlush(
            Teacher(user = teacherUser, employeeId = "EMP_EESET", department = "CS", designation = "Professor")
        )
        subject = em.persistAndFlush(
            Subject(code = "EESET101", name = "Enrolled Embedding Set Subject", credits = 3, department = "CS")
        )
    }

    // Feature: face-recognition, Property 2: The Enrolled_Embedding_Set is exactly the ACTIVE-enrolled, embedding-holding student set
    @Test
    fun `enrolled embedding set equals exactly the ACTIVE-and-has-embeddings subset`(): Unit = runBlocking {
        checkAll(PropTestConfig(iterations = 100), rosterArb) { roster ->
            val iteration = iterationCounter++

            val section = em.persist(
                ClassSection(
                    name = "Eeset Section $iteration",
                    subject = subject,
                    teacher = teacher,
                    semesterId = UUID.randomUUID(),
                    batch = "2025",
                    section = "S$iteration",
                    roomNumber = "R1"
                )
            )

            // studentId -> number of Face_Embedding rows this student should expect to be
            // reported with, restricted to the ACTIVE-and-has-embeddings subset (R5.1-R5.3).
            val expectedEntries = mutableMapOf<Long, Int>()

            roster.forEachIndexed { index, case ->
                val suffix = "${iteration}_$index"
                val user = em.persist(
                    User(
                        firebaseUid = "uid_eeset_$suffix",
                        email = "eeset_$suffix@afora.edu",
                        firstName = "F$suffix",
                        lastName = "L$suffix",
                        role = UserRole.STUDENT
                    )
                )
                val student = em.persist(
                    Student(user = user, rollNumber = "R_EESET_$suffix", department = "CS", batch = "2025")
                )
                val studentId = student.id!!

                em.persist(Enrollment(student = student, classSection = section, status = case.status))

                repeat(case.embeddingCount) { vectorIndex ->
                    em.persist(
                        FaceEmbedding(
                            studentId = studentId,
                            embeddingVector = "0.${vectorIndex + 1},0.${vectorIndex + 2}",
                            dimensions = 2
                        )
                    )
                }

                if (case.status == EnrollmentStatus.ACTIVE && case.embeddingCount > 0) {
                    expectedEntries[studentId] = case.embeddingCount // R5.1
                }
                // DROPPED/COMPLETED (R5.2) and ACTIVE-with-zero-embeddings (R5.3) students
                // are deliberately left out of expectedEntries.
            }

            em.flush()
            em.clear()

            val result = service().enrolledEmbeddingSet(section.id)

            assertEquals(
                expectedEntries.keys,
                result.entries.map { it.studentId }.toSet(),
                "the Enrolled_Embedding_Set must contain exactly the ACTIVE-and-has-embeddings student set"
            )
            result.entries.forEach { entry ->
                assertEquals(
                    expectedEntries.getValue(entry.studentId),
                    entry.embeddings.size,
                    "every reported entry must carry all of that student's embedding vectors"
                )
            }
            // R5.8: an empty qualifying subset still yields entries = [], never null.
            if (expectedEntries.isEmpty()) {
                assertEquals(emptyList(), result.entries)
            }

            // Settled Decision B: the three thresholds are always included, unconditionally.
            assertEquals(FaceMatchingThresholds.HIGH_CONFIDENCE, result.highConfidenceThreshold)
            assertEquals(FaceMatchingThresholds.MEDIUM_CONFIDENCE, result.mediumConfidenceThreshold)
            assertEquals(FaceMatchingThresholds.UNMATCHED_FLOOR, result.unmatchedFloorThreshold)
        }
    }
}
