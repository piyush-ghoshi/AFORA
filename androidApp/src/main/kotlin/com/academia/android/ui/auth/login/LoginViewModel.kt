package com.academia.android.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.academia.android.data.UserPreferencesStore
import com.academia.android.ui.auth.AuthUiEvent
import com.academia.android.ui.auth.AuthUiState
import com.academia.android.ui.auth.FormValidation
import com.academia.android.ui.auth.LoginFormState
import com.academia.shared.data.repository.AuthRepository
import com.academia.shared.data.repository.UserRepository
import com.academia.shared.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

/**
 * ViewModel for the login screen.
 *
 * Real auth flow:
 *  1. Firebase signs the user in
 *  2. POST /api/auth/sync  → backend updates last_login_at + returns authoritative role
 *  3. Role saved in DataStore for instant startup routing
 *  4. NavigateToHome event fired
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val userPreferences: UserPreferencesStore
) : ViewModel() {

    private val _formState = MutableStateFlow(LoginFormState())
    val formState: StateFlow<LoginFormState> = _formState.asStateFlow()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = Channel<AuthUiEvent>(Channel.BUFFERED)
    val events: Flow<AuthUiEvent> = _events.receiveAsFlow()

    // ── Form field updates ────────────────────────────────────────────────────

    fun onEmailChange(email: String)       { _formState.update { it.copy(email = email) };       validate() }
    fun onPasswordChange(password: String) { _formState.update { it.copy(password = password) }; validate() }
    fun onRememberMeChange(v: Boolean)     { _formState.update { it.copy(rememberMe = v) } }

    // ── Email / password login ────────────────────────────────────────────────

    fun onLoginClick() {
        val form = _formState.value
        if (!form.validation.isValid) return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            // Step 1 — Firebase authentication (also enforces email verification)
            val firebaseResult = authRepository.login(form.email, form.password)

            when (firebaseResult) {
                is Result.Error -> {
                    _uiState.value = AuthUiState.Error(
                        firebaseResult.error.message ?: "Login failed"
                    )
                    return@launch
                }
                is Result.Success -> {
                    val authResult = firebaseResult.data
                    completeSignIn(
                        firebaseUid = authResult.user.firebaseUid,
                        email       = authResult.user.email,
                        displayName = authResult.user.fullName,
                        roleHint    = authResult.user.role   // from Firebase custom claim
                    )
                }
            }
        }
    }

    // ── Google Sign-In ────────────────────────────────────────────────────────

    fun onGoogleSignIn(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            val firebaseResult = authRepository.signInWithGoogle(idToken)

            when (firebaseResult) {
                is Result.Error -> {
                    _uiState.value = AuthUiState.Error(
                        firebaseResult.error.message ?: "Google Sign-In failed"
                    )
                    return@launch
                }
                is Result.Success -> {
                    val authResult = firebaseResult.data
                    completeSignIn(
                        firebaseUid = authResult.user.firebaseUid,
                        email       = authResult.user.email,
                        displayName = authResult.user.fullName,
                        roleHint    = authResult.user.role
                    )
                }
            }
        }
    }

    // ── Shared post-Firebase sign-in logic ────────────────────────────────────

    /**
     * After Firebase auth succeeds, sync to backend and cache the authoritative role.
     */
    private suspend fun completeSignIn(
        firebaseUid: String,
        email: String,
        displayName: String,
        roleHint: String
    ) {
        // Step 2 — Backend sync: updates last_login_at, returns DB user with real role
        val syncResult = userRepository.syncUser(
            firebaseUid = firebaseUid,
            email       = email,
            firstName   = displayName.substringBefore(" "),
            lastName    = displayName.substringAfter(" ", ""),
            role        = roleHint.ifBlank { "STUDENT" }
        )

        val finalRole = when (syncResult) {
            is Result.Success -> syncResult.data.role
            is Result.Error   -> roleHint.ifBlank { "STUDENT" }   // fallback if backend offline
        }

        // Step 3 — Cache role in DataStore
        userPreferences.saveUser(
            firebaseUid = firebaseUid,
            email       = email,
            displayName = displayName,
            role        = finalRole
        )

        // Build final user from backend response or fallback
        val finalUser = when (syncResult) {
            is Result.Success -> syncResult.data
            is Result.Error   -> com.academia.shared.domain.model.User(
                firebaseUid = firebaseUid,
                email       = email,
                firstName   = displayName.substringBefore(" "),
                lastName    = displayName.substringAfter(" ", ""),
                role        = finalRole
            )
        }

        _uiState.value = AuthUiState.Success(finalUser)
        _events.send(AuthUiEvent.NavigateToHome)
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private fun validate() {
        val form = _formState.value

        val emailError = when {
            form.email.isBlank()             -> "Email is required"
            !EMAIL_REGEX.matches(form.email) -> "Invalid email format"
            else                             -> null
        }

        val passwordError = when {
            form.password.isBlank()       -> "Password is required"
            form.password.length < 6      -> "Password must be at least 6 characters"
            else                          -> null
        }

        _formState.update {
            it.copy(
                validation = FormValidation(
                    isValid       = emailError == null && passwordError == null,
                    emailError    = emailError,
                    passwordError = passwordError
                )
            )
        }
    }

    fun clearError() {
        if (_uiState.value is AuthUiState.Error) _uiState.value = AuthUiState.Idle
    }
}
