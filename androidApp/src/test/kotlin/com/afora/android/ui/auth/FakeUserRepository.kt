package com.afora.android.ui.auth

import com.afora.shared.data.repository.UserRepository
import com.afora.shared.domain.model.User
import com.afora.shared.util.AppError
import com.afora.shared.util.Result

/**
 * Fake UserRepository for unit tests.
 * syncUser returns success by default so ViewModel tests don't need to configure it.
 */
class FakeUserRepository : UserRepository {

    var syncResult: Result<User> = Result.Success(
        User(1, "uid_synced", "synced@afora.edu", "Test", "User", "STUDENT")
    )
    var profileResult: Result<User> = Result.Error(AppError.NotFoundError("No profile"))
    var updateResult: Result<User>? = null

    var syncCalls = 0
    var lastSyncIdNumber: String? = null
    var lastSyncRole: String? = null
    var lastUpdate: Pair<String, String>? = null

    override suspend fun syncUser(
        firebaseUid: String,
        email: String,
        firstName: String,
        lastName: String,
        role: String,
        idNumber: String?
    ): Result<User> {
        syncCalls++
        lastSyncIdNumber = idNumber
        lastSyncRole = role
        return syncResult
    }

    override suspend fun getMyProfile(): Result<User> = profileResult

    override suspend fun updateProfile(
        firstName: String,
        lastName: String,
        profilePictureUrl: String?
    ): Result<User> {
        lastUpdate = firstName to lastName
        return updateResult ?: syncResult
    }
}