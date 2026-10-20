package com.afora.android.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afora.shared.data.api.StudentAttendanceResponse
import com.afora.shared.data.api.SubjectAttendanceSummary
import com.afora.shared.data.repository.AttendanceRepository
import com.afora.shared.data.repository.UserRepository
import com.afora.shared.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state for the student attendance detail screen. */
sealed class StudentAttendanceUiState {
    object Loading : StudentAttendanceUiState()
    data class Success(
        val response: StudentAttendanceResponse,
        /** The subject currently expanded (null = none). */
        val expandedSubjectId: Long? = null
    ) : StudentAttendanceUiState()
    data class Error(val message: String) : StudentAttendanceUiState()
}

/**
 * ViewModel for the student attendance detail screen (Phase 4).
 *
 * Fetches the signed-in student's attendance data from the backend,
 * grouped by subject and including per-session records.
 */
@HiltViewModel
class StudentAttendanceViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<StudentAttendanceUiState>(StudentAttendanceUiState.Loading)
    val uiState: StateFlow<StudentAttendanceUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = StudentAttendanceUiState.Loading

            val userResult = userRepository.getMyProfile()
            if (userResult is Result.Error) {
                _uiState.value = StudentAttendanceUiState.Error(userResult.error.message)
                return@launch
            }
            val user = (userResult as Result.Success).data
            val studentId = user.studentId
            if (studentId == null) {
                _uiState.value = StudentAttendanceUiState.Error(
                    "Your student profile is not set up yet. Please contact your administrator."
                )
                return@launch
            }

            when (val result = attendanceRepository.getStudentAttendanceDetail(studentId)) {
                is Result.Success -> _uiState.value = StudentAttendanceUiState.Success(result.data)
                is Result.Error  -> _uiState.value = StudentAttendanceUiState.Error(result.error.message)
            }
        }
    }

    /** Toggle the expanded subject accordion. */
    fun toggleSubject(subjectId: Long) {
        val state = _uiState.value as? StudentAttendanceUiState.Success ?: return
        _uiState.value = state.copy(
            expandedSubjectId = if (state.expandedSubjectId == subjectId) null else subjectId
        )
    }
}
