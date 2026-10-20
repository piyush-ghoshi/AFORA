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
 * Loads the signed-in user's profile from the backend, then today's schedule for that
 * teacher. Failures are shown as errors with a retry; there is no offline fallback that
 * substitutes empty figures (see [com.afora.android.ui.dashboard.student.StudentDashboardViewModel]).
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

            val userResult = userRepository.getMyProfile()
            if (userResult is Result.Error) {
                _uiState.value = TeacherDashboardUiState.Error(userResult.error.message)
                return@launch
            }
            val user = (userResult as Result.Success).data

            // The dashboard is keyed by the teachers-table id, which is not the users-table id.
            val teacherId = user.teacherId
            if (teacherId == null) {
                _uiState.value = TeacherDashboardUiState.Error(
                    "Your teacher profile is not set up yet. Please contact your administrator."
                )
                return@launch
            }

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
                    _uiState.value = TeacherDashboardUiState.Error(statsResult.error.message)
                }
            }
        }
    }
}