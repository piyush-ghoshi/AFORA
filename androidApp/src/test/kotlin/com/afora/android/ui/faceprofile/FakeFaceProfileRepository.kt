package com.afora.android.ui.faceprofile

import com.afora.shared.data.api.EnrolledEmbeddingSetResponse
import com.afora.shared.data.api.FaceConsentResponse
import com.afora.shared.data.api.FaceProfileDeletionResponse
import com.afora.shared.data.api.FaceProfileResponse
import com.afora.shared.data.repository.FaceProfileRepository
import com.afora.shared.util.AppError
import com.afora.shared.util.Result

/**
 * Fake FaceProfileRepository for [FaceRegistrationViewModel] unit tests, following the
 * [com.afora.android.ui.attendance.FakeAttendanceRepository] precedent: a hand-written fake is
 * easier to drive across the consent/capture/register sequence than a mock requiring a stub per
 * call, and it lets tests assert on exactly what was passed to [registerFaceProfile] (R13.3) and
 * how many times [recordConsent] was invoked (R13.5).
 *
 * Package-local to `ui.faceprofile` rather than reusing
 * `ui.attendance.camera.FakeFaceProfileRepository`: that fake defaults `recordConsent()` and
 * `registerFaceProfile(...)` to an unconfigured error (it's tailored to
 * `CameraAttendanceViewModel`'s `getEnrolledEmbeddingSet()` path) and doesn't track call counts or
 * capture arguments, which this test needs.
 */
class FakeFaceProfileRepository : FaceProfileRepository {

    var recordConsentResult: Result<FaceConsentResponse> = Result.Success(
        FaceConsentResponse(studentId = 1, consentedAt = "2026-01-01T00:00:00")
    )

    var registerFaceProfileResult: Result<FaceProfileResponse> = Result.Success(
        FaceProfileResponse(
            studentId = 1,
            embeddingCount = 1,
            dimensions = 128,
            registeredAt = "2026-01-01T00:00:00"
        )
    )

    var recordConsentCallCount: Int = 0
        private set

    var lastRegisteredEmbeddings: List<List<Double>>? = null
        private set

    override suspend fun recordConsent(): Result<FaceConsentResponse> {
        recordConsentCallCount++
        return recordConsentResult
    }

    override suspend fun registerFaceProfile(embeddings: List<List<Double>>): Result<FaceProfileResponse> {
        lastRegisteredEmbeddings = embeddings
        return registerFaceProfileResult
    }

    override suspend fun deleteFaceProfile(studentId: Long): Result<FaceProfileDeletionResponse> =
        Result.Error(AppError.UnknownError("deleteFaceProfile not configured"))

    override suspend fun getEnrolledEmbeddingSet(classSectionId: Long): Result<EnrolledEmbeddingSetResponse> =
        Result.Error(AppError.UnknownError("getEnrolledEmbeddingSet not configured"))
}
