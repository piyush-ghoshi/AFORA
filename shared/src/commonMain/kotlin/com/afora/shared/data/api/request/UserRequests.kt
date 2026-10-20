package com.afora.shared.data.api.request

import kotlinx.serialization.Serializable

/**
 * Sent to POST /api/auth/sync after Firebase registration.
 */
@Serializable
data class SyncUserApiRequest(
    val firebaseUid: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String,  // "STUDENT" or "TEACHER"
    /** Roll number (students) or employee id (teachers); required to create a new account. */
    val idNumber: String? = null
)

/**
 * Sent to PUT /api/users/me.
 */
@Serializable
data class UpdateProfileApiRequest(
    val firstName: String,
    val lastName: String,
    val profilePictureUrl: String? = null
)
