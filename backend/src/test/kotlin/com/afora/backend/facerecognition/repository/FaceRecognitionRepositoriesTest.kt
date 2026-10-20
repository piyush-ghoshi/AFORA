package com.afora.backend.facerecognition.repository

import com.afora.backend.facerecognition.entity.FaceConsent
import com.afora.backend.facerecognition.entity.FaceEmbedding
import com.afora.backend.facerecognition.entity.FaceProfile
import com.afora.backend.facerecognition.entity.FaceProfileDeletion
import com.afora.backend.facerecognition.entity.PurgeReason
import com.afora.backend.user.entity.Student
import com.afora.backend.user.entity.User
import com.afora.backend.user.entity.UserRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for task 2.1's four new repositories
 * (`FaceConsentRepository`, `FaceProfileRepository`, `FaceEmbeddingRepository`,
 * `FaceProfileDeletionRepository`) against H2, following the same
 * `@DataJpaTest` + `@ActiveProfiles("test")` pattern already used by
 * `AcademicRepositoryTest` and `AttendanceRepositoryTest`.
 *
 * The test profile runs with `ddl-auto: create-drop` and Flyway disabled
 * (`application-test.yml`), so H2's schema here is generated directly from
 * the JPA entity mappings, not from `V11__create_face_recognition_tables.sql`.
 * The migration's `CHECK (dimensions > 0)` / `CHECK (reason IN (...))`
 * constraints are therefore not present in this test schema (no entity here
 * declares an equivalent `@Check`), and `PurgeReason` is a Kotlin enum, so an
 * out-of-range `reason` cannot even be constructed through the typed
 * repository API. Asserting those two DB-level CHECK constraints would
 * require a native SQL insert bypassing the repositories entirely, which is
 * exactly the schema-agreement question already covered once, as a temporary
 * check, in task 2.1. These tests instead cover the ordinary save/find/delete
 * round trips the four repositories are actually used for.
 */
@DataJpaTest
@ActiveProfiles("test")
class FaceRecognitionRepositoriesTest {

    @Autowired private lateinit var em: TestEntityManager
    @Autowired private lateinit var faceConsentRepository: FaceConsentRepository
    @Autowired private lateinit var faceProfileRepository: FaceProfileRepository
    @Autowired private lateinit var faceEmbeddingRepository: FaceEmbeddingRepository
    @Autowired private lateinit var faceProfileDeletionRepository: FaceProfileDeletionRepository

    private var studentId = 0L
    private var otherStudentId = 0L

    private fun newStudent(uid: String, rollNumber: String): Long {
        val user = em.persistAndFlush(
            User(firebaseUid = uid, email = "$uid@afora.edu", firstName = "F", lastName = "L", role = UserRole.STUDENT)
        )
        val student = em.persistAndFlush(
            Student(user = user, rollNumber = rollNumber, department = "CS", batch = "2024")
        )
        return student.id!!
    }

    @BeforeEach
    fun seed() {
        studentId = newStudent("fr1", "2024CS900")
        otherStudentId = newStudent("fr2", "2024CS901")
        em.clear()
    }

    // -- FaceConsentRepository ------------------------------------------------

    @Test
    fun `FaceConsentRepository saves and finds a consent row by studentId`() {
        val consentedAt = LocalDateTime.of(2026, 1, 1, 10, 0)
        faceConsentRepository.save(FaceConsent(studentId = studentId, consentedAt = consentedAt))
        em.flush()
        em.clear()

        val found = faceConsentRepository.findById(studentId)
        assertTrue(found.isPresent)
        assertEquals(consentedAt, found.get().consentedAt)
    }

    @Test
    fun `FaceConsentRepository existsById is an O(1)-shaped existence check`() {
        assertFalse(faceConsentRepository.existsById(studentId), "no consent recorded yet")

        faceConsentRepository.save(FaceConsent(studentId = studentId, consentedAt = LocalDateTime.now()))
        em.flush()
        em.clear()

        assertTrue(faceConsentRepository.existsById(studentId))
        assertFalse(
            faceConsentRepository.existsById(otherStudentId),
            "existence check must not report a consent recorded for a different student"
        )
    }

