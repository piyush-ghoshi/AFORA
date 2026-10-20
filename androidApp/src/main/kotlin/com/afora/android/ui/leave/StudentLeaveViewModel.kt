package com.afora.android.ui.leave

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afora.shared.data.repository.LeaveRepository
import com.afora.shared.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the student's leave screen.
 *
 * The backend resolves the caller's own student id from the Firebase token
 * (`LeaveAccessGuard.requireOwnStudentId`), so no studentId is needed here.
 */
@HiltViewModel
class StudentLeaveViewModel @Inject constructor(
    private val leaveRepository: LeaveRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<StudentLeaveUiState>(StudentLeaveUiState.Loading)
    val uiState: StateFlow<StudentLeaveUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = StudentLeaveUiState.Loading
            when (val result = leaveRepository.getMyLeaveRequests()) {
                is Result.Success -> {
                    // The class list only feeds the optional scope picker; if it fails
                    // the student can still submit a request that spans all classes.
                    val classes = (leaveRepository.getMyClassOptions() as? Result.Success)?.data ?: emptyList()
                    _uiState.value = StudentLeaveUiState.Success(
                        requests = result.data,
                        classOptions = classes
                    )
                }
                is Result.Error -> _uiState.value = StudentLeaveUiState.Error(result.error.message)
            }
        }
    }

    /** Submit a new leave request. [classSectionId] null means it spans all of the student's classes. */
    fun submit(fromDate: String, toDate: String, reason: String, classSectionId: Long? = null) {
        val state = _uiState.value as? StudentLeaveUiState.Success ?: return
        if (state.isSubmitting) return
        _uiState.value = state.copy(isSubmitting = true, submitError = null)

        viewModelScope.launch {
            when (val result = leaveRepository.submitLeaveRequest(fromDate, toDate, reason, classSectionId)) {
                is Result.Success -> {
                    val current = _uiState.value as? StudentLeaveUiState.Success ?: state
                    _uiState.value = current.copy(
                        requests = listOf(result.data) + current.requests,
                        isSubmitting = false,
                        submitError = null,
                        submitCount = current.submitCount + 1
                    )
                }
                is Result.Error -> {
                    val current = _uiState.value as? StudentLeaveUiState.Success ?: state
                    _uiState.value = current.copy(isSubmitting = false, submitError = result.error.message)
                }
            }
        }
    }

    /** Cancel one of the student's own still-PENDING requests. */
    fun cancel(leaveRequestId: Long) {
        val state = _uiState.value as? StudentLeaveUiState.Success ?: return

        viewModelScope.launch {
            when (val result = leaveRepository.cancelLeaveRequest(leaveRequestId)) {
                is Result.Success -> {
                    val current = _uiState.value as? StudentLeaveUiState.Success ?: state
                    _uiState.value = current.copy(
                        requests = current.requests.map { if (it.id == leaveRequestId) result.data else it },
                        submitError = null
                    )
                }
                is Result.Error -> {
                    val current = _uiState.value as? StudentLeaveUiState.Success ?: state
                    _uiState.value = current.copy(submitError = result.error.message)
                }
            }
        }
    }
}