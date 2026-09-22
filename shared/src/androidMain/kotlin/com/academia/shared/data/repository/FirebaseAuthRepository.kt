package com.academia.shared.data.repository

import com.academia.shared.domain.model.User
import com.academia.shared.util.AppError
import com.academia.shared.util.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.EmailAuthProvider
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Real Firebase Authentication implementation.
 *
 * Flow for email/password:
 *  register()  → Firebase createUser → updateProfile(displayName) → send email verification → return AuthResult
 *  login()     → Firebase signIn → verify email check → fetch ID token + role claim → return AuthResult
 *
 * Flow for Google:
 *  signInWithGoogle() → Firebase signInWithCredential(GoogleAuthProvider) → fetch ID token + role claim → return AuthResult
 *
 * Role source-of-truth:
 *  - On register:   role is supplied by the caller (from registration form) and stored in AuthResult.user.role
 *  - On login:      role is read from Firebase ID token custom claim "role" (set by backend after syncUser)
 *                   Falls back to "STUDENT" if the claim is absent (first login before backend sets it)
 *  - On Google:     same as login — custom claim, fallback STUDENT
 *
 * The caller (ViewModel) is responsible for calling backend /api/auth/sync after receiving AuthResult
 * to persist the user and get the authoritative role from the DB.
 */
