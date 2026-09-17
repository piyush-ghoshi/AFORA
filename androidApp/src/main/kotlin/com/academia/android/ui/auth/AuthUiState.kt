package com.academia.android.ui.auth

import com.academia.shared.domain.model.User

/**
 * UI state for authentication screens.
 */
sealed class AuthUiState {
    /**
     * Initial/idle state.
     */
    object Idle : AuthUiState()
    
    /**
     * Loading state (API call in progress).
     */
    object Loading : AuthUiState()
    
    /**
     * Success state with authenticated user.
     */
    data class Success(val user: User) : AuthUiState()
    
    /**
     * Error state with error message.
     */
    data class Error(val message: String) : AuthUiState()
}

/**
 * Form validation state.
 */
data class FormValidation(
    val isValid: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val nameError: String? = null
)

/**
 * Login form state.
 */
data class LoginFormState(
    val email: String = "",
    val password: String = "",
    val rememberMe: Boolean = false,
    val validation: FormValidation = FormValidation()
)

/**
 * Registration form state.
 */
data class RegisterFormState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val role: UserRole = UserRole.STUDENT,
    val validation: FormValidation = FormValidation()
)

/**
 * User roles for registration.
 */
enum class UserRole(val displayName: String, val value: String) {
    STUDENT("Student", "STUDENT"),
    TEACHER("Teacher", "TEACHER")
}

/**
 * UI events for authentication screens.
 */
sealed class AuthUiEvent {
    data class ShowError(val message: String) : AuthUiEvent()
    data class ShowSuccess(val message: String) : AuthUiEvent()
    object NavigateToHome : AuthUiEvent()
    object NavigateToLogin : AuthUiEvent()
    object NavigateToRegister : AuthUiEvent()
}
