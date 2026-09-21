package com.academia.shared.data.repository

import com.academia.shared.domain.model.User
import com.academia.shared.util.Result

/**
 * User repository interface — bridges Firebase auth with the backend user profile.
 *
 * After Firebase authentication succeeds on the client, the app calls [syncUser]
 * to create/retrieve the matching local user record in the backend DB.
 */
interface UserRepository {

    /**
     * Sync a Firebase-authenticated user to the backend database.
     * Creates a new local user on first registration, or updates last_login_at on subsequent logins.
     *
     * @param firebaseUid  Firebase UID from the authenticated user
     * @param email        User email
     * @param firstName    User first name
     * @param lastName     User last name
     * @param role         "STUDENT" or "TEACHER"
     * @return The full User domain model
     */
    suspend fun syncUser(
        firebaseUid: String,
        email: String,
        firstName: String,
        lastName: String,
        role: String
    ): Result<User>

    /**
     * Get the currently authenticated user's full profile from the backend.
     * Requires a valid Firebase ID token to be attached by ApiClient.
     */
    suspend fun getMyProfile(): Result<User>

    /**
     * Update the authenticated user's display name / profile picture.
     */
    suspend fun updateProfile(
        firstName: String,
        lastName: String,
        profilePictureUrl: String? = null
    ): Result<User>
}
