package com.academia.shared.data.repository

import com.academia.shared.domain.model.User
import com.academia.shared.util.Result

/**
 * Authentication repository interface for Firebase Authentication.
 * 
 * Phase A2: Firebase-based authentication.
 */
interface AuthRepository {
    /**
     * Login with email and password using Firebase Authentication.
     * @param email User email
     * @param password User password
     * @return AuthResult with user and Firebase ID token
     */
    suspend fun login(email: String, password: String): Result<AuthResult>
    
    /**
     * Register new user with email and password.
     * @param email User email
     * @param password User password
     * @param firstName User first name
     * @param lastName User last name
     * @param role User role (STUDENT, TEACHER, ADMIN)
     * @return AuthResult with created user and Firebase ID token
     */
    suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        role: String
    ): Result<AuthResult>
    
    /**
     * Logout current user (sign out from Firebase).
     */
    suspend fun logout(): Result<Unit>
    
    /**
     * Get currently authenticated user from Firebase.
     * @return Current user if authenticated, error otherwise
     */
    suspend fun getCurrentUser(): Result<User>
    
    /**
     * Get Firebase ID token for current user.
     * Token is auto-refreshed by Firebase SDK if expired.
     * @param forceRefresh Force token refresh
     * @return Firebase ID token
     */
    suspend fun getIdToken(forceRefresh: Boolean = false): Result<String>
    
    /**
     * Check if user is currently authenticated.
     */
    suspend fun isAuthenticated(): Boolean
    
    /**
     * Send password reset email.
     * @param email User email
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    
    /**
     * Update user password (requires re-authentication).
     * @param currentPassword Current password for verification
     * @param newPassword New password
     */
    suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit>
    
    /**
     * Re-authenticate user with current credentials.
     * Required before sensitive operations like password change.
     * @param password Current password
     */
    suspend fun reauthenticate(password: String): Result<Unit>
}

/**
 * Authentication result containing user and Firebase ID token.
 */
data class AuthResult(
    val user: User,
    val idToken: String,        // Firebase ID token (send to backend)
    val expiresIn: Long         // Seconds until token expiry
)
