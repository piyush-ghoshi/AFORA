package com.afora.backend.facerecognition.service

import com.afora.backend.academic.repository.EnrollmentRepository
import com.afora.backend.common.exception.BadRequestException
import com.afora.backend.common.exception.ForbiddenException
import com.afora.backend.facerecognition.entity.FaceEmbedding
import com.afora.backend.facerecognition.entity.FaceProfile
import com.afora.backend.facerecognition.repository.FaceConsentRepository
import com.afora.backend.facerecognition.repository.FaceEmbeddingRepository
import com.afora.backend.facerecognition.repository.FaceProfileDeletionRepository
import com.afora.backend.facerecognition.repository.FaceProfileRepository
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.orNull
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * **Validates: Requirements 2.4, 2.6, 2.7**
 *
 * Exercises [FaceProfileService.register] following the sibling attendance-marking
 * plan's own property-testing harness pattern: `kotest-property` 5.8.0 used as a
 * library, `checkAll` invoked from a plain JUnit test inside `runBlocking { }`, and
 * MockK-only repository mocks.
 */
class FaceProfileRegistrationPropertyTest {

    private val studentId = 42L
    private val fixedClock: Clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC)
    private val dimensionArb: Arb<Int> = Arb.int(1..6)
    private val vectorArb: Arb<Double> = Arb.double(-1.0, 1.0)

    /** One registration request: a flag picking consistent-vs-inconsistent dimensionality,
     * a random base dimension, a vector count, and an optional pre-existing profile of its
     * own (possibly different) dimension. */
    private data class RegistrationCase(
        val consistentDimensionality: Boolean,
        val baseDimension: Int,
        val vectorCount: Int,
        val existingProfileDimension: Int?
    )

    private val registrationCaseArb: Arb<RegistrationCase> = Arb.bind(
        Arb.boolean(),
        dimensionArb,
        Arb.int(2..5),
        dimensionArb.orNull(nullProbability = 0.25)
    ) { consistent, baseDimension, vectorCount, existingDimension ->
        RegistrationCase(consistent, baseDimension, vectorCount, existingDimension)
    }

    /** Builds the submitted vectors for [case]: all sharing [RegistrationCase.baseDimension]
     * when consistent, or deliberately varying dimension-by-index when not. */
    private fun buildEmbeddings(case: RegistrationCase): List<List<Double>> =
        List(case.vectorCount) { index ->
            val dimension = if (case.consistentDimensionality) {
                case.baseDimension
            } else {
                // Guarantees at least two distinct dimensionalities across the request.
                case.baseDimension + (index % 2)
            }
            List(dimension) { 0.1 * (it + 1) }
        }

    private fun buildService(
        existingProfile: FaceProfile?,
        savedEmbeddings: MutableList<List<FaceEmbedding>>
    ): FaceProfileService {
        val faceConsentRepository = mockk<FaceConsentRepository>()
        val faceProfileRepository = mockk<FaceProfileRepository>()
        val faceEmbeddingRepository = mockk<FaceEmbeddingRepository>()
        val faceProfileDeletionRepository = mockk<FaceProfileDeletionRepository>()
        val enrollmentRepository = mockk<EnrollmentRepository>()

        every { faceConsentRepository.existsById(studentId) } returns true
        every { faceProfileRepository.findById(studentId) } returns Optional.ofNullable(existingProfile)
        every { faceEmbeddingRepository.deleteByStudentId(studentId) } returns Unit

        val savedSlot = slot<List<FaceEmbedding>>()
        every { faceEmbeddingRepository.saveAll(capture(savedSlot)) } answers {
            savedEmbeddings.add(savedSlot.captured)
            savedSlot.captured
        }
        every { faceProfileRepository.save(any()) } answers { firstArg() }

        return FaceProfileService(
            faceConsentRepository = faceConsentRepository,
            faceProfileRepository = faceProfileRepository,
            faceEmbeddingRepository = faceEmbeddingRepository,
            faceProfileDeletionRepository = faceProfileDeletionRepository,
            enrollmentRepository = enrollmentRepository,
            clock = fixedClock
        )
    }

    // Feature: face-recognition, Property 1: Registration replaces the prior profile and preserves dimensionality rules
    @Test
    fun `registration replaces the prior profile and preserves dimensionality rules`(): Unit = runBlocking {
        checkAll(PropTestConfig(iterations = 100), registrationCaseArb) { case ->
            val embeddings = buildEmbeddings(case)
            val existingProfile = case.existingProfileDimension?.let { dimension ->
                FaceProfile(
                    studentId = studentId,
                    dimensions = dimension,
                    registeredAt = LocalDateTime.now(fixedClock).minusDays(1),
                    updatedAt = LocalDateTime.now(fixedClock).minusDays(1)
                )
            }
            val savedEmbeddings = mutableListOf<List<FaceEmbedding>>()
            val service = buildService(existingProfile, savedEmbeddings)

            val submittedDimensions = embeddings.map { it.size }.distinct()
            val withinRequestDisagreement = submittedDimensions.size > 1
            val crossRequestMismatch = !withinRequestDisagreement &&
                existingProfile != null &&
                existingProfile.dimensions != submittedDimensions.single()

            if (withinRequestDisagreement || crossRequestMismatch) {
                // R2.6/R2.7: a disagreeing or mismatched registration must be rejected.
                assertFailsWith<BadRequestException> {
                    service.register(studentId, embeddings)
                }
                // A rejected registration must never persist a replacement set.
                assertEquals(0, savedEmbeddings.size)
            } else {
                // Acceptance path: exactly the newly submitted vectors are left stored (R2.4).
                val response = service.register(studentId, embeddings)

                assertEquals(embeddings.size, response.embeddingCount)
                assertEquals(submittedDimensions.single(), response.dimensions)

                assertEquals(1, savedEmbeddings.size, "saveAll must be called exactly once on acceptance")
                val persistedVectors = savedEmbeddings.single().map { it.embeddingVector }
                val expectedVectors = embeddings.map { it.joinToString(",") }
                assertEquals(expectedVectors.toSet(), persistedVectors.toSet())
                assertEquals(embeddings.size, persistedVectors.size)
            }
        }
    }

    // Feature: face-recognition, Property 1: Registration replaces the prior profile and preserves dimensionality rules
    @Test
    fun `registration is rejected when no Face_Consent is recorded`(): Unit = runBlocking {
        checkAll(
            PropTestConfig(iterations = 100),
            Arb.list(vectorArb, 1..4).map { listOf(it) }
        ) { embeddings ->
            val faceConsentRepository = mockk<FaceConsentRepository>()
            val faceProfileRepository = mockk<FaceProfileRepository>()
            val faceEmbeddingRepository = mockk<FaceEmbeddingRepository>()
            val faceProfileDeletionRepository = mockk<FaceProfileDeletionRepository>()
            val enrollmentRepository = mockk<EnrollmentRepository>()
            every { faceConsentRepository.existsById(studentId) } returns false

            val service = FaceProfileService(
                faceConsentRepository = faceConsentRepository,
                faceProfileRepository = faceProfileRepository,
                faceEmbeddingRepository = faceEmbeddingRepository,
                faceProfileDeletionRepository = faceProfileDeletionRepository,
                enrollmentRepository = enrollmentRepository,
                clock = fixedClock
            )

            assertFailsWith<ForbiddenException> {
                service.register(studentId, embeddings)
            }
        }
    }

    // Feature: face-recognition, Property 1: Registration replaces the prior profile and preserves dimensionality rules
    @Test
    fun `registration is rejected when zero vectors are submitted`() {
        val savedEmbeddings = mutableListOf<List<FaceEmbedding>>()
        val service = buildService(existingProfile = null, savedEmbeddings = savedEmbeddings)

        assertFailsWith<BadRequestException> {
            service.register(studentId, emptyList())
        }
    }
}