    @Test
    fun `FaceConsentRepository deleteById removes exactly that studentId's consent`() {
        faceConsentRepository.save(FaceConsent(studentId = studentId, consentedAt = LocalDateTime.now()))
        faceConsentRepository.save(FaceConsent(studentId = otherStudentId, consentedAt = LocalDateTime.now()))
        em.flush()
        em.clear()

        faceConsentRepository.deleteById(studentId)
        em.flush()
        em.clear()

        assertFalse(faceConsentRepository.existsById(studentId))
        assertTrue(faceConsentRepository.existsById(otherStudentId), "the other student's consent must survive")
    }

    // -- FaceProfileRepository -------------------------------------------------

    @Test
    fun `FaceProfileRepository saves and finds a profile by studentId`() {
        val registeredAt = LocalDateTime.of(2026, 1, 1, 9, 0)
        val updatedAt = LocalDateTime.of(2026, 1, 2, 9, 0)
        faceProfileRepository.save(
            FaceProfile(studentId = studentId, dimensions = 128, registeredAt = registeredAt, updatedAt = updatedAt)
        )
        em.flush()
        em.clear()

        val found = faceProfileRepository.findById(studentId)
        assertTrue(found.isPresent)
        assertEquals(128, found.get().dimensions)
        assertEquals(registeredAt, found.get().registeredAt)
        assertEquals(updatedAt, found.get().updatedAt)
    }

    @Test
    fun `FaceProfileRepository deleteById removes the profile`() {
        faceProfileRepository.save(
            FaceProfile(studentId = studentId, dimensions = 64, registeredAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())
        )
        em.flush()
        em.clear()

        faceProfileRepository.deleteById(studentId)
        em.flush()
        em.clear()

        assertFalse(faceProfileRepository.existsById(studentId))
    }

    // -- FaceEmbeddingRepository -----------------------------------------------

    @Test
    fun `FaceEmbeddingRepository saves and finds embeddings by studentId`() {
        val createdAt = LocalDateTime.of(2026, 1, 1, 8, 0)
        faceEmbeddingRepository.saveAll(
            listOf(
                FaceEmbedding(studentId = studentId, embeddingVector = "0.1,0.2,0.3", dimensions = 3, createdAt = createdAt),
                FaceEmbedding(studentId = studentId, embeddingVector = "0.4,0.5,0.6", dimensions = 3, createdAt = createdAt)
            )
        )
        em.flush()
        em.clear()

        val found = faceEmbeddingRepository.findByStudentId(studentId)
        assertEquals(2, found.size)
        assertTrue(found.all { it.dimensions == 3 })
        assertTrue(found.any { it.embeddingVector == "0.1,0.2,0.3" })
    }

    @Test
    fun `FaceEmbeddingRepository findByStudentIdIn returns rows for every requested student`() {
        faceEmbeddingRepository.save(
            FaceEmbedding(studentId = studentId, embeddingVector = "0.1,0.2", dimensions = 2, createdAt = LocalDateTime.now())
        )
        faceEmbeddingRepository.save(
            FaceEmbedding(studentId = otherStudentId, embeddingVector = "0.3,0.4", dimensions = 2, createdAt = LocalDateTime.now())
        )
        em.flush()
        em.clear()

        val found = faceEmbeddingRepository.findByStudentIdIn(listOf(studentId, otherStudentId))
        assertEquals(2, found.size)
        assertEquals(setOf(studentId, otherStudentId), found.map { it.studentId }.toSet())
    }

    @Test
    fun `FaceEmbeddingRepository deleteByStudentId removes exactly that student's rows and no others`() {
        faceEmbeddingRepository.saveAll(
            listOf(
                FaceEmbedding(studentId = studentId, embeddingVector = "0.1,0.2", dimensions = 2, createdAt = LocalDateTime.now()),
                FaceEmbedding(studentId = studentId, embeddingVector = "0.3,0.4", dimensions = 2, createdAt = LocalDateTime.now())
            )
        )
        faceEmbeddingRepository.save(
            FaceEmbedding(studentId = otherStudentId, embeddingVector = "0.5,0.6", dimensions = 2, createdAt = LocalDateTime.now())
        )
        em.flush()
        em.clear()

        faceEmbeddingRepository.deleteByStudentId(studentId)
        em.flush()
        em.clear()

        assertTrue(faceEmbeddingRepository.findByStudentId(studentId).isEmpty())
        assertEquals(1, faceEmbeddingRepository.findByStudentId(otherStudentId).size)
    }

