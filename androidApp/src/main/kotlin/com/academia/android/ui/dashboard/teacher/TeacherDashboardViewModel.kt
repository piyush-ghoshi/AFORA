package com.academia.android.ui.dashboard.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.academia.android.ui.dashboard.TeacherDashboardData
import com.academia.android.ui.dashboard.TeacherDashboardUiState
import com.academia.shared.data.repository.AuthRepository
import com.academia.shared.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the teacher dashboard.
 *
 * Phase A3: Loads authenticated user profile.
 * Phase A4: Will add today's schedule from TimetableRepository.
 */
@HiltViewModel
class TeacherDashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TeacherDashboardUiState>(TeacherDashboardUiState.Loading)
    val uiState: StateFlow<TeacherDashboardUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = TeacherDashboardUiState.Loading

            when (val result = authRepository.getCurrentUser()) {
                is Result.Success -> {
                    _uiState.value = TeacherDashboardUiState.Success(
                        TeacherDashboardData(
                            user = result.data,
                            // Phase A4: load todaySchedule from TimetableRepository
                            todaySchedule = emptyList(),
                            pendingLeaveRequests = 0,
                            pendingQueries = 0
                        )
                    )
                }
                is Result.Error -> {
                    _uiState.value = TeacherDashboardUiState.Error(
                        result.error.message ?: "Failed to load dashboard"
                    )
                }
            }
        }
    }
}
