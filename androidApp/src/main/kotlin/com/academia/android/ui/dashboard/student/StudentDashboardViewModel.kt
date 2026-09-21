package com.academia.android.ui.dashboard.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.academia.android.ui.dashboard.StudentDashboardData
import com.academia.android.ui.dashboard.StudentDashboardUiState
import com.academia.shared.data.repository.AuthRepository
import com.academia.shared.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the student dashboard.
 *
 * Phase A3: Loads authenticated user profile.
 * Phase A4: Will add attendance stats from AttendanceRepository.
 */
@HiltViewModel
class StudentDashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<StudentDashboardUiState>(StudentDashboardUiState.Loading)
    val uiState: StateFlow<StudentDashboardUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = StudentDashboardUiState.Loading

            when (val result = authRepository.getCurrentUser()) {
                is Result.Success -> {
                    _uiState.value = StudentDashboardUiState.Success(
                        StudentDashboardData(
                            user = result.data,
                            // Phase A4: load real stats from AttendanceRepository
                            attendancePercentage = 0.0,
                            presentCount = 0,
                            totalCount = 0,
                            upcomingClasses = emptyList(),
                            pendingLeaveRequests = 0
                        )
                    )
                }
                is Result.Error -> {
                    _uiState.value = StudentDashboardUiState.Error(
                        result.error.message ?: "Failed to load dashboard"
                    )
                }
            }
        }
    }
}