    @Test
    fun `FaceEmbeddingRepository existsByStudentId reflects whether any embedding is stored`() {
        assertFalse(faceEmbeddingRepository.existsByStudentId(studentId))

        faceEmbeddingRepository.save(
            FaceEmbedding(studentId = studentId, embeddingVector = "0.1,0.2", dimensions = 2, createdAt = LocalDateTime.now())
        )
        em.flush()
        em.clear()

        assertTrue(faceEmbeddingRepository.existsByStudentId(studentId))
        assertFalse(faceEmbeddingRepository.existsByStudentId(otherStudentId))
    }

    // -- FaceProfileDeletionRepository -----------------------------------------

    @Test
    fun `FaceProfileDeletionRepository saves and finds a deletion row`() {
        val deletedAt = LocalDateTime.of(2026, 1, 3, 12, 0)
        val saved = faceProfileDeletionRepository.save(
            FaceProfileDeletion(studentId = studentId, reason = PurgeReason.STUDENT_REQUEST, deletedAt = deletedAt)
        )
        em.flush()
        em.clear()

        val found = faceProfileDeletionRepository.findById(saved.id)
        assertTrue(found.isPresent)
        assertEquals(studentId, found.get().studentId)
        assertEquals(PurgeReason.STUDENT_REQUEST, found.get().reason)
        assertEquals(deletedAt, found.get().deletedAt)
    }

    @Test
    fun `FaceProfileDeletionRepository findByStudentId returns every deletion row for that student, append-only`() {
        faceProfileDeletionRepository.save(
            FaceProfileDeletion(studentId = studentId, reason = PurgeReason.ADMIN_REQUEST, deletedAt = LocalDateTime.now())
        )
        faceProfileDeletionRepository.save(
            FaceProfileDeletion(studentId = studentId, reason = PurgeReason.ENROLLMENT_ENDED, deletedAt = LocalDateTime.now())
        )
        faceProfileDeletionRepository.save(
            FaceProfileDeletion(studentId = otherStudentId, reason = PurgeReason.STUDENT_REQUEST, deletedAt = LocalDateTime.now())
        )
        em.flush()
        em.clear()

        val found = faceProfileDeletionRepository.findByStudentId(studentId)
        assertEquals(2, found.size)
        assertEquals(
            setOf(PurgeReason.ADMIN_REQUEST, PurgeReason.ENROLLMENT_ENDED),
            found.map { it.reason }.toSet()
        )
    }

    @Test
    fun `FaceProfileDeletionRepository rows are never updated in place by this feature's own usage`() {
        // Append-only (R4.4): there is no update-by-criteria method on the repository
        // beyond what JpaRepository provides, and the service never calls save() with
        // an existing generated id. This just confirms two independent deletion events
        // for the same student produce two distinct rows, not one overwritten row.
        val first = faceProfileDeletionRepository.save(
            FaceProfileDeletion(studentId = studentId, reason = PurgeReason.STUDENT_REQUEST, deletedAt = LocalDateTime.now())
        )
        val second = faceProfileDeletionRepository.save(
            FaceProfileDeletion(studentId = studentId, reason = PurgeReason.ADMIN_REQUEST, deletedAt = LocalDateTime.now())
        )
        em.flush()
        em.clear()

        assertTrue(first.id != second.id)
        assertEquals(2, faceProfileDeletionRepository.findByStudentId(studentId).size)
    }

    @Test
    fun `an unknown studentId yields no rows across the four repositories`() {
        val unknown = 999_999L
        assertFalse(faceConsentRepository.existsById(unknown))
        assertNull(faceProfileRepository.findById(unknown).orElse(null))
        assertTrue(faceEmbeddingRepository.findByStudentId(unknown).isEmpty())
        assertTrue(faceProfileDeletionRepository.findByStudentId(unknown).isEmpty())
    }
}
