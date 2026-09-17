package com.academia.shared.data.api.request

import kotlinx.serialization.Serializable

/**
 * Login request DTO.
 */
@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

/**
 * Refresh token request DTO.
 */
@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)
