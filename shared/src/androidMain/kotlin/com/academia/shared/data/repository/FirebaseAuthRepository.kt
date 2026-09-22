package com.academia.shared.data.repository

import com.academia.shared.domain.model.User
import com.academia.shared.util.AppError
import com.academia.shared.util.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Firebase Authentication Repository implementation for Android.
 * 
 * Handles:
 * - Email/password authentication
 * - User registration
 * - Firebase ID token management
 * - Password reset
 * 
 * Phase A2: Complete Firebase auth integration.
 */
class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    companion object {
        private const val TIMEOUT_MS = 30_000L // 30 seconds
    }

    /**
     * Login with email and password using Firebase.
     */
    override suspend fun login(email: String, password: String): Result<AuthResult> {
        return try {
            withTimeout(TIMEOUT_MS) {
                // Sign in with Firebase
                val authResult = firebaseAuth
                    .signInWithEmailAndPassword(email, password)
                    .await()

                val firebaseUser = authResult.user
                    ?: return@withTimeout Result.Error(AppError.AuthenticationError("Login failed"))

                // Get Firebase ID token & claims
                val tokenResult = firebaseUser.getIdToken(false).await()
                val idToken = tokenResult.token
                    ?: return@withTimeout Result.Error(AppError.AuthenticationError("Failed to get ID token"))

                val roleClaim = tokenResult.claims["role"] as? String ?: "STUDENT"

                // Map to domain User
                val user = mapFirebaseUserToDomainUser(firebaseUser, roleClaim)

                Result.Success(
                    AuthResult(
                        user = user,
                        idToken = idToken,
                        expiresIn = 3600L // Firebase tokens expire in 1 hour
                    )
                )
            }
        } catch (e: FirebaseAuthException) {
            Result.Error(mapFirebaseAuthException(e))
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError("Login failed: ${e.message}"))
        }
    }

    /**
     * Register new user with Firebase Authentication.
     */
    override suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        role: String
    ): Result<AuthResult> {
        return try {
            withTimeout(TIMEOUT_MS) {
                // Create user in Firebase Auth
                val authResult = firebaseAuth
                    .createUserWithEmailAndPassword(email, password)
                    .await()

                val firebaseUser = authResult.user
                    ?: return@withTimeout Result.Error(AppError.AuthenticationError("Registration failed"))

                // Update profile with display name
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName("$firstName $lastName")
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()

                // Get Firebase ID token
                val idToken = firebaseUser.getIdToken(false).await().token
                    ?: return@withTimeout Result.Error(AppError.AuthenticationError("Failed to get ID token"))

                // Map to domain User (role will be set by backend)
                val user = mapFirebaseUserToDomainUser(firebaseUser, role)

                Result.Success(
                    AuthResult(
                        user = user,
                        idToken = idToken,
                        expiresIn = 3600L
                    )
                )
            }
        } catch (e: FirebaseAuthException) {
            Result.Error(mapFirebaseAuthException(e))
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError("Registration failed: ${e.message}"))
        }
    }

    /**
     * Sign in or register with Google credential.
     */
    override suspend fun signInWithGoogle(idToken: String): Result<AuthResult> {
        return try {
            withTimeout(TIMEOUT_MS) {
                val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
                val authResult = firebaseAuth.signInWithCredential(credential).await()

                val firebaseUser = authResult.user
                    ?: return@withTimeout Result.Error(AppError.AuthenticationError("Google Sign-In failed"))

                val tokenResult = firebaseUser.getIdToken(false).await()
                val firebaseIdToken = tokenResult.token
                    ?: return@withTimeout Result.Error(AppError.AuthenticationError("Failed to get ID token"))

                val roleClaim = tokenResult.claims["role"] as? String ?: "STUDENT"
                val user = mapFirebaseUserToDomainUser(firebaseUser, roleClaim)

                Result.Success(
                    AuthResult(
                        user = user,
                        idToken = firebaseIdToken,
                        expiresIn = 3600L
                    )
                )
            }
        } catch (e: FirebaseAuthException) {
            Result.Error(mapFirebaseAuthException(e))
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError("Google Sign-In failed: ${e.message}"))
        }
    }

    /**
     * Logout (sign out from Firebase).
     */
    override suspend fun logout(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.UnknownError("Logout failed: ${e.message}"))
        }
    }

    /**
     * Get current authenticated user from Firebase.
     */
    override suspend fun getCurrentUser(): Result<User> {
        val firebaseUser = firebaseAuth.currentUser
            ?: return Result.Error(AppError.AuthenticationError("Not authenticated"))

        return try {
            // Reload user to get latest data
            firebaseUser.reload().await()
            val tokenResult = firebaseUser.getIdToken(false).await()
            val roleClaim = tokenResult.claims["role"] as? String ?: "STUDENT"
            val user = mapFirebaseUserToDomainUser(firebaseUser, roleClaim)
            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError("Failed to get current user: ${e.message}"))
        }
    }

    /**
     * Get Firebase ID token (auto-refreshed if expired).
     */
    override suspend fun getIdToken(forceRefresh: Boolean): Result<String> {
        val firebaseUser = firebaseAuth.currentUser
            ?: return Result.Error(AppError.AuthenticationError("Not authenticated"))

        return try {
            val tokenResult = firebaseUser.getIdToken(forceRefresh).await()
            val token = tokenResult.token
                ?: return Result.Error(AppError.AuthenticationError("Failed to get ID token"))
            
            Result.Success(token)
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError("Failed to get ID token: ${e.message}"))
        }
    }

    /**
     * Check if user is authenticated.
     */
    override suspend fun isAuthenticated(): Boolean {
        return firebaseAuth.currentUser != null
    }

    /**
     * Send password reset email.
     */
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            withTimeout(TIMEOUT_MS) {
                firebaseAuth.sendPasswordResetEmail(email).await()
                Result.Success(Unit)
            }
        } catch (e: FirebaseAuthException) {
            Result.Error(mapFirebaseAuthException(e))
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError("Failed to send reset email: ${e.message}"))
        }
    }

    /**
     * Update user password.
     */
    override suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit> {
        val firebaseUser = firebaseAuth.currentUser
            ?: return Result.Error(AppError.AuthenticationError("Not authenticated"))

        return try {
            withTimeout(TIMEOUT_MS) {
                // Re-authenticate first
                reauthenticate(currentPassword).let { result ->
                    if (result is Result.Error) return@withTimeout result
                }

                // Update password
                firebaseUser.updatePassword(newPassword).await()
                Result.Success(Unit)
            }
        } catch (e: FirebaseAuthException) {
            Result.Error(mapFirebaseAuthException(e))
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError("Failed to update password: ${e.message}"))
        }
    }

    /**
     * Re-authenticate user (required for sensitive operations).
     */
    override suspend fun reauthenticate(password: String): Result<Unit> {
        val firebaseUser = firebaseAuth.currentUser
            ?: return Result.Error(AppError.AuthenticationError("Not authenticated"))

        val email = firebaseUser.email
            ?: return Result.Error(AppError.AuthenticationError("Email not available"))

        return try {
            withTimeout(TIMEOUT_MS) {
                val credential = com.google.firebase.auth.EmailAuthProvider
                    .getCredential(email, password)
                firebaseUser.reauthenticate(credential).await()
                Result.Success(Unit)
            }
        } catch (e: FirebaseAuthException) {
            Result.Error(mapFirebaseAuthException(e))
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError("Re-authentication failed: ${e.message}"))
        }
    }

    /**
     * Map Firebase user to domain User model.
     */
    private fun mapFirebaseUserToDomainUser(firebaseUser: FirebaseUser, role: String = "STUDENT"): User {
        // Parse display name
        val displayName = firebaseUser.displayName ?: ""
        val nameParts = displayName.split(" ", limit = 2)
        val firstName = nameParts.getOrNull(0) ?: ""
        val lastName = nameParts.getOrNull(1) ?: ""

        return User(
            id = 0, // Will be set by backend
            firebaseUid = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            firstName = firstName,
            lastName = lastName,
            role = role,
            profilePictureUrl = firebaseUser.photoUrl?.toString()
        )
    }

    /**
     * Map Firebase auth exceptions to domain errors.
     */
    private fun mapFirebaseAuthException(exception: FirebaseAuthException): AppError {
        return when (exception.errorCode) {
            "ERROR_INVALID_EMAIL" -> AppError.ValidationError(listOf("Invalid email format"))
            "ERROR_WRONG_PASSWORD" -> AppError.AuthenticationError("Invalid password")
            "ERROR_USER_NOT_FOUND" -> AppError.AuthenticationError("User not found")
            "ERROR_USER_DISABLED" -> AppError.AuthenticationError("Account has been disabled")
            "ERROR_EMAIL_ALREADY_IN_USE" -> AppError.ValidationError(listOf("Email already in use"))
            "ERROR_WEAK_PASSWORD" -> AppError.ValidationError(listOf("Password is too weak"))
            "ERROR_NETWORK_REQUEST_FAILED" -> AppError.NetworkError("Network error")
            "ERROR_TOO_MANY_REQUESTS" -> AppError.AuthenticationError("Too many requests. Try again later")
            else -> AppError.UnknownError("Authentication error: ${exception.message}")
        }
    }
}
