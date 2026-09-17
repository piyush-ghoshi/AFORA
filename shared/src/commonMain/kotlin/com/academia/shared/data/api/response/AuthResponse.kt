package com.academia.shared.data.api.response

import com.academia.shared.domain.model.User
import kotlinx.serialization.Serializable

/**
 * Authentication response DTO.
 */
@Serializable
data class AuthResponse(
    val user: User,
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long  // Seconds until token expiry
)
