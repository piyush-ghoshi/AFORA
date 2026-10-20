package com.afora.backend.facerecognition.service

import com.afora.backend.attendance.dto.SubmittedAttendanceEntry
import com.afora.backend.common.exception.BadRequestException
import com.afora.backend.common.exception.ConflictException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertTrue

/**
 * Tests [FaceMatchReviewGate.require]'s three confidence-band branches, per task 8.2's
 * own task text and design.md's Settled Decision B.
 *
 * `FaceMatchReviewGate` is a dependency-free, stateless `object`, so this is a pure-function
 * test: no mocking framework is needed anywhere in this file. `SubmittedAttendanceEntry`
 * instances are built directly with the `status` field left at an arbitrary, irrelevant
 * value ("PRESENT") since the gate only ever inspects `confidenceScore`/`teacherConfirmed`.
 *
 * Mocking: MockK only (project-wide rule) -- not applicable here since nothing is mocked.
 */
class FaceMatchReviewGateTest {

    private fun entry(
        studentId: Long,
        confidenceScore: Double?,
        teacherConfirmed: Boolean = false
    ) = SubmittedAttendanceEntry(
        studentId = studentId,
        status = "PRESENT",
        confidenceScore = confidenceScore,
        teacherConfirmed = teacherConfirmed
    )

    // ── Branch 1: HIGH ───────────────────────────────────────────────────────

    @Test
    fun `an entry at or above HIGH_CONFIDENCE passes with no exception regardless of teacherConfirmed`() {
        val entries = listOf(
            entry(studentId = 1L, confidenceScore = FaceMatchingThresholds.HIGH_CONFIDENCE, teacherConfirmed = false)
        )

        // No exception thrown (R7.5).
        FaceMatchReviewGate.require(entries)
    }

    @Test
    fun `a score well above HIGH_CONFIDENCE also passes with no exception`() {
        val entries = listOf(entry(studentId = 1L, confidenceScore = 0.99, teacherConfirmed = false))

        FaceMatchReviewGate.require(entries)
    }

    // ── Branch 2: MEDIUM, unconfirmed ───────────────────────────────────────────

    @Test
    fun `an unconfirmed MEDIUM-band entry throws ConflictException naming that student id`() {
        val entries = listOf(
            entry(studentId = 7L, confidenceScore = FaceMatchingThresholds.MEDIUM_CONFIDENCE, teacherConfirmed = false)
        )

        val ex = assertThrows<ConflictException> { FaceMatchReviewGate.require(entries) }

        assertTrue("7" in (ex.message ?: ""), "expected student id 7 to be named in: ${ex.message}")
    }

    @Test
    fun `a score just below HIGH_CONFIDENCE and unconfirmed still falls in the MEDIUM band`() {
        val justBelowHigh = FaceMatchingThresholds.HIGH_CONFIDENCE - 0.01
        val entries = listOf(entry(studentId = 9L, confidenceScore = justBelowHigh, teacherConfirmed = false))

        val ex = assertThrows<ConflictException> { FaceMatchReviewGate.require(entries) }

        assertTrue("9" in (ex.message ?: ""), "expected student id 9 to be named in: ${ex.message}")
    }

    @Test
    fun `a MEDIUM-band entry with teacherConfirmed true passes with no exception`() {
        val entries = listOf(
            entry(studentId = 7L, confidenceScore = FaceMatchingThresholds.MEDIUM_CONFIDENCE, teacherConfirmed = true)
        )

        // No exception thrown -- explicit teacher confirmation clears the gate (R8.1, R8.2).
        FaceMatchReviewGate.require(entries)
    }

    // ── Branch 3: below the matching floor, with a score present ────────────────

    @Test
    fun `a non-null score below MEDIUM_CONFIDENCE throws BadRequestException naming that student id`() {
        val belowFloor = FaceMatchingThresholds.MEDIUM_CONFIDENCE - 0.01
        val entries = listOf(entry(studentId = 13L, confidenceScore = belowFloor, teacherConfirmed = false))

        val ex = assertThrows<BadRequestException> { FaceMatchReviewGate.require(entries) }

        assertTrue("13" in (ex.message ?: ""), "expected student id 13 to be named in: ${ex.message}")
    }

