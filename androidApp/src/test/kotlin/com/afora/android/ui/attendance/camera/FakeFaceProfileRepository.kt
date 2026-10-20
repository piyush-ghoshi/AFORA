package com.afora.android.ui.attendance.camera

import com.afora.shared.data.api.EnrolledEmbeddingEntry
import com.afora.shared.data.api.EnrolledEmbeddingSetResponse
import com.afora.shared.data.api.FaceConsentResponse
import com.afora.shared.data.api.FaceProfileDeletionResponse
import com.afora.shared.data.api.FaceProfileResponse
import com.afora.shared.data.repository.FaceProfileRepository
import com.afora.shared.util.AppError
import com.afora.shared.util.Result

/**
 * Fake FaceProfileRepository for CameraAttendanceViewModel unit tests, following the
 * existing [com.afora.android.ui.attendance.FakeAttendanceRepository] precedent.
 *
 * Defaults [enrolledEmbeddingSetResult] to a non-empty set so existing camera-flow
 * tests, which don't exercise Requirement 10's fallback, keep their current behavior.
 */
class FakeFaceProfileRepository : FaceProfileRepository {

    var enrolledEmbeddingSetResult: Result<EnrolledEmbeddingSetResponse> = Result.Success(
        EnrolledEmbeddingSetResponse(
            classSectionId = 10,
            entries = listOf(
                EnrolledEmbeddingEntry(
                    studentId = 1,
                    rollNumber = "CS001",
                    fullName = "Alice Johnson",
                    embeddings = listOf(listOf(0.1, 0.2, 0.3))
                )
            ),
            highConfidenceThreshold = 0.80,
            mediumConfidenceThreshold = 0.60,
            unmatchedFloorThreshold = 0.60
        )
    )

    var lastClassSectionId: Long? = null

    override suspend fun recordConsent(): Result<FaceConsentResponse> =
        Result.Error(AppError.UnknownError("recordConsent not configured"))

    override suspend fun registerFaceProfile(embeddings: List<List<Double>>): Result<FaceProfileResponse> =
        Result.Error(AppError.UnknownError("registerFaceProfile not configured"))

    override suspend fun deleteFaceProfile(studentId: Long): Result<FaceProfileDeletionResponse> =
        Result.Error(AppError.UnknownError("deleteFaceProfile not configured"))

    override suspend fun getEnrolledEmbeddingSet(classSectionId: Long): Result<EnrolledEmbeddingSetResponse> {
        lastClassSectionId = classSectionId
        return enrolledEmbeddingSetResult
    }
}
