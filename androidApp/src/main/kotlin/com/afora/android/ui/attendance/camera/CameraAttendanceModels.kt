package com.afora.android.ui.attendance.camera

import android.graphics.RectF
import com.afora.shared.data.api.AttendanceConfirmationResponse
import com.afora.shared.data.api.RosterEntry
import com.afora.shared.data.api.RosterResponse
import com.afora.shared.domain.model.AttendanceStatus

/**
 * Confidence level categories per docs/face-recognition.md section 5.
 */
enum class ConfidenceCategory(val label: String) {
    HIGH("High Confidence"),
    MEDIUM("Needs Review"),
    LOW("Low Confidence"),
    UNKNOWN("Unrecognized")
}

/**
 * Pure Kotlin bounding box representation (independent of android.graphics.RectF stubs).
 */
data class CameraBoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/**
 * Single detected student candidate recognized across one or more camera frames.
 */
data class DetectedFaceCandidate(
    val studentId: Long,
    val rollNumber: String,
    val fullName: String,
    val confidence: Double,
    val category: ConfidenceCategory,
    val trackingId: Int? = null,
    val boundingBox: CameraBoundingBox? = null,
    val detectionCount: Int = 1,
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val attendanceStatus: AttendanceStatus = AttendanceStatus.PRESENT,
    val notes: String? = null,
    /**
     * True once a Review_Queue entry has been explicitly confirmed by the Teacher
     * (R8.2, face-recognition requirements.md). Carried through to
     * `SubmittedAttendanceEntry.teacherConfirmed` on confirmation (R9's wiring),
     * connecting this client-side UX gate to the server-side `FaceMatchReviewGate`.
     */
    val teacherConfirmed: Boolean = false
)

/**
 * Face bounding box representation for the live camera canvas overlay.
 */
data class FaceOverlayItem(
    val boundingBox: CameraBoundingBox,
    val trackingId: Int?,
    val matchedStudentName: String?,
    val confidence: Double,
    val category: ConfidenceCategory
)

/**
 * UI State for Camera-Based Attendance Scanning Screen.
 */
sealed interface CameraAttendanceUiState {
    object Loading : CameraAttendanceUiState

    data class Error(val message: String) : CameraAttendanceUiState

    data class Ready(
        val sessionId: Long,
        val roster: RosterResponse,
        val enrolledStudents: List<RosterEntry>,
        val candidates: Map<Long, DetectedFaceCandidate> = emptyMap(),
        /** R8.1: unresolved MEDIUM-band Face_Match_Candidate values, keyed by studentId. */
        val reviewQueue: Map<Long, FaceMatchCandidate> = emptyMap(),
        val liveOverlayFaces: List<FaceOverlayItem> = emptyList(),
        val isScanning: Boolean = true,
        val isTorchOn: Boolean = false,
        val isReviewing: Boolean = false,
        val isSubmitting: Boolean = false,
        val errorMessage: String? = null,
        val confirmation: AttendanceConfirmationResponse? = null,
        /**
         * Set when a Requirement 10 fallback trigger fires (empty Enrolled_Embedding_Set,
         * a zero-candidate scanning pass, or an unrecoverable matcher error). The composable
         * observes this and navigates to Mark_Attendance_Screen, then the field is cleared
         * via [consumeFallbackNavigation] so the one-off event is not replayed on
         * recomposition/rotation.
         */
        val shouldFallbackToManualMarking: Boolean = false
    ) : CameraAttendanceUiState {
        val totalEnrolled: Int get() = enrolledStudents.size
        val detectedCount: Int get() = candidates.size
        val highConfidenceCount: Int get() = candidates.values.count { it.category == ConfidenceCategory.HIGH }
        val reviewNeededCount: Int get() = candidates.values.count { it.category == ConfidenceCategory.MEDIUM || it.category == ConfidenceCategory.LOW }

        /** R8.4: client-side UX gate -- disabled while any Review_Queue entry is unresolved. */
        val confirmEnabled: Boolean get() = reviewQueue.isEmpty()
    }
}
