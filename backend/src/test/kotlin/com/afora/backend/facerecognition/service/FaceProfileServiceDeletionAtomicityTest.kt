package com.afora.backend.facerecognition.service

import com.afora.backend.academic.repository.EnrollmentRepository
import com.afora.backend.facerecognition.entity.FaceConsent
import com.afora.backend.facerecognition.entity.FaceEmbedding
import com.afora.backend.facerecognition.entity.FaceProfile
import com.afora.backend.facerecognition.entity.PurgeReason
import com.afora.backend.facerecognition.repository.FaceConsentRepository
import com.afora.backend.facerecognition.repository.FaceEmbeddingRepository
import com.afora.backend.facerecognition.repository.FaceProfileDeletionRepository
import com.afora.backend.facerecognition.repository.FaceProfileRepository
import com.afora.backend.user.entity.Student
import com.afora.backend.user.entity.User
import com.afora.backend.user.entity.UserRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.transaction.TestTransaction
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.time.Clock
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests Settled Decision C's first "How this is tested" bullet
 * (`.kiro/specs/face-recognition/design.md`): `FaceProfileService.deleteProfile()` is
 * exercised through the two call sites that exist today -- a student's own deletion
 * request (R4.2, `PurgeReason.STUDENT_REQUEST`) and an admin's deletion request naming a
 * student (R4.6, R4.7, `PurgeReason.ADMIN_REQUEST`) -- asserting that all four writes
 * (`face_embeddings` deleted, `face_consents` deleted (R4.3), `face_profiles` deleted, one
 * `face_profile_deletions` row inserted with the correct reason (R4.4)) happen together, or
 * none do.
 *
 * Same `@DataJpaTest` plus real-transaction technique already used by
 * `AttendanceRepositoryTest`/`AcademicRepositoryTest` against H2. `FaceProfileService` is
 * never pulled in as a Spring bean here -- it is instantiated directly in each test method
 * from the `@DataJpaTest`-autowired repository beans (plus a real [Clock]), so its own
 * `@Transactional` methods join the test's already-open database transaction exactly as
 * they would in production, with no extra Spring configuration needed to get there.
 *
 * The "induced failure after the first write" case does not use a mocking framework at
 * all -- `deleteProfile()`'s first write is `FaceEmbeddingRepository.deleteByStudentId`,
 * its second is `FaceConsentRepository.deleteById`, so [throwOnConsentDeleteRepository]
 * builds a `java.lang.reflect.Proxy`-backed decorator around the real, autowired
 * [FaceConsentRepository] bean: every call is forwarded to the real repository (so the
 * first write still genuinely happens), and only `deleteById` additionally throws
 * afterwards. A [FaceProfileService] built with that decorator in place of the real
 * [FaceConsentRepository] then fails partway through its own `@Transactional` method,
 * exactly the shape Settled Decision C's test plan describes.
 *
 * `TestTransaction.end()`/`TestTransaction.start()`
 * (`org.springframework.test.context.transaction.TestTransaction`) force a genuine
 * rollback against the database and open a fresh transaction/persistence context before
 * the induced-failure test re-reads the four tables -- `deleteProfile()`'s own
 * `@Transactional` joins the already-open per-test transaction (default `REQUIRED`
 * propagation), so merely catching the thrown exception and re-querying within that same
 * still-open transaction would only prove the in-memory Hibernate session's view, not that
 * the database itself rolled back.
 */
@DataJpaTest
@ActiveProfiles("test")
class FaceProfileServiceDeletionAtomicityTest {

    /**
     * Builds a [FaceConsentRepository] that forwards every call to [delegate] unchanged,
     * except `deleteById`, which performs the real delegate call first -- so the write
     * genuinely happens -- and then throws, simulating a failure that strikes partway
     * through `deleteProfile()`'s sequence of writes, immediately after the first write
     * (`face_embeddings` deletion) already completed.
     *
     * A `java.lang.reflect.Proxy` is used rather than Kotlin interface-delegation (`by`)
     * because `FaceConsentRepository` inherits `deleteById` from the generic
     * `CrudRepository<T, ID>`, and Kotlin's own delegation codegen for that generic,
     * bridge-method-bearing signature conflicts with an explicit `override` of the same
     * method.
     */
    private fun throwOnConsentDeleteRepository(delegate: FaceConsentRepository): FaceConsentRepository {
        val handler = InvocationHandler { _, method: Method, args: Array<Any?>? ->
            val result = method.invoke(delegate, *(args ?: emptyArray()))
            if (method.name == "deleteById") {
                throw RuntimeException("Induced failure immediately after the first write for the atomicity test")
            }
            result
        }
        return Proxy.newProxyInstance(
            FaceConsentRepository::class.java.classLoader,
            arrayOf(FaceConsentRepository::class.java),
            handler
        ) as FaceConsentRepository
    }

    @Autowired private lateinit var em: TestEntityManager
    @Autowired private lateinit var faceConsentRepository: FaceConsentRepository
    @Autowired private lateinit var faceProfileRepository: FaceProfileRepository
    @Autowired private lateinit var faceEmbeddingRepository: FaceEmbeddingRepository
    @Autowired private lateinit var faceProfileDeletionRepository: FaceProfileDeletionRepository
    @Autowired private lateinit var enrollmentRepository: EnrollmentRepository

