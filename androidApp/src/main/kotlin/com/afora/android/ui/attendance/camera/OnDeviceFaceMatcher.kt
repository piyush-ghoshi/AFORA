package com.afora.android.ui.attendance.camera

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face

/**
 * On_Device_Matcher (requirements.md Glossary; design.md "On-device matching").
 *
 * Replaces the index-arithmetic placeholder (`abs(trackingId) % enrolledStudents.size`)
 * and the fixed-base confidence formula previously living in
 * `FaceDetectionAnalyzer.kt`'s `matchStudentForFace`/`calculateConfidence` (Requirement 12).
 *
 * Generates a Face_Embedding from a detected face entirely on-device (R6.1), then compares
 * that embedding against a distributed Enrolled_Embedding_Set to produce at most one
 * [FaceMatchCandidate] (R6.2-R6.4). No threshold value is declared or referenced anywhere in
 * this file: the three Confidence_Band boundaries (High_Confidence_Threshold,
 * Medium_Confidence_Threshold, Unmatched_Floor_Threshold) are backend-owned
 * (`FaceMatchingThresholds`) and arrive at runtime inside `EnrolledEmbeddingSetResponse` —
 * never hardcoded on the device (Binding Convention 6).
 */
interface OnDeviceFaceMatcher {

    /** R6.1: generates a Face_Embedding from one detected face, entirely on-device. */
    fun generateEmbedding(face: Face, frame: InputImage): FloatArray

    /**
     * R6.2-R6.4: compares the embedding against every entry in the distributed set and
     * returns the single best match, or null if every candidate falls below the
     * Unmatched_Floor_Threshold (R7.3) — in which case no Face_Match_Candidate exists
     * at all, by contract, and the caller must not fabricate one.
     */
    fun match(embedding: FloatArray, enrolledSet: List<EnrolledEmbedding>): FaceMatchCandidate?
}

/**
 * One enrolled student's distributed embedding set, as received from the backend's
 * Enrolled_Embedding_Set for one Class_Section (Requirement 5). Lives in this
 * Android camera-flow package rather than in `shared`, since it is consumed only by
 * on-device matching code and has no shared-KMP consumer.
 */
data class EnrolledEmbedding(
    val studentId: Long,
    val rollNumber: String,
    val fullName: String,
    val vectors: List<FloatArray>
)

/**
 * One detected face in a Camera_Session, correlated by the [OnDeviceFaceMatcher] to a
 * specific enrolled student with a Match_Confidence and a [ConfidenceBand] (Requirement 6/7).
 */
data class FaceMatchCandidate(
    val studentId: Long,
    val rollNumber: String,
    val fullName: String,
    val matchConfidence: Double,
    val confidenceBand: ConfidenceBand,
    val trackingId: Int?
)

/**
 * Confidence_Band (requirements.md Glossary). Exactly two values: anything that would
 * otherwise be classified below the matching floor never materializes a
 * [FaceMatchCandidate] at all (R7.3), so there is no third, "below-floor" value here by
 * contract.
 */
enum class ConfidenceBand { HIGH, MEDIUM }

/**
 * Documented placeholder implementation of [OnDeviceFaceMatcher] (Binding Convention 11).
 *
 * This is **not** real face matching. It performs no embedding generation and no embedding
 * comparison of any kind. [generateEmbedding] unconditionally returns an empty [FloatArray]
 * for every input, and [match] unconditionally returns `null` for every input, regardless of
 * the detected face, the frame, the embedding passed in, or the contents of [enrolledSet].
 *
 * Until a real matcher replaces this stub, every detected face resolves to "no candidate."
 * The camera flow is expected to rely on the Requirement 10 fallback to
 * `Mark_Attendance_Screen`: a scanning attempt that produces zero [FaceMatchCandidate]s for
 * the entire roster (R10.2) routes the teacher to manual roster marking rather than blocking
 * the session.
 *
 * Model selection and real matching logic are explicitly out of scope for this spec
 * (requirements.md's own "Open Items for the Design Phase"; design.md's Overview).
 */
class StubFaceMatcher : OnDeviceFaceMatcher {

    override fun generateEmbedding(face: Face, frame: InputImage): FloatArray {
        return FloatArray(0)
    }

    override fun match(embedding: FloatArray, enrolledSet: List<EnrolledEmbedding>): FaceMatchCandidate? {
        return null
    }
}
