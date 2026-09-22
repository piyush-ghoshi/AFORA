package com.afora.android.ui.dashboard.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afora.android.ui.dashboard.StudentDashboardData
import com.afora.android.ui.dashboard.StudentDashboardUiState
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
 * ViewModel for the student dashboard.
 *
 * Phase A5: Loads authenticated user profile and dashboard stats from DashboardRepository.
 */
@HiltViewModel
class StudentDashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<StudentDashboardUiState>(StudentDashboardUiState.Loading)
    val uiState: StateFlow<StudentDashboardUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = StudentDashboardUiState.Loading

            // First, get the user profile to extract studentId
            val userResult = userRepository.getMyProfile()
            if (userResult is Result.Error) {
                _uiState.value = StudentDashboardUiState.Error(
                    userResult.error.message ?: "Failed to load user profile"
                )
                return@launch
            }

            val user = (userResult as Result.Success).data
            
            // Extract studentId from user (assuming user.id is the student ID for now)
            // In a real implementation, you'd have user.studentId or similar
            val studentId = user.id

            // Now fetch dashboard stats
            when (val statsResult = dashboardRepository.getStudentDashboard(studentId)) {
                is Result.Success -> {
                    _uiState.value = StudentDashboardUiState.Success(
                        StudentDashboardData(
                            user = user,
                            attendancePercentage = statsResult.data.attendancePercentage,
                            presentCount = statsResult.data.presentCount,
                            totalCount = statsResult.data.totalCount,
                            upcomingClasses = statsResult.data.upcomingClasses,
                            pendingLeaveRequests = statsResult.data.pendingLeaveRequests
                        )
                    )
                }
                is Result.Error -> {
                    _uiState.value = StudentDashboardUiState.Error(
                        statsResult.error.message ?: "Failed to load dashboard"
                    )
                }
            }
        }
    }
}
