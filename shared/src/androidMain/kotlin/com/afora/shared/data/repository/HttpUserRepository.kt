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
        role: String
    ): Result<User> {
        val result: Result<ApiResponse<AuthSyncApiResponse>> = apiClient.post(
            endpoint = "/auth/sync",
            body = SyncUserApiRequest(
                firebaseUid = firebaseUid,
                email = email,
                firstName = firstName,
                lastName = lastName,
                role = role
            )
        )
        return when (result) {
            is Result.Success -> {
                val user = result.data.data?.user?.toDomain()
                if (user != null) Result.Success(user)
                else Result.Error(AppError.UnknownError("Empty response from server"))
            }
            is Result.Error -> result
        }
    }

    override suspend fun getMyProfile(): Result<User> {
        val result: Result<ApiResponse<UserApiResponse>> = apiClient.get(
            endpoint = "/users/me"
        )
        return when (result) {
            is Result.Success -> {
                val user = result.data.data?.toDomain()
                if (user != null) Result.Success(user)
                else Result.Error(AppError.UnknownError("Empty response from server"))
            }
            is Result.Error -> result
        }
    }

    override suspend fun updateProfile(
        firstName: String,
        lastName: String,
        profilePictureUrl: String?
    ): Result<User> {
        val result: Result<ApiResponse<UserApiResponse>> = apiClient.put(
            endpoint = "/users/me",
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
            is Result.Error -> result
        }
    }
}
