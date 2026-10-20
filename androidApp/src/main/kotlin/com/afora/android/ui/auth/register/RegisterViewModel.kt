package com.afora.android.ui.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afora.android.ui.auth.*
import com.afora.android.data.UserPreferencesStore
import com.afora.shared.data.repository.AuthMessages
import com.afora.shared.data.repository.AuthRepository
import com.afora.shared.data.repository.AuthResult
import com.afora.shared.data.repository.UserRepository
import com.afora.shared.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

/** Shortest roll number / employee id accepted by the form. */
internal const val MIN_ID_LENGTH = 3

/**
 * ViewModel for the registration screen.
 *
 * Real auth flow:
 *  1. Firebase creates the user account (email/password or Google)
 *  2. POST /api/auth/sync -> the backend creates the user and its student or teacher
 *     record from the roll number / employee id
 *  3. The role stored by the backend is cached in DataStore
 *  4. NavigateToHome event fired
 *
 * Step 2 must succeed. If it fails the error is shown rather than carrying on with a
 * guessed role: an account with no backend record cannot load anything. For an
 * email/password sign-up the Firebase account created in step 1 is deleted again, so
 * the user can fix the problem (for example a roll number already in use) and retry
 * with the same email. A Google account may already exist, so it is only signed out.
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

    // -- Form field updates ----------------------------------------------------

    fun onRoleChange(role: UserRole) { _formState.update { it.copy(role = role) }; validate() }

    fun onFirstNameChange(v: String) { _formState.update { it.copy(firstName = v) }; validate() }
    fun onLastNameChange(v: String)  { _formState.update { it.copy(lastName  = v) }; validate() }
    fun onIdNumberChange(v: String)  { _formState.update { it.copy(idNumber  = v) }; validate() }
    fun onEmailChange(v: String)     { _formState.update { it.copy(email     = v) }; validate() }
    fun onPasswordChange(v: String)  { _formState.update { it.copy(password  = v) }; validate() }
    fun onConfirmPasswordChange(v: String) { _formState.update { it.copy(confirmPassword = v) }; validate() }

    // -- Email / password registration ---------------------------------------------

    fun onRegisterClick() {
        val form = _formState.value
        if (!form.validation.isValid) return
        if (form.password != form.confirmPassword) {
            _uiState.value = AuthUiState.Error("Passwords do not match")
            return
        }
        if (_uiState.value is AuthUiState.Loading) return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            // Step 1 -- Firebase registration
            var firebaseResult: Result<AuthResult> = authRepository.register(
                email     = form.email,
                password  = form.password,
                firstName = form.firstName,
                lastName  = form.lastName,
                role      = form.role.value
            )

            // The email may already exist in Firebase without a backend profile: an earlier
            // sign-up that never reached the backend, or one that was interrupted. If the
            // password entered is that account's password, sign in to it and finish creating
            // the profile instead of leaving the user stuck.
            var createdHere = true
            val failure = (firebaseResult as? Result.Error)?.error
            if (failure != null && failure.message == AuthMessages.EMAIL_ALREADY_IN_USE) {
                val signIn = authRepository.login(form.email, form.password)
                if (signIn is Result.Success) {
                    firebaseResult = signIn
                    createdHere = false
                } else {
                    _uiState.value = AuthUiState.Error(
                        "${AuthMessages.EMAIL_ALREADY_IN_USE}. Log in with its password, or use a different email."
                    )
                    return@launch
                }
            }

            when (firebaseResult) {
                is Result.Error -> {
                    _uiState.value = AuthUiState.Error(
                        firebaseResult.error.message ?: "Registration failed"
                    )
                    return@launch
                }
                is Result.Success -> {
                    val authResult = firebaseResult.data

                    // Step 2 -- create the backend record (user + student/teacher)
                    val syncResult = userRepository.syncUser(
                        firebaseUid = authResult.user.firebaseUid,
                        email       = authResult.user.email,
                        firstName   = form.firstName,
                        lastName    = form.lastName,
                        role        = form.role.value,
                        idNumber    = form.idNumber.trim()
                    )

                    if (syncResult is Result.Error) {
                        // Roll back an account we just created so the user can retry. An account
                        // that already existed is never deleted, only signed out.
                        if (!createdHere || authRepository.deleteCurrentAccount() is Result.Error) {
                            authRepository.logout()
                        }
                        _uiState.value = AuthUiState.Error(syncResult.error.message)
                        return@launch
                    }

                    val finalRole = (syncResult as Result.Success).data.role   // authoritative from DB

                    // Step 3 -- cache role locally
                    userPreferences.saveUser(
                        firebaseUid = authResult.user.firebaseUid,
                        email       = authResult.user.email,
                        displayName = "${form.firstName} ${form.lastName}",
                        role        = finalRole
                    )

                    // Registration succeeded -- user still needs to verify email before logging in
                    val verifiedUser = authResult.user.copy(role = finalRole)
                    _uiState.value = AuthUiState.Success(verifiedUser)
                    if (createdHere) {
                        _events.send(
                            AuthUiEvent.ShowSuccess(
                                "Account created! Please check your email to verify your account before signing in."
                            )
                        )
                    }
                    _events.send(AuthUiEvent.NavigateToHome)
                }
            }
        }
    }

    // -- Google Sign-In / Sign-Up ----------------------------------------------------

    /**
     * Google sign-up uses the role and roll number / employee id already entered in
     * the form, because the backend needs them to create the account.
     */
    fun onGoogleSignIn(idToken: String) {
        val form = _formState.value
        if (!form.isIdValid) {
            _uiState.value = AuthUiState.Error("Enter your ${form.role.idLabel.lowercase()} before continuing with Google")
            return
        }
        if (_uiState.value is AuthUiState.Loading) return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            // Step 1 -- Firebase Google auth
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

                    // Step 2 -- create or fetch the backend record
                    val syncResult = userRepository.syncUser(
                        firebaseUid = authResult.user.firebaseUid,
                        email       = authResult.user.email,
                        firstName   = authResult.user.firstName,
                        lastName    = authResult.user.lastName,
                        role        = form.role.value,
                        idNumber    = form.idNumber.trim()
                    )

                    if (syncResult is Result.Error) {
                        // The Google account may already exist, so only sign out.
                        authRepository.logout()
                        _uiState.value = AuthUiState.Error(syncResult.error.message)
                        return@launch
                    }

                    val syncedUser = (syncResult as Result.Success).data
                    val finalRole = syncedUser.role

                    // Step 3 -- cache role locally
                    userPreferences.saveUser(
                        firebaseUid = authResult.user.firebaseUid,
                        email       = authResult.user.email,
                        displayName = authResult.user.fullName,
                        role        = finalRole
                    )

                    _uiState.value = AuthUiState.Success(authResult.user.copy(role = finalRole))
                    _events.send(AuthUiEvent.NavigateToHome)
                }
            }
        }
    }

    // -- Validation ------------------------------------------------------------------

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

        val idNumberError = when {
            form.idNumber.isBlank()                    -> "${form.role.idLabel} is required"
            form.idNumber.trim().length < MIN_ID_LENGTH -> "${form.role.idLabel} looks too short"
            else                                       -> null
        }

        val isValid = emailError == null &&
                passwordError == null &&
                nameError == null &&
                idNumberError == null &&
                form.confirmPassword.isNotBlank() &&
                form.password == form.confirmPassword

        _formState.update {
            it.copy(
                validation = FormValidation(
                    isValid       = isValid,
                    emailError    = emailError,
                    passwordError = passwordError,
                    nameError     = nameError,
                    idNumberError = idNumberError
                )
            )
        }
    }

    fun clearError() {
        if (_uiState.value is AuthUiState.Error) _uiState.value = AuthUiState.Idle
    }
}