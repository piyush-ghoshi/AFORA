package com.afora.android.ui.dashboard

import com.afora.shared.domain.model.LectureSession
import com.afora.shared.domain.model.TimetableSlot
import com.afora.shared.domain.model.User

// ── Teacher ───────────────────────────────────────────────────────────────────

/**
 * UI state for the teacher dashboard.
 */
sealed class TeacherDashboardUiState {
    object Loading : TeacherDashboardUiState()
    data class Success(val data: TeacherDashboardData) : TeacherDashboardUiState()
    data class Error(val message: String) : TeacherDashboardUiState()
}

data class TeacherDashboardData(
    val user: User,
    val todaySchedule: List<LectureSession> = emptyList(),
    val pendingLeaveRequests: Int = 0,
    val pendingQueries: Int = 0
)

// ── Student ───────────────────────────────────────────────────────────────────

/**
 * UI state for the student dashboard.
 */
sealed class StudentDashboardUiState {
    object Loading : StudentDashboardUiState()
    data class Success(val data: StudentDashboardData) : StudentDashboardUiState()
    data class Error(val message: String) : StudentDashboardUiState()
}

data class StudentDashboardData(
    val user: User,
    val attendancePercentage: Double = 0.0,
    val presentCount: Int = 0,
    val totalCount: Int = 0,
    val upcomingClasses: List<TimetableSlot> = emptyList(),
    val pendingLeaveRequests: Int = 0
)

// ── Profile ───────────────────────────────────────────────────────────────────

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Ready(
        val user: User,
        val firstName: String,
        val lastName: String,
        val isSaving: Boolean = false,
        val saveSuccess: Boolean = false,
        val errorMessage: String? = null
    ) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}
