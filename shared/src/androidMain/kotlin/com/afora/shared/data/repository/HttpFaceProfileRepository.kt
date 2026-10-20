package com.afora.shared.data.repository

import com.afora.shared.data.api.ApiClient
import com.afora.shared.data.api.EnrolledEmbeddingSetResponse
import com.afora.shared.data.api.FaceConsentResponse
import com.afora.shared.data.api.FaceProfileDeletionResponse
import com.afora.shared.data.api.FaceProfileResponse
import com.afora.shared.data.api.RegisterFaceProfileRequest
import com.afora.shared.data.api.response.ApiResponse
import com.afora.shared.util.AppError
import com.afora.shared.util.Result

/**
 * HTTP implementation of [FaceProfileRepository] for the Face_Recognition
 * feature (`.kiro/specs/face-recognition/design.md` → "Components and
 * Interfaces").
 *
 * Follows the [HttpAttendanceRepository] pattern: constructor-injected
 * [ApiClient], full `/api/face-profiles/...` endpoint strings (Binding
 * Convention 7), `result.data.data` unwrapping, [AppError] on failure.
 */
class HttpFaceProfileRepository(
    private val apiClient: ApiClient
) : FaceProfileRepository {

    override suspend fun recordConsent(): Result<FaceConsentResponse> {
        return when (
            val result: Result<ApiResponse<FaceConsentResponse>> =
                apiClient.post(
                    endpoint = "/api/face-profiles/consent",
                    body = Unit
                )
        ) {
            is Result.Success -> result.data.data?.let { Result.Success(it) }
                ?: Result.Error(AppError.NotFoundError("No consent response returned"))
            is Result.Error -> Result.Error(result.error)
        }
    }

    override suspend fun registerFaceProfile(
        embeddings: List<List<Double>>
    ): Result<FaceProfileResponse> {
        return when (
            val result: Result<ApiResponse<FaceProfileResponse>> =
                apiClient.post(
                    endpoint = "/api/face-profiles/embeddings",
                    body = RegisterFaceProfileRequest(embeddings)
                )
        ) {
            is Result.Success -> result.data.data?.let { Result.Success(it) }
                ?: Result.Error(AppError.NotFoundError("No face profile response returned"))
            is Result.Error -> Result.Error(result.error)
        }
    }

    override suspend fun deleteFaceProfile(studentId: Long): Result<FaceProfileDeletionResponse> {
        return when (
            val result: Result<ApiResponse<FaceProfileDeletionResponse>> =
                apiClient.delete(endpoint = "/api/face-profiles/$studentId")
        ) {
            is Result.Success -> result.data.data?.let { Result.Success(it) }
                ?: Result.Error(AppError.NotFoundError("No deletion response returned for student $studentId"))
            is Result.Error -> Result.Error(result.error)
        }
    }

    override suspend fun getEnrolledEmbeddingSet(
        classSectionId: Long
    ): Result<EnrolledEmbeddingSetResponse> {
        return when (
            val result: Result<ApiResponse<EnrolledEmbeddingSetResponse>> =
                apiClient.get(endpoint = "/api/face-profiles/class-sections/$classSectionId/enrolled-embeddings")
        ) {
            is Result.Success -> result.data.data?.let { Result.Success(it) }
                ?: Result.Error(AppError.NotFoundError("No enrolled embedding set returned for class section $classSectionId"))
            is Result.Error -> Result.Error(result.error)
        }
    }
}
