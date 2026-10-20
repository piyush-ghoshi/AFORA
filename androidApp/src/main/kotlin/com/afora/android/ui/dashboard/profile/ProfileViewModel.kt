package com.afora.android.ui.dashboard.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afora.android.ui.dashboard.ProfileUiState
import com.afora.shared.data.repository.UserRepository
import com.afora.shared.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the profile screen, shared by students and teachers.
 *
 * Loads the profile from the backend (including roll number or employee id and
 * department) and saves name changes with PUT /api/users/me. Saving reports success
 * only when the backend confirmed it.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            when (val result = userRepository.getMyProfile()) {
                is Result.Success -> _uiState.value = ProfileUiState.Ready(
                    user = result.data,
                    firstName = result.data.firstName,
                    lastName = result.data.lastName
                )
                is Result.Error -> _uiState.value = ProfileUiState.Error(result.error.message)
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
        if (current.isSaving) return

        if (current.firstName.isBlank() || current.lastName.isBlank()) {
            _uiState.update {
                (it as? ProfileUiState.Ready)?.copy(errorMessage = "Name fields cannot be empty") ?: it
            }
            return
        }

        _uiState.value = current.copy(isSaving = true, saveSuccess = false, errorMessage = null)

        viewModelScope.launch {
            when (val result = userRepository.updateProfile(
                firstName = current.firstName.trim(),
                lastName = current.lastName.trim(),
                profilePictureUrl = null
            )) {
                is Result.Success -> _uiState.update {
                    (it as? ProfileUiState.Ready)?.copy(
                        user = result.data,
                        firstName = result.data.firstName,
                        lastName = result.data.lastName,
                        isSaving = false,
                        saveSuccess = true,
                        errorMessage = null
                    ) ?: it
                }
                is Result.Error -> _uiState.update {
                    (it as? ProfileUiState.Ready)?.copy(
                        isSaving = false,
                        saveSuccess = false,
                        errorMessage = result.error.message
                    ) ?: it
                }
            }
        }
    }
}