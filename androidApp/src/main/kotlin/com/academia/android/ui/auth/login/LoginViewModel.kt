package com.academia.android.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.academia.android.ui.auth.AuthUiEvent
import com.academia.android.ui.auth.AuthUiState
import com.academia.android.ui.auth.FormValidation
import com.academia.android.ui.auth.LoginFormState
import com.academia.shared.data.repository.AuthRepository
import com.academia.shared.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

/**
 * ViewModel for login screen.
 * 
 * Handles:
 * - Form state and validation
 * - Firebase authentication
 * - UI state management
 * - Navigation events
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // Form state
    private val _formState = MutableStateFlow(LoginFormState())
    val formState: StateFlow<LoginFormState> = _formState.asStateFlow()

    // UI state
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // One-time events
    private val _events = Channel<AuthUiEvent>(Channel.BUFFERED)
    val events: Flow<AuthUiEvent> = _events.receiveAsFlow()

    /**
     * Update email and validate.
     */
    fun onEmailChange(email: String) {
        _formState.update { it.copy(email = email) }
        validateForm()
    }

    /**
     * Update password and validate.
     */
    fun onPasswordChange(password: String) {
        _formState.update { it.copy(password = password) }
        validateForm()
    }

    /**
     * Toggle remember me.
     */
    fun onRememberMeChange(remember: Boolean) {
        _formState.update { it.copy(rememberMe = remember) }
    }

    /**
     * Perform login with Firebase.
     */
    fun onLoginClick() {
        val form = _formState.value
        
        if (!form.validation.isValid) {
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            when (val result = authRepository.login(form.email, form.password)) {
                is Result.Success -> {
                    _uiState.value = AuthUiState.Success(result.data.user)
                    _events.send(AuthUiEvent.NavigateToHome)
                    
                    // TODO: Save rememberMe preference if enabled
                    // if (form.rememberMe) { saveUserSession() }
                }
                is Result.Error -> {
                    _uiState.value = AuthUiState.Error(
                        result.error.message ?: "Login failed. Please try again."
                    )
                }
            }
        }
    }

    /**
     * Perform login with Google ID token.
     */
    fun onGoogleSignIn(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            when (val result = authRepository.signInWithGoogle(idToken)) {
                is Result.Success -> {
                    _uiState.value = AuthUiState.Success(result.data.user)
                    _events.send(AuthUiEvent.NavigateToHome)
                }
                is Result.Error -> {
                    _uiState.value = AuthUiState.Error(
                        result.error.message ?: "Google Sign-In failed. Please try again."
                    )
                }
            }
        }
    }

    /**
     * Validate form fields.
     */
    private fun validateForm() {
        val form = _formState.value
        
        val emailError = when {
            form.email.isBlank() -> "Email is required"
            !EMAIL_REGEX.matches(form.email) -> "Invalid email format"
            else -> null
        }
        
        val passwordError = when {
            form.password.isBlank() -> "Password is required"
            form.password.length < 6 -> "Password must be at least 6 characters"
            else -> null
        }
        
        val isValid = emailError == null && passwordError == null
        
        _formState.update {
            it.copy(
                validation = FormValidation(
                    isValid = isValid,
                    emailError = emailError,
                    passwordError = passwordError
                )
            )
        }
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }
}
