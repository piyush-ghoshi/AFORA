package com.afora.backend.facerecognition.service

/**
 * The three Confidence_Band boundaries of Requirement 7, owned exclusively by the
 * backend per design.md's Settled Decision B.
 *
 * These are **placeholder values pending real model tuning**, as requirements.md's own
 * "Open Items for the Design Phase" names explicitly. They are not derived from any
 * benchmark or real model evaluation — they exist so the Review_Queue gate and the
 * on-device UX classification have concrete numbers to agree on before a tuned model
 * is selected.
 *
 * The ordering invariant required by requirement 7.4 holds by construction:
 * `HIGH_CONFIDENCE > MEDIUM_CONFIDENCE >= UNMATCHED_FLOOR`.
 *
 * This object is the **single source of truth** for all three values across the entire
 * codebase. No other file, backend or Android, may declare a literal threshold value
 * (Binding Convention 6). A device never ships its own copy of these constants: it
 * receives them at runtime inside `EnrolledEmbeddingSetResponse` (a future task) and
 * uses that copy for its own HIGH/MEDIUM/unmatched classification, while the backend
 * recomputes the band server-side from this same object inside `FaceMatchReviewGate`
 * (also a future task) for the security-relevant Review_Queue enforcement.
 */
object FaceMatchingThresholds {

    /** Match_Confidence at or above this value classifies as HIGH (requirement 7.1). */
    const val HIGH_CONFIDENCE: Double = 0.80

    /**
     * Match_Confidence at or above this value, and below [HIGH_CONFIDENCE], classifies
     * as MEDIUM (requirement 7.2).
     */
    const val MEDIUM_CONFIDENCE: Double = 0.60

    /**
     * Match_Confidence below this value is presented as unmatched (requirement 7.3).
     * Equal to [MEDIUM_CONFIDENCE] today, satisfying the `<=` half of requirement 7.4's
     * ordering constraint.
     */
    const val UNMATCHED_FLOOR: Double = 0.60
}
