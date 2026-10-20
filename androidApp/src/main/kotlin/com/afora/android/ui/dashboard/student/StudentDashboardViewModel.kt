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
 * Loads the signed-in user's profile from the backend, then the dashboard figures for
 * that student. Failures are shown as errors with a retry: there is deliberately no
 * offline fallback that substitutes empty figures, because that hides a broken
 * connection or a missing profile behind a screen that looks like a new student.
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

            val userResult = userRepository.getMyProfile()
            if (userResult is Result.Error) {
                _uiState.value = StudentDashboardUiState.Error(userResult.error.message)
                return@launch
            }
            val user = (userResult as Result.Success).data

            // The dashboard is keyed by the students-table id, which is not the users-table id.
            val studentId = user.studentId
            if (studentId == null) {
                _uiState.value = StudentDashboardUiState.Error(
                    "Your student profile is not set up yet. Please contact your administrator."
                )
                return@launch
            }

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
                    _uiState.value = StudentDashboardUiState.Error(statsResult.error.message)
                }
            }
        }
    }
}