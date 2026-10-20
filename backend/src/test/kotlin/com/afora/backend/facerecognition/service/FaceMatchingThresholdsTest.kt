package com.afora.backend.facerecognition.service

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * Guards the ordering invariant required by requirement 7.4:
 * `HIGH_CONFIDENCE > MEDIUM_CONFIDENCE >= UNMATCHED_FLOOR`.
 *
 * `FaceMatchReviewGate` depends on this invariant holding; a future edit to
 * [FaceMatchingThresholds] that breaks it would silently change the gate's
 * behavior without any compiler error, which is exactly what this test exists
 * to catch.
 */
class FaceMatchingThresholdsTest {

    @Test
    fun `HIGH_CONFIDENCE is strictly greater than MEDIUM_CONFIDENCE`() {
        assertTrue(
            FaceMatchingThresholds.HIGH_CONFIDENCE > FaceMatchingThresholds.MEDIUM_CONFIDENCE,
            "HIGH_CONFIDENCE must be strictly greater than MEDIUM_CONFIDENCE (R7.4)"
        )
    }

    @Test
    fun `MEDIUM_CONFIDENCE is greater than or equal to UNMATCHED_FLOOR`() {
        assertTrue(
            FaceMatchingThresholds.MEDIUM_CONFIDENCE >= FaceMatchingThresholds.UNMATCHED_FLOOR,
            "MEDIUM_CONFIDENCE must be greater than or equal to UNMATCHED_FLOOR (R7.4)"
        )
    }

    @Test
    fun `all three thresholds fall within the sane 0 to 1 confidence range`() {
        val thresholds = listOf(
            FaceMatchingThresholds.HIGH_CONFIDENCE,
            FaceMatchingThresholds.MEDIUM_CONFIDENCE,
            FaceMatchingThresholds.UNMATCHED_FLOOR
        )
        thresholds.forEach { value ->
            assertTrue(
                value in 0.0..1.0,
                "Threshold value $value must fall within [0.0, 1.0]"
            )
        }
    }
}