    @Test
    fun `a below-floor score throws BadRequestException even when teacherConfirmed is true`() {
        val belowFloor = FaceMatchingThresholds.MEDIUM_CONFIDENCE - 0.01
        val entries = listOf(entry(studentId = 13L, confidenceScore = belowFloor, teacherConfirmed = true))

        // A below-floor score must carry no corresponding Face_Match_Candidate at all (R7.3,
        // R9.3); teacherConfirmed is irrelevant to this branch.
        assertThrows<BadRequestException> { FaceMatchReviewGate.require(entries) }
    }

    // ── Null confidenceScore is skipped entirely ─────────────────────────────────

    @Test
    fun `a null confidenceScore entry is never inspected regardless of teacherConfirmed`() {
        val unconfirmedManual = listOf(entry(studentId = 21L, confidenceScore = null, teacherConfirmed = false))
        val confirmedManual = listOf(entry(studentId = 22L, confidenceScore = null, teacherConfirmed = true))

        // Neither throws: a null score (manual marking) is skipped entirely, independent of
        // teacherConfirmed.
        FaceMatchReviewGate.require(unconfirmedManual)
        FaceMatchReviewGate.require(confirmedManual)
    }

    // ── A mixed batch reports every offending id ────────────────────────────────

    @Test
    fun `invalid entries take priority and BadRequestException names every invalid id, not just the first`() {
        val entries = listOf(
            entry(studentId = 1L, confidenceScore = FaceMatchingThresholds.HIGH_CONFIDENCE), // passes
            entry(studentId = 2L, confidenceScore = FaceMatchingThresholds.MEDIUM_CONFIDENCE, teacherConfirmed = false), // unresolved
            entry(studentId = 3L, confidenceScore = FaceMatchingThresholds.MEDIUM_CONFIDENCE - 0.01), // invalid
            entry(studentId = 4L, confidenceScore = FaceMatchingThresholds.MEDIUM_CONFIDENCE - 0.02), // invalid
            entry(studentId = 5L, confidenceScore = null) // skipped
        )

        // invalid is checked before unresolved inside require(), so a batch containing both
        // an invalid and an unresolved entry surfaces BadRequestException naming every
        // invalid id, not the unresolved one.
        val ex = assertThrows<BadRequestException> { FaceMatchReviewGate.require(entries) }

        assertTrue("3" in (ex.message ?: ""), "expected student id 3 to be named in: ${ex.message}")
        assertTrue("4" in (ex.message ?: ""), "expected student id 4 to be named in: ${ex.message}")
    }

    @Test
    fun `a batch with only unresolved entries reports every unresolved id, not just the first`() {
        val entries = listOf(
            entry(studentId = 1L, confidenceScore = FaceMatchingThresholds.HIGH_CONFIDENCE), // passes
            entry(studentId = 2L, confidenceScore = FaceMatchingThresholds.MEDIUM_CONFIDENCE, teacherConfirmed = false), // unresolved
            entry(studentId = 6L, confidenceScore = FaceMatchingThresholds.MEDIUM_CONFIDENCE, teacherConfirmed = false), // unresolved
            entry(studentId = 7L, confidenceScore = FaceMatchingThresholds.MEDIUM_CONFIDENCE, teacherConfirmed = true), // passes
            entry(studentId = 8L, confidenceScore = null) // skipped
        )

        val ex = assertThrows<ConflictException> { FaceMatchReviewGate.require(entries) }

        assertTrue("2" in (ex.message ?: ""), "expected student id 2 to be named in: ${ex.message}")
        assertTrue("6" in (ex.message ?: ""), "expected student id 6 to be named in: ${ex.message}")
    }

    @Test
    fun `an entry list with nothing invalid or unresolved passes with no exception`() {
        val entries = listOf(
            entry(studentId = 1L, confidenceScore = FaceMatchingThresholds.HIGH_CONFIDENCE),
            entry(studentId = 2L, confidenceScore = FaceMatchingThresholds.MEDIUM_CONFIDENCE, teacherConfirmed = true),
            entry(studentId = 3L, confidenceScore = null)
        )

        FaceMatchReviewGate.require(entries)
    }

    @Test
    fun `an empty entry list passes with no exception`() {
        FaceMatchReviewGate.require(emptyList())
    }
}
