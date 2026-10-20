package com.afora.backend.facerecognition.service

import com.afora.backend.attendance.dto.SubmittedAttendanceEntry
import com.afora.backend.common.exception.BadRequestException
import com.afora.backend.common.exception.ConflictException
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.orNull
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * **Validates: Requirements 7.3, 7.5, 8.1, 8.2, 8.5**
 *
 * Exercises [FaceMatchReviewGate.require] following the sibling
 * `FaceProfileRegistrationPropertyTest`'s own harness pattern: `kotest-property` used as a
 * library, `checkAll` invoked from a plain JUnit test inside `runBlocking { }`.
 *
 * [FaceMatchReviewGate] is a stateless, dependency-free `object`, so this test needs no
 * mocking at all -- it builds [SubmittedAttendanceEntry] instances directly and asserts the
 * gate's verdict is a pure function of each entry's `confidenceScore`/`teacherConfirmed`
 * pair, independent of every other entry in the batch (per design.md Settled Decision B).
 */
class FaceMatchReviewGatePropertyTest {

    private val highConfidence = FaceMatchingThresholds.HIGH_CONFIDENCE
    private val mediumConfidence = FaceMatchingThresholds.MEDIUM_CONFIDENCE

    /**
     * One entry's randomly generated (confidenceScore, teacherConfirmed) pair. Scores span
     * well below the floor, the open interval between the two boundaries, the HIGH band,
     * and both exact boundary values themselves, so every one of the gate's three branches
     * -- and their boundary edges -- gets exercised. `null` stands in for a manual mark or
     * an already-HIGH match the teacher left untouched, which the gate must skip entirely.
     */
    private val scoreArb: Arb<Double?> = Arb.choice(
        Arb.double(0.0, 1.0),
        Arb.constant(highConfidence),
        Arb.constant(mediumConfidence)
    ).orNull(nullProbability = 0.25)

    private val entrySpecArb: Arb<Pair<Double?, Boolean>> =
        Arb.bind(scoreArb, Arb.boolean()) { score, confirmed -> score to confirmed }

    /** Student ids are the list index, guaranteeing uniqueness so the expected offending-id
     * sets below can be compared without any duplicate-id ambiguity. */
    private fun buildEntries(specs: List<Pair<Double?, Boolean>>): List<SubmittedAttendanceEntry> =
        specs.mapIndexed { index, (score, confirmed) ->
            SubmittedAttendanceEntry(
                studentId = index.toLong(),
                status = "PRESENT",
                confidenceScore = score,
                teacherConfirmed = confirmed
            )
        }

    // Feature: face-recognition, Property 3: The review gate's verdict is a pure function of each entry's score and confirmation flag
    @Test
    fun `the review gate's verdict is a pure function of each entry's score and confirmation flag`(): Unit = runBlocking {
        checkAll(PropTestConfig(iterations = 100), Arb.list(entrySpecArb, 0..15)) { specs ->
            val entries = buildEntries(specs)

            val invalidIds = entries
                .filter { entry -> entry.confidenceScore?.let { it < mediumConfidence } ?: false }
                .map { it.studentId }
                .sorted()
            val unresolvedIds = entries
                .filter { entry ->
                    val score = entry.confidenceScore
                    score != null && score >= mediumConfidence && score < highConfidence && !entry.teacherConfirmed
                }
                .map { it.studentId }
                .sorted()

            when {
                // Below-the-floor entries take precedence, matching FaceMatchReviewGate.require's
                // own check order (invalid before unresolved).
                invalidIds.isNotEmpty() -> {
                    val exception = assertFailsWith<BadRequestException> {
                        FaceMatchReviewGate.require(entries)
                    }
                    assertTrue(
                        exception.message!!.contains(invalidIds.toString()),
                        "expected the exact offending id set $invalidIds in: ${exception.message}"
                    )
                }
                unresolvedIds.isNotEmpty() -> {
                    val exception = assertFailsWith<ConflictException> {
                        FaceMatchReviewGate.require(entries)
                    }
                    assertTrue(
                        exception.message!!.contains(unresolvedIds.toString()),
                        "expected the exact offending id set $unresolvedIds in: ${exception.message}"
                    )
                }
                else -> {
                    // Every scored entry is either HIGH, or MEDIUM-and-confirmed; null scores are
                    // skipped entirely. The gate must accept with no exception thrown.
                    FaceMatchReviewGate.require(entries)
                }
            }
        }
    }
}
