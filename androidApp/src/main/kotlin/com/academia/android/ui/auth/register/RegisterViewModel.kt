package com.academia.android.ui.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.academia.android.ui.auth.*
import com.academia.android.data.UserPreferencesStore
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
 * ViewModel for the registration screen.
 *
 * Real auth flow:
 *  1. Firebase creates the user account (email/password or Google)
 *  2. POST /api/auth/sync  → backend persists user + role in DB
 *  3. Authoritative role from DB response is saved in DataStore
 *  4. NavigateToHome event fired — AppViewModel picks up cached role
 *
 * After registration the user receives a verification email.
 * They must verify before they can log in with email/password.
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val userPreferences: UserPreferencesStore
) : ViewModel() {

    private val _formState = MutableStateFlow(RegisterFormState())
    val formState: StateFlow<RegisterFormState> = _formState.asStateFlow()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = Channel<AuthUiEvent>(Channel.BUFFERED)
    val events: Flow<AuthUiEvent> = _events.receiveAsFlow()

    // ── Form field updates ────────────────────────────────────────────────────

    fun onRoleChange(role: UserRole) = _formState.update { it.copy(role = role) }

    fun onFirstNameChange(v: String) { _formState.update { it.copy(firstName = v) }; validate() }
    fun onLastNameChange(v: String)  { _formState.update { it.copy(lastName  = v) }; validate() }
    fun onEmailChange(v: String)     { _formState.update { it.copy(email     = v) }; validate() }
    fun onPasswordChange(v: String)  { _formState.update { it.copy(password  = v) }; validate() }
    fun onConfirmPasswordChange(v: String) { _formState.update { it.copy(confirmPassword = v) }; validate() }

    // ── Email / password registration ─────────────────────────────────────────

    fun onRegisterClick() {
        val form = _formState.value
        if (!form.validation.isValid) return
        if (form.password != form.confirmPassword) {
            _uiState.value = AuthUiState.Error("Passwords do not match")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            // Step 1 — Firebase registration
            val firebaseResult = authRepository.register(
                email     = form.email,
                password  = form.password,
                firstName = form.firstName,
                lastName  = form.lastName,
                role      = form.role.value
            )

            when (firebaseResult) {
                is Result.Error -> {
                    _uiState.value = AuthUiState.Error(
                        firebaseResult.error.message ?: "Registration failed"
                    )
                    return@launch
                }
                is Result.Success -> {
                    val authResult = firebaseResult.data

                    // Step 2 — sync to backend DB (creates the user record + sets role)
                    val syncResult = userRepository.syncUser(
                        firebaseUid = authResult.user.firebaseUid,
                        email       = authResult.user.email,
                        firstName   = form.firstName,
                        lastName    = form.lastName,
                        role        = form.role.value
                    )

                    val finalRole = when (syncResult) {
                        is Result.Success -> syncResult.data.role   // authoritative from DB
                        is Result.Error   -> form.role.value         // fallback if backend unreachable
                    }

                    // Step 3 — cache role locally
                    userPreferences.saveUser(
                        firebaseUid = authResult.user.firebaseUid,
                        email       = authResult.user.email,
                        displayName = "${form.firstName} ${form.lastName}",
                        role        = finalRole
                    )

                    // Registration succeeded — user still needs to verify email before logging in
                    val verifiedUser = authResult.user.copy(role = finalRole)
                    _uiState.value = AuthUiState.Success(verifiedUser)
                    _events.send(
                        AuthUiEvent.ShowSuccess(
                            "Account created! Please check your email to verify your account before signing in."
                        )
                    )
                    _events.send(AuthUiEvent.NavigateToHome)
                }
            }
        }
    }

    // ── Google Sign-In / Sign-Up ──────────────────────────────────────────────

    fun onGoogleSignIn(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            // Step 1 — Firebase Google auth
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

                    // Step 2 — sync to backend (role defaults to STUDENT for Google sign-ups;
                    // user can change role later from profile)
                    val syncResult = userRepository.syncUser(
                        firebaseUid = authResult.user.firebaseUid,
                        email       = authResult.user.email,
                        firstName   = authResult.user.firstName,
                        lastName    = authResult.user.lastName,
                        role        = authResult.user.role.ifBlank { "STUDENT" }
                    )

                    val finalRole = when (syncResult) {
                        is Result.Success -> syncResult.data.role
                        is Result.Error   -> authResult.user.role.ifBlank { "STUDENT" }
                    }

                    // Step 3 — cache role locally
                    userPreferences.saveUser(
                        firebaseUid = authResult.user.firebaseUid,
                        email       = authResult.user.email,
                        displayName = authResult.user.fullName,
                        role        = finalRole
                    )

                    val finalUser = authResult.user.copy(role = finalRole)
                    _uiState.value = AuthUiState.Success(finalUser)
                    _events.send(AuthUiEvent.NavigateToHome)
                }
            }
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private fun validate() {
        val form = _formState.value

        val emailError = when {
            form.email.isBlank()               -> "Email is required"
            !EMAIL_REGEX.matches(form.email)   -> "Invalid email format"
            else                               -> null
        }

        val passwordError = when {
            form.password.isBlank()                          -> "Password is required"
            form.password.length < 8                         -> "At least 8 characters required"
            !form.password.any { it.isUpperCase() }          -> "Must contain an uppercase letter"
            !form.password.any { it.isLowerCase() }          -> "Must contain a lowercase letter"
            !form.password.any { it.isDigit() }              -> "Must contain a number"
            else                                             -> null
        }

        val nameError = when {
            form.firstName.isBlank() || form.lastName.isBlank() -> "First and last name are required"
            form.firstName.length < 2 || form.lastName.length < 2 -> "Name must be at least 2 characters"
            else                                                 -> null
        }

        val isValid = emailError == null &&
                passwordError == null &&
                nameError == null &&
                form.confirmPassword.isNotBlank() &&
                form.password == form.confirmPassword

        _formState.update {
            it.copy(
                validation = FormValidation(
                    isValid       = isValid,
                    emailError    = emailError,
                    passwordError = passwordError,
                    nameError     = nameError
                )
            )
        }
    }

    fun clearError() {
        if (_uiState.value is AuthUiState.Error) _uiState.value = AuthUiState.Idle
    }
}
