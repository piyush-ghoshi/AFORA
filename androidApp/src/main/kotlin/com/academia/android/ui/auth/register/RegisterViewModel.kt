package com.academia.android.ui.auth.register

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.academia.android.ui.auth.*
import com.academia.shared.data.repository.AuthRepository
import com.academia.shared.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for registration screen.
 * 
 * Handles:
 * - Form state and validation
 * - Firebase user registration
 * - Password strength validation
 * - UI state management
 * - Navigation events
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // Form state
    private val _formState = MutableStateFlow(RegisterFormState())
    val formState: StateFlow<RegisterFormState> = _formState.asStateFlow()

    // UI state
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // One-time events
    private val _events = Channel<AuthUiEvent>(Channel.BUFFERED)
    val events: Flow<AuthUiEvent> = _events.receiveAsFlow()

    /**
     * Update role selection.
     */
    fun onRoleChange(role: UserRole) {
        _formState.update { it.copy(role = role) }
    }

    /**
     * Update first name and validate.
     */
    fun onFirstNameChange(firstName: String) {
        _formState.update { it.copy(firstName = firstName) }
        validateForm()
    }

    /**
     * Update last name and validate.
     */
    fun onLastNameChange(lastName: String) {
        _formState.update { it.copy(lastName = lastName) }
        validateForm()
    }

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
     * Update confirm password and validate.
     */
    fun onConfirmPasswordChange(confirmPassword: String) {
        _formState.update { it.copy(confirmPassword = confirmPassword) }
        validateForm()
    }

    /**
     * Perform registration with Firebase.
     */
    fun onRegisterClick() {
        val form = _formState.value
        
        if (!form.validation.isValid) {
            return
        }

        if (form.password != form.confirmPassword) {
            _uiState.value = AuthUiState.Error("Passwords do not match")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            when (val result = authRepository.register(
                email = form.email,
                password = form.password,
                firstName = form.firstName,
                lastName = form.lastName,
                role = form.role.value
            )) {
                is Result.Success -> {
                    _uiState.value = AuthUiState.Success(result.data.user)
                    _events.send(AuthUiEvent.ShowSuccess("Account created successfully!"))
                    _events.send(AuthUiEvent.NavigateToHome)
                }
                is Result.Error -> {
                    _uiState.value = AuthUiState.Error(
                        result.error.message ?: "Registration failed. Please try again."
                    )
                }
            }
        }
    }

    /**
     * Validate all form fields.
     */
    private fun validateForm() {
        val form = _formState.value
        
        val emailError = when {
            form.email.isBlank() -> "Email is required"
            !Patterns.EMAIL_ADDRESS.matcher(form.email).matches() -> "Invalid email format"
            else -> null
        }
        
        val passwordError = when {
            form.password.isBlank() -> "Password is required"
            form.password.length < 8 -> "Password must be at least 8 characters"
            !form.password.any { it.isUpperCase() } -> "Password must contain an uppercase letter"
            !form.password.any { it.isLowerCase() } -> "Password must contain a lowercase letter"
            !form.password.any { it.isDigit() } -> "Password must contain a number"
            else -> null
        }
        
        val nameError = when {
            form.firstName.isBlank() || form.lastName.isBlank() -> "Name is required"
            form.firstName.length < 2 || form.lastName.length < 2 -> "Name must be at least 2 characters"
            else -> null
        }
        
        val isValid = emailError == null && 
                     passwordError == null && 
                     nameError == null &&
                     form.password == form.confirmPassword
        
        _formState.update {
            it.copy(
                validation = FormValidation(
                    isValid = isValid,
                    emailError = emailError,
                    passwordError = passwordError,
                    nameError = nameError
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
