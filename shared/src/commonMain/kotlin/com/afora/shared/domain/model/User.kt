package com.afora.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * User entity representing authentication and authorization.
 * 
 * Phase A2: Integrated with Firebase Authentication.
 * Phase A7: Added studentId/teacherId from nested profile for dashboard routing.
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
    val lastLoginAt: String? = null,  // ISO 8601 datetime string
    val studentId: Long? = null,      // Student table PK, present when role == STUDENT
    val teacherId: Long? = null       // Teacher table PK, present when role == TEACHER
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
