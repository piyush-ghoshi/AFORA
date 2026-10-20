package com.afora.backend.facerecognition.service

import com.afora.backend.attendance.dto.SubmittedAttendanceEntry
import com.afora.backend.common.exception.BadRequestException
import com.afora.backend.common.exception.ConflictException

/**
 * Server-side enforcement of the Review_Queue gate, per design.md's Settled Decision B.
 *
 * This is a pure, stateless object: no persistence, no injected repositories. It depends
 * only on [FaceMatchingThresholds] for the three confidence boundaries, and reuses the
 * existing project-wide [BadRequestException]/[ConflictException] types.
 *
 * R8.5. Gated on `confidenceScore` being present, not on `request.method`, so a client
 * cannot bypass the gate by mislabeling the method (see design.md's rejected alternatives
 * for this decision). Entries with a null `confidenceScore` (manual marks, or a HIGH match
 * the teacher simply left as-is) are skipped entirely.
 */
object FaceMatchReviewGate {

    /**
     * Recomputes each entry's Confidence_Band server-side from its `confidenceScore` and
     * enforces the Review_Queue gate:
     * - `score >= HIGH_CONFIDENCE` passes untouched (R7.5).
     * - `score >= MEDIUM_CONFIDENCE` and `!teacherConfirmed` collects into `unresolved`
     *   (R8.1, R8.2).
     * - A present score below `MEDIUM_CONFIDENCE` (i.e. below the matching floor while
     *   still carrying a score) collects into `invalid` (R7.3, R9.3).
     * - A null `confidenceScore` is skipped entirely.
     *
     * Throws [BadRequestException] naming the offending student ids when `invalid` is
     * non-empty, and [ConflictException] naming the offending student ids when
     * `unresolved` is non-empty.
     */
    fun require(
        entries: List<SubmittedAttendanceEntry>,
        thresholds: FaceMatchingThresholds = FaceMatchingThresholds
    ) {
        val unresolved = mutableListOf<Long>()
        val invalid = mutableListOf<Long>()
        for (entry in entries) {
            val score = entry.confidenceScore ?: continue
            when {
                score >= thresholds.HIGH_CONFIDENCE -> Unit // HIGH: no gate (R7.5)
                score >= thresholds.MEDIUM_CONFIDENCE ->
                    if (!entry.teacherConfirmed) unresolved += entry.studentId // R8.1-R8.2
                else -> invalid += entry.studentId // below the floor must carry no score (R7.3, R9.3)
            }
        }
        if (invalid.isNotEmpty()) throw BadRequestException(
            "Entries carry a confidenceScore below the matching floor with no corresponding " +
                "Face_Match_Candidate: ${invalid.sorted()}"
        )
        if (unresolved.isNotEmpty()) throw ConflictException(
            "Camera confirmation blocked: ${unresolved.size} medium-confidence match(es) require " +
                "teacher confirmation before this session can finalize: ${unresolved.sorted()}"
        )
    }
}