    private val clock: Clock = Clock.systemDefaultZone()
    private var studentId: Long = 0

    private fun faceProfileService(consentRepository: FaceConsentRepository = faceConsentRepository) =
        FaceProfileService(
            consentRepository,
            faceProfileRepository,
            faceEmbeddingRepository,
            faceProfileDeletionRepository,
            enrollmentRepository,
            clock
        )

    @BeforeEach
    fun seed() {
        val user = em.persistAndFlush(
            User(
                firebaseUid = "uid_atomicity",
                email = "atomicity@afora.edu",
                firstName = "Ada",
                lastName = "Test",
                role = UserRole.STUDENT
            )
        )
        val student = em.persistAndFlush(
            Student(user = user, rollNumber = "2025CS999", department = "CS", batch = "2025")
        )
        studentId = student.id!!

        em.persistAndFlush(FaceConsent(studentId = studentId))
        em.persistAndFlush(FaceProfile(studentId = studentId, dimensions = 2))
        em.persistAndFlush(FaceEmbedding(studentId = studentId, embeddingVector = "0.1,0.2", dimensions = 2))
        em.flush()
        em.clear()
    }

    @Test
    fun `student self-delete removes all four writes together`() {
        faceProfileService().deleteProfile(studentId, PurgeReason.STUDENT_REQUEST)

        assertAllFourWritesHappened(PurgeReason.STUDENT_REQUEST)
    }

    @Test
    fun `admin-named delete removes all four writes together`() {
        faceProfileService().deleteProfile(studentId, PurgeReason.ADMIN_REQUEST)

        assertAllFourWritesHappened(PurgeReason.ADMIN_REQUEST)
    }

    @Test
    fun `an induced failure after the first write leaves nothing partial once the transaction rolls back`() {
        // Commit the @BeforeEach seed data on its own, into a fresh transaction, before the
        // write under test runs. Otherwise the rollback below -- which must genuinely hit
        // the database to prove anything -- would also wipe out the seed rows themselves
        // (they were inserted in this same test's one open transaction), making every
        // "survived" assertion vacuously true for the wrong reason.
        TestTransaction.flagForCommit()
        TestTransaction.end()
        TestTransaction.start()

        try {
            val serviceWithFailingConsentDelete =
                faceProfileService(throwOnConsentDeleteRepository(faceConsentRepository))

            assertThrows<RuntimeException> {
                serviceWithFailingConsentDelete.deleteProfile(studentId, PurgeReason.STUDENT_REQUEST)
            }

            // Force a genuine rollback against the database and start a fresh transaction/
            // persistence context before re-reading -- deleteProfile()'s writes joined this
            // test's own open transaction, so reading it without ending that transaction
            // first would only reflect the uncommitted in-memory session, not a real
            // rollback.
            TestTransaction.flagForRollback()
            TestTransaction.end()
            TestTransaction.start()

            assertTrue(
                faceEmbeddingRepository.findByStudentId(studentId).isNotEmpty(),
                "face_embeddings must survive an induced failure that rolled back the transaction"
            )
            assertTrue(
                faceConsentRepository.findById(studentId).isPresent,
                "face_consents must survive an induced failure that rolled back the transaction"
            )
            assertTrue(
                faceProfileRepository.findById(studentId).isPresent,
                "face_profiles must survive an induced failure that rolled back the transaction"
            )
            assertTrue(
                faceProfileDeletionRepository.findByStudentId(studentId).isEmpty(),
                "no face_profile_deletions row may be inserted when the transaction rolled back"
            )
        } finally {
            // The seed data was committed above (outside the normal @DataJpaTest
            // per-test rollback) specifically so the rollback-proof above is genuine. Clean
            // it back up here, in its own committed transaction, so this test leaves no
            // residue for any later test method reusing the same H2 database/Spring
            // context.
            faceEmbeddingRepository.deleteByStudentId(studentId)
            faceConsentRepository.deleteById(studentId)
            faceProfileRepository.deleteById(studentId)
            faceProfileDeletionRepository.deleteAll(faceProfileDeletionRepository.findByStudentId(studentId))
            em.entityManager.createQuery("DELETE FROM Student s WHERE s.id = :id")
                .setParameter("id", studentId)
                .executeUpdate()
            em.entityManager.createQuery("DELETE FROM User u WHERE u.firebaseUid = :uid")
                .setParameter("uid", "uid_atomicity")
                .executeUpdate()
            em.flush()
            TestTransaction.flagForCommit()
            TestTransaction.end()
            TestTransaction.start()
        }
    }

    private fun assertAllFourWritesHappened(expectedReason: PurgeReason) {
        em.flush()
        em.clear()

        assertTrue(faceEmbeddingRepository.findByStudentId(studentId).isEmpty(), "face_embeddings must be deleted")
        assertTrue(faceConsentRepository.findById(studentId).isEmpty, "face_consents must be deleted (R4.3)")
        assertTrue(faceProfileRepository.findById(studentId).isEmpty, "face_profiles must be deleted")

        val deletions = faceProfileDeletionRepository.findByStudentId(studentId)
        assertEquals(1, deletions.size, "exactly one face_profile_deletions row must be inserted (R4.4)")
        assertEquals(expectedReason, deletions.single().reason)
    }
}
