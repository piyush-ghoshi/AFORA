package com.afora.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * User entity representing authentication and authorization.
 * 
 * Phase A2: Integrated with Firebase Authentication.
 */
@Serializable
data class User(
    val id: Long = 0,
    val firebaseUid: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String, // STUDENT, TEACHER, ADMIN
    val profilePictureUrl: String? = null,
    val isActive: Boolean = true,
    val lastLoginAt: String? = null  // ISO 8601 datetime string
) {
    val fullName: String
        get() = "$firstName $lastName"
}

/**
 * User roles for RBAC.
 */
@Serializable
enum class UserRole {
    STUDENT,
    TEACHER,
    ADMIN
}
