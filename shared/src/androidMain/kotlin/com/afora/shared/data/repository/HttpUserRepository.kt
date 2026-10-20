package com.afora.shared.data.repository

import com.afora.shared.data.api.ApiClient
import com.afora.shared.data.api.request.SyncUserApiRequest
import com.afora.shared.data.api.request.UpdateProfileApiRequest
import com.afora.shared.data.api.response.ApiResponse
import com.afora.shared.data.api.response.AuthSyncApiResponse
import com.afora.shared.data.api.response.UserApiResponse
import com.afora.shared.data.mapper.toDomain
import com.afora.shared.domain.model.User
import com.afora.shared.util.AppError
import com.afora.shared.util.Result
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * HTTP implementation of UserRepository.
 *
 * Calls:
 *  POST /api/auth/sync  — sync Firebase user to backend DB
 *  GET  /api/users/me   — get current user's profile
 *  PUT  /api/users/me   — update profile
 */
class HttpUserRepository(
    private val apiClient: ApiClient
) : UserRepository {

    override suspend fun syncUser(
        firebaseUid: String,
        email: String,
        firstName: String,
        lastName: String,
        role: String,
        idNumber: String?
    ): Result<User> {
        val result: Result<ApiResponse<AuthSyncApiResponse>> = apiClient.post(
            endpoint = "/api/auth/sync",
            body = SyncUserApiRequest(
                firebaseUid = firebaseUid,
                email = email,
                firstName = firstName,
                lastName = lastName,
                role = role,
                idNumber = idNumber
            )
        )
        return when (result) {
            is Result.Success -> {
                val user = result.data.data?.user?.toDomain()
                if (user != null) Result.Success(user)
                else Result.Error(AppError.UnknownError("Empty response from server"))
            }
            is Result.Error -> Result.Error(humanise(result.error))
        }
    }

    override suspend fun getMyProfile(): Result<User> {
        val result: Result<ApiResponse<UserApiResponse>> = apiClient.get(
            endpoint = "/api/users/me"
        )
        return when (result) {
            is Result.Success -> {
                val user = result.data.data?.toDomain()
                if (user != null) Result.Success(user)
                else Result.Error(AppError.UnknownError("Empty response from server"))
            }
            is Result.Error -> Result.Error(humanise(result.error))
        }
    }

    override suspend fun updateProfile(
        firstName: String,
        lastName: String,
        profilePictureUrl: String?
    ): Result<User> {
        val result: Result<ApiResponse<UserApiResponse>> = apiClient.put(
            endpoint = "/api/users/me",
            body = UpdateProfileApiRequest(
                firstName = firstName,
                lastName = lastName,
                profilePictureUrl = profilePictureUrl
            )
        )
        return when (result) {
            is Result.Success -> {
                val user = result.data.data?.toDomain()
                if (user != null) Result.Success(user)
                else Result.Error(AppError.UnknownError("Empty response from server"))
            }
            is Result.Error -> Result.Error(humanise(result.error))
        }
    }

    // --- tolerant error decoding (same approach as the attendance and leave repositories) ---

    private val errorJson = Json { ignoreUnknownKeys = true; isLenient = true }

    @Serializable
    private data class BackendErrorEnvelope(val error: BackendErrorDetail? = null)

    @Serializable
    private data class BackendErrorDetail(val message: String, val code: String? = null)

    /** Replaces an [AppError.ApiError]'s raw JSON body with the backend's readable message. */
    private fun humanise(error: AppError): AppError {
        if (error !is AppError.ApiError) return error
        val decoded = try {
            errorJson.decodeFromString<BackendErrorEnvelope>(error.message)
        } catch (e: Exception) {
            null
        }
        return AppError.ApiError(code = error.code, message = decoded?.error?.message ?: error.message)
    }
}
