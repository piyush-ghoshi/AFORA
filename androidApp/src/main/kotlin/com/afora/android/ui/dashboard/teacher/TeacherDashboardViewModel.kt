package com.afora.android.ui.dashboard.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afora.android.ui.dashboard.TeacherDashboardData
import com.afora.android.ui.dashboard.TeacherDashboardUiState
import com.afora.shared.data.repository.AuthRepository
import com.afora.shared.data.repository.DashboardRepository
import com.afora.shared.data.repository.UserRepository
import com.afora.shared.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the teacher dashboard.
 *
 * Phase A5: Loads authenticated user profile and dashboard stats from DashboardRepository.
 */
@HiltViewModel
class TeacherDashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TeacherDashboardUiState>(TeacherDashboardUiState.Loading)
    val uiState: StateFlow<TeacherDashboardUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = TeacherDashboardUiState.Loading

            // First, get the user profile to extract teacherId
            val userResult = userRepository.getMyProfile()
            if (userResult is Result.Error) {
                _uiState.value = TeacherDashboardUiState.Error(
                    userResult.error.message ?: "Failed to load user profile"
                )
                return@launch
            }

            val user = (userResult as Result.Success).data
            
            // Use teacherId from the nested teacher profile; fall back to user.id if not yet
            // populated (e.g. during onboarding before profile is fully created).
            val teacherId = user.teacherId ?: user.id

            // Now fetch dashboard stats (defaults to today's date)
            when (val statsResult = dashboardRepository.getTeacherDashboard(teacherId)) {
                is Result.Success -> {
                    _uiState.value = TeacherDashboardUiState.Success(
                        TeacherDashboardData(
                            user = user,
                            todaySchedule = statsResult.data.todaySchedule,
                            pendingLeaveRequests = statsResult.data.pendingLeaveRequests,
                            pendingQueries = statsResult.data.pendingQueries
                        )
                    )
                }
                is Result.Error -> {
                    _uiState.value = TeacherDashboardUiState.Error(
                        statsResult.error.message ?: "Failed to load dashboard"
                    )
                }
            }
        }
    }
}
