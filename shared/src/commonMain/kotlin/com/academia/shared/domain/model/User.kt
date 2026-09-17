package com.academia.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * User entity representing authentication and authorization.
 */
@Serializable
data class User(
    val id: Long,
    val username: String,
    val email: String,
    val role: UserRole,
    val isActive: Boolean = true,
    val lastLoginAt: String? = null  // ISO 8601 datetime string
)

/**
 * User roles for RBAC.
 */
@Serializable
enum class UserRole {
    STUDENT,
    TEACHER,
    ADMIN
}