class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    companion object {
        private const val TIMEOUT_MS = 30_000L
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    override suspend fun login(email: String, password: String): Result<AuthResult> {
        return try {
            withTimeout(TIMEOUT_MS) {
                val authResult = firebaseAuth
                    .signInWithEmailAndPassword(email, password)
                    .await()

                val firebaseUser = authResult.user
                    ?: return@withTimeout Result.Error(AppError.AuthenticationError("Login failed"))

                // Enforce email verification
                if (!firebaseUser.isEmailVerified) {
                    // Re-send verification so user can try again
                    firebaseUser.sendEmailVerification().await()
                    return@withTimeout Result.Error(
                        AppError.AuthenticationError(
                            "Please verify your email before signing in. A new verification email has been sent."
                        )
                    )
                }

                val tokenResult = firebaseUser.getIdToken(false).await()
                val idToken = tokenResult.token
                    ?: return@withTimeout Result.Error(AppError.AuthenticationError("Failed to get ID token"))

                // Role from custom claim (set by backend after first syncUser call)
                // Falls back to "STUDENT" on very first login before backend sync runs
                val roleClaim = tokenResult.claims["role"] as? String ?: "STUDENT"

                Result.Success(
                    AuthResult(
                        user = mapFirebaseUser(firebaseUser, roleClaim),
                        idToken = idToken,
                        expiresIn = 3600L
                    )
                )
            }
        } catch (e: FirebaseAuthException) {
            Result.Error(mapError(e))
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError("Login failed: ${e.message}"))
        }
    }

    // ── Register ──────────────────────────────────────────────────────────────

    override suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        role: String
    ): Result<AuthResult> {
        return try {
            withTimeout(TIMEOUT_MS) {
                val authResult = firebaseAuth
                    .createUserWithEmailAndPassword(email, password)
                    .await()

                val firebaseUser = authResult.user
                    ?: return@withTimeout Result.Error(AppError.AuthenticationError("Registration failed"))

                // Set display name so mapFirebaseUser can parse firstName/lastName
                firebaseUser.updateProfile(
                    UserProfileChangeRequest.Builder()
                        .setDisplayName("$firstName $lastName")
                        .build()
                ).await()

                // Send email verification — user must verify before they can log in
                firebaseUser.sendEmailVerification().await()

                // Get ID token immediately (used for the backend sync call)
                val idToken = firebaseUser.getIdToken(false).await().token
                    ?: return@withTimeout Result.Error(AppError.AuthenticationError("Failed to get ID token"))

                Result.Success(
                    AuthResult(
                        user = mapFirebaseUser(firebaseUser, role),
                        idToken = idToken,
                        expiresIn = 3600L
                    )
                )
            }
        } catch (e: FirebaseAuthException) {
            Result.Error(mapError(e))
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError("Registration failed: ${e.message}"))
        }
    }

    // ── Google Sign-In ────────────────────────────────────────────────────────

    override suspend fun signInWithGoogle(idToken: String): Result<AuthResult> {
        return try {
            withTimeout(TIMEOUT_MS) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = firebaseAuth.signInWithCredential(credential).await()

                val firebaseUser = authResult.user
                    ?: return@withTimeout Result.Error(AppError.AuthenticationError("Google Sign-In failed"))

                val tokenResult = firebaseUser.getIdToken(false).await()
                val firebaseIdToken = tokenResult.token
                    ?: return@withTimeout Result.Error(AppError.AuthenticationError("Failed to get ID token"))

                val roleClaim = tokenResult.claims["role"] as? String ?: "STUDENT"

                Result.Success(
                    AuthResult(
                        user = mapFirebaseUser(firebaseUser, roleClaim),
                        idToken = firebaseIdToken,
                        expiresIn = 3600L
                    )
                )
            }
        } catch (e: FirebaseAuthException) {
            Result.Error(mapError(e))
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError("Google Sign-In failed: ${e.message}"))
        }
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    override suspend fun logout(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.UnknownError("Logout failed: ${e.message}"))
        }
    }

    // ── Current user ──────────────────────────────────────────────────────────

    override suspend fun getCurrentUser(): Result<User> {
        val firebaseUser = firebaseAuth.currentUser
            ?: return Result.Error(AppError.AuthenticationError("Not authenticated"))

        return try {
            firebaseUser.reload().await()
            val tokenResult = firebaseUser.getIdToken(false).await()
            val roleClaim = tokenResult.claims["role"] as? String ?: "STUDENT"
            Result.Success(mapFirebaseUser(firebaseUser, roleClaim))
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError("Failed to get current user: ${e.message}"))
        }
    }

    override suspend fun getIdToken(forceRefresh: Boolean): Result<String> {
        val firebaseUser = firebaseAuth.currentUser
            ?: return Result.Error(AppError.AuthenticationError("Not authenticated"))

        return try {
            val token = firebaseUser.getIdToken(forceRefresh).await().token
                ?: return Result.Error(AppError.AuthenticationError("Failed to get ID token"))
            Result.Success(token)
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError("Failed to get ID token: ${e.message}"))
        }
    }

    override suspend fun isAuthenticated(): Boolean = firebaseAuth.currentUser != null

    // ── Password management ───────────────────────────────────────────────────

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            withTimeout(TIMEOUT_MS) {
                firebaseAuth.sendPasswordResetEmail(email).await()
                Result.Success(Unit)
            }
        } catch (e: FirebaseAuthException) {
            Result.Error(mapError(e))
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError("Failed to send reset email: ${e.message}"))
        }
    }

    override suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit> {
        val firebaseUser = firebaseAuth.currentUser
            ?: return Result.Error(AppError.AuthenticationError("Not authenticated"))

        return try {
            withTimeout(TIMEOUT_MS) {
                // Must re-authenticate before changing password
                val reauthResult = reauthenticate(currentPassword)
                if (reauthResult is Result.Error) return@withTimeout reauthResult

                firebaseUser.updatePassword(newPassword).await()
                Result.Success(Unit)
            }
        } catch (e: FirebaseAuthException) {
            Result.Error(mapError(e))
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError("Failed to update password: ${e.message}"))
        }
    }

    override suspend fun reauthenticate(password: String): Result<Unit> {
        val firebaseUser = firebaseAuth.currentUser
            ?: return Result.Error(AppError.AuthenticationError("Not authenticated"))
        val email = firebaseUser.email
            ?: return Result.Error(AppError.AuthenticationError("Email not available"))

        return try {
            withTimeout(TIMEOUT_MS) {
                firebaseUser.reauthenticate(
                    EmailAuthProvider.getCredential(email, password)
                ).await()
                Result.Success(Unit)
            }
        } catch (e: FirebaseAuthException) {
            Result.Error(mapError(e))
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError("Re-authentication failed: ${e.message}"))
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun mapFirebaseUser(user: FirebaseUser, role: String): User {
        val displayName = user.displayName?.trim() ?: ""
        val parts = displayName.split(" ", limit = 2)
        return User(
            id = 0,
            firebaseUid = user.uid,
            email = user.email ?: "",
            firstName = parts.getOrElse(0) { "" },
            lastName = parts.getOrElse(1) { "" },
            role = role,
            profilePictureUrl = user.photoUrl?.toString(),
            isActive = true
        )
    }

    /**
     * Maps Firebase error codes to user-friendly domain errors.
     * Firebase SDK (26+) uses getErrorCode() which returns strings like "ERROR_USER_NOT_FOUND".
     */
    private fun mapError(e: FirebaseAuthException): AppError {
        return when (e.errorCode) {
            "ERROR_INVALID_EMAIL"           -> AppError.ValidationError(listOf("Invalid email address"))
            "ERROR_WRONG_PASSWORD",
            "ERROR_INVALID_CREDENTIAL"      -> AppError.AuthenticationError("Incorrect email or password")
            "ERROR_USER_NOT_FOUND"          -> AppError.AuthenticationError("No account found with this email")
            "ERROR_USER_DISABLED"           -> AppError.AuthenticationError("This account has been disabled")
            "ERROR_EMAIL_ALREADY_IN_USE"    -> AppError.ValidationError(listOf("An account with this email already exists"))
            "ERROR_WEAK_PASSWORD"           -> AppError.ValidationError(listOf("Password is too weak"))
            "ERROR_NETWORK_REQUEST_FAILED"  -> AppError.NetworkError("Network error. Check your connection.")
            "ERROR_TOO_MANY_REQUESTS"       -> AppError.AuthenticationError("Too many attempts. Try again later.")
            "ERROR_OPERATION_NOT_ALLOWED"   -> AppError.AuthenticationError("Sign-in method not enabled")
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
                AppError.AuthenticationError("An account already exists with a different sign-in method")
            else -> AppError.AuthenticationError(e.message ?: "Authentication failed")
        }
    }
}
