package com.afora.android.ui.auth

import com.afora.shared.domain.model.User

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
    val nameError: String? = null,
    val idNumberError: String? = null
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
    /** Roll number (students) or employee ID (teachers). */
    val idNumber: String = "",
    val validation: FormValidation = FormValidation()
) {
    /** True once the roll number / employee id is long enough to send to the backend. */
    val isIdValid: Boolean get() = idNumber.trim().length >= 3
}

/**
 * User roles for registration.
 */
enum class UserRole(val displayName: String, val value: String, val idLabel: String) {
    STUDENT("Student", "STUDENT", "Roll Number"),
    TEACHER("Faculty / Teacher", "TEACHER", "Employee ID")
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
