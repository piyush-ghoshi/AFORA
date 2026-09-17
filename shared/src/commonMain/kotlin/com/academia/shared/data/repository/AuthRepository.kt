package com.academia.shared.data.repository

import com.academia.shared.domain.model.User
import com.academia.shared.util.Result

/**
 * Authentication repository interface.
 */
interface AuthRepository {
    /**
     * Login with username and password.
     * @return User with auth token on success
     */
    suspend fun login(username: String, password: String): Result<AuthResult>
    
    /**
     * Logout current user.
     */
    suspend fun logout(): Result<Unit>
    
    /**
     * Refresh authentication token.
     */
    suspend fun refreshToken(refreshToken: String): Result<AuthResult>
    
    /**
     * Get currently authenticated user.
     */
    suspend fun getCurrentUser(): Result<User>
    
    /**
     * Check if user is authenticated.
     */
    suspend fun isAuthenticated(): Boolean
    
    /**
     * Get stored auth token (if any).
     */
    suspend fun getAuthToken(): String?
}

/**
 * Authentication result containing user and tokens.
 */
data class AuthResult(
    val user: User,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long  // Seconds until expiry
)
