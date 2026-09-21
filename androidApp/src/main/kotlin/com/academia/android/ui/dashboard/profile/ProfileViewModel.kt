package com.academia.android.ui.dashboard.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.academia.android.ui.dashboard.ProfileUiState
import com.academia.shared.data.repository.AuthRepository
import com.academia.shared.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the profile screen.
 *
 * Phase A3: Reads user from Firebase; updates via backend in Phase A4 when
 * HttpUserRepository is wired. For now uses AuthRepository.getCurrentUser().
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            when (val result = authRepository.getCurrentUser()) {
                is Result.Success -> {
                    _uiState.value = ProfileUiState.Ready(
                        user = result.data,
                        firstName = result.data.firstName,
                        lastName = result.data.lastName
                    )
                }
                is Result.Error -> {
                    _uiState.value = ProfileUiState.Error(
                        result.error.message ?: "Failed to load profile"
                    )
                }
            }
        }
    }

    fun onFirstNameChange(value: String) {
        val current = _uiState.value as? ProfileUiState.Ready ?: return
        _uiState.value = current.copy(firstName = value, saveSuccess = false, errorMessage = null)
    }

    fun onLastNameChange(value: String) {
        val current = _uiState.value as? ProfileUiState.Ready ?: return
        _uiState.value = current.copy(lastName = value, saveSuccess = false, errorMessage = null)
    }

    fun onSave() {
        val current = _uiState.value as? ProfileUiState.Ready ?: return

        if (current.firstName.isBlank() || current.lastName.isBlank()) {
            _uiState.update {
                (it as? ProfileUiState.Ready)?.copy(errorMessage = "Name fields cannot be empty")
                    ?: it
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { (it as? ProfileUiState.Ready)?.copy(isSaving = true) ?: it }

            // Phase A4: call HttpUserRepository.updateProfile() here
            // For now we just show success after a short delay
            kotlinx.coroutines.delay(600)

            _uiState.update {
                (it as? ProfileUiState.Ready)?.copy(
                    isSaving = false,
                    saveSuccess = true,
                    errorMessage = null
                ) ?: it
            }
        }
    }
}
