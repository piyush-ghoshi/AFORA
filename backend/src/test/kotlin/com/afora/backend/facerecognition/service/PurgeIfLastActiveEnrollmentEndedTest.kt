package com.afora.backend.facerecognition.service

import com.afora.backend.academic.repository.EnrollmentRepository
import com.afora.backend.facerecognition.entity.FaceProfileDeletion
import com.afora.backend.facerecognition.entity.PurgeReason
import com.afora.backend.facerecognition.repository.FaceConsentRepository
import com.afora.backend.facerecognition.repository.FaceEmbeddingRepository
import com.afora.backend.facerecognition.repository.FaceProfileDeletionRepository
import com.afora.backend.facerecognition.repository.FaceProfileRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

/**
 * Task 6.4 (design.md Settled Decision C, "How this is tested", second paragraph):
 * `FaceProfileService.purgeIfLastActiveEnrollmentEnded(studentId)` gets exactly the one
 * narrow unit test Decision C describes -- given a studentId with zero remaining ACTIVE
 * enrollments it purges, given at least one remaining ACTIVE enrollment it is a no-op --
 * and nothing more.
 *
 * **Stated explicitly, matching Decision C's own stated gap:** this test cannot prove
 * requirement 4.1's "within the same database transaction" clause. Requirement 4.1
 * requires the purge to run in the same transaction as a Student's Enrollment status
 * change that results in zero remaining ACTIVE Enrollment rows, but as design.md's
 * Settled Decision C documents, no code anywhere in this codebase today writes
 * `Enrollment.status` -- `EnrollmentRepository` is read-only in every production path,
 * and `purgeIfLastActiveEnrollmentEnded` has zero call sites in the current codebase. A
 * unit test instantiating `FaceProfileService` directly and calling this one method
 * cannot fabricate the "called inline from inside an enrollment-mutating
 * `@Transactional` method" shape requirement 4.1 describes, because that caller does not
 * exist yet. This test asserts only the method's own purge/no-op branching logic in
 * isolation (R4.1's zero-remaining-ACTIVE condition, R4.5's no-op condition); it makes no
 * claim about transactional co-location with an enrollment status change. That gap closes
 * only when a future spec implements enrollment mutation and wires the call, per
 * Decision C's own binding contract on [FaceProfileService.purgeIfLastActiveEnrollmentEnded].
 *
 * Plain MockK-based unit test, not `@DataJpaTest` -- the behavior under test is pure
 * branching on [EnrollmentRepository.countActiveEnrollmentsByStudentId]'s return value,
 * so mocking all five repository dependencies is simpler than standing up a real
 * database for this one method, and matches the sibling atomicity test's own observation
 * that `deleteProfile()`'s actual persistence behavior is already covered elsewhere
 * (`FaceProfileServiceDeletionAtomicityTest`). Delegation to [FaceProfileService.deleteProfile]
 * is observed indirectly, through the same four repository writes that method performs,
 * since `deleteProfile` is a concrete method on the class under test rather than an
 * injected collaborator that could be mocked directly.
 */
class PurgeIfLastActiveEnrollmentEndedTest {

    private val studentId = 77L
    private val fixedClock: Clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC)

    private fun buildService(
        faceConsentRepository: FaceConsentRepository = mockk(relaxed = true),
        faceProfileRepository: FaceProfileRepository = mockk(relaxed = true),
        faceEmbeddingRepository: FaceEmbeddingRepository = mockk(relaxed = true),
        faceProfileDeletionRepository: FaceProfileDeletionRepository = mockk(relaxed = true),
        enrollmentRepository: EnrollmentRepository = mockk()
    ) = FaceProfileService(
        faceConsentRepository = faceConsentRepository,
        faceProfileRepository = faceProfileRepository,
        faceEmbeddingRepository = faceEmbeddingRepository,
        faceProfileDeletionRepository = faceProfileDeletionRepository,
        enrollmentRepository = enrollmentRepository,
        clock = fixedClock
    )

    @Test
    fun `zero remaining ACTIVE enrollments triggers a purge with reason ENROLLMENT_ENDED`() {
        val faceConsentRepository = mockk<FaceConsentRepository>(relaxed = true)
        val faceProfileRepository = mockk<FaceProfileRepository>(relaxed = true)
        val faceEmbeddingRepository = mockk<FaceEmbeddingRepository>(relaxed = true)
        val faceProfileDeletionRepository = mockk<FaceProfileDeletionRepository>(relaxed = true)
        val enrollmentRepository = mockk<EnrollmentRepository>()
        every { enrollmentRepository.countActiveEnrollmentsByStudentId(studentId) } returns 0L

        val savedDeletion = slot<FaceProfileDeletion>()
        every { faceProfileDeletionRepository.save(capture(savedDeletion)) } answers { firstArg() }

        val service = buildService(
            faceConsentRepository = faceConsentRepository,
            faceProfileRepository = faceProfileRepository,
            faceEmbeddingRepository = faceEmbeddingRepository,
            faceProfileDeletionRepository = faceProfileDeletionRepository,
            enrollmentRepository = enrollmentRepository
        )

        service.purgeIfLastActiveEnrollmentEnded(studentId)

        verify(exactly = 1) { faceEmbeddingRepository.deleteByStudentId(studentId) }
        verify(exactly = 1) { faceConsentRepository.deleteById(studentId) }
        verify(exactly = 1) { faceProfileRepository.deleteById(studentId) }
        verify(exactly = 1) { faceProfileDeletionRepository.save(any()) }
        assertEquals(PurgeReason.ENROLLMENT_ENDED, savedDeletion.captured.reason, "R4.1")
        assertEquals(studentId, savedDeletion.captured.studentId)
    }

    @Test
    fun `at least one remaining ACTIVE enrollment is a no-op`() {
        val faceConsentRepository = mockk<FaceConsentRepository>(relaxed = true)
        val faceProfileRepository = mockk<FaceProfileRepository>(relaxed = true)
        val faceEmbeddingRepository = mockk<FaceEmbeddingRepository>(relaxed = true)
        val faceProfileDeletionRepository = mockk<FaceProfileDeletionRepository>(relaxed = true)
        val enrollmentRepository = mockk<EnrollmentRepository>()
        every { enrollmentRepository.countActiveEnrollmentsByStudentId(studentId) } returns 1L

        val service = buildService(
            faceConsentRepository = faceConsentRepository,
            faceProfileRepository = faceProfileRepository,
            faceEmbeddingRepository = faceEmbeddingRepository,
            faceProfileDeletionRepository = faceProfileDeletionRepository,
            enrollmentRepository = enrollmentRepository
        )

        service.purgeIfLastActiveEnrollmentEnded(studentId)

        verify(exactly = 0) { faceEmbeddingRepository.deleteByStudentId(any()) }
        verify(exactly = 0) { faceConsentRepository.deleteById(any()) }
        verify(exactly = 0) { faceProfileRepository.deleteById(any()) }
        verify(exactly = 0) { faceProfileDeletionRepository.save(any()) }
    }
}
