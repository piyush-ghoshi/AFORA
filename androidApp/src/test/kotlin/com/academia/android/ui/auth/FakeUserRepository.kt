package com.academia.android.ui.auth

import com.academia.shared.data.repository.UserRepository
import com.academia.shared.domain.model.User
import com.academia.shared.util.AppError
import com.academia.shared.util.Result

/**
 * Fake UserRepository for unit tests.
 * syncUser returns success by default so ViewModel tests don't need to configure it.
 */
class FakeUserRepository : UserRepository {

    var syncResult: Result<User> = Result.Success(
        User(1, "uid_synced", "synced@afora.edu", "Test", "User", "STUDENT")
    )
    var profileResult: Result<User> = Result.Error(AppError.NotFoundError("No profile"))

    override suspend fun syncUser(
        firebaseUid: String,
        email: String,
        firstName: String,
        lastName: String,
        role: String
    ): Result<User> = syncResult

    override suspend fun getMyProfile(): Result<User> = profileResult

    override suspend fun updateProfile(
        firstName: String,
        lastName: String,
        profilePictureUrl: String?
    ): Result<User> = syncResult
}
