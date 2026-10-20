package com.afora.android.ui.leave

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afora.shared.data.repository.LeaveRepository
import com.afora.shared.data.repository.UserRepository
import com.afora.shared.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for a teacher's leave review queue (Leave Requests feature).
 *
 * Resolves `teacherId` from the authenticated user's own profile first, the
 * same two-step load as [com.afora.android.ui.dashboard.teacher.TeacherDashboardViewModel]
 * (`getMyProfile()` then the id-scoped call), rather than taking it as a
 * navigation argument -- a teacher only ever reviews their own queue.
 */
@HiltViewModel
class TeacherLeaveReviewViewModel @Inject constructor(
    private val leaveRepository: LeaveRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TeacherLeaveReviewUiState>(TeacherLeaveReviewUiState.Loading)
    val uiState: StateFlow<TeacherLeaveReviewUiState> = _uiState.asStateFlow()

    private var teacherId: Long? = null

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = TeacherLeaveReviewUiState.Loading

            val userResult = userRepository.getMyProfile()
            if (userResult is Result.Error) {
                _uiState.value = TeacherLeaveReviewUiState.Error(userResult.error.message)
                return@launch
            }
            val user = (userResult as Result.Success).data
            val resolvedTeacherId = user.teacherId ?: user.id
            teacherId = resolvedTeacherId

            when (val result = leaveRepository.getPendingLeaveRequests(resolvedTeacherId)) {
                is Result.Success -> _uiState.value = TeacherLeaveReviewUiState.Success(requests = result.data)
                is Result.Error -> _uiState.value = TeacherLeaveReviewUiState.Error(result.error.message)
            }
        }
    }

    fun approve(leaveRequestId: Long, notes: String? = null) {
        val state = _uiState.value as? TeacherLeaveReviewUiState.Success ?: return
        _uiState.value = state.copy(actioningRequestId = leaveRequestId, actionError = null)

        viewModelScope.launch {
            when (val result = leaveRepository.approveLeaveRequest(leaveRequestId, notes)) {
                is Result.Success -> removeFromQueue(leaveRequestId)
                is Result.Error -> {
                    val current = _uiState.value as? TeacherLeaveReviewUiState.Success ?: state
                    _uiState.value = current.copy(actioningRequestId = null, actionError = result.error.message)
                }
            }
        }
    }

    fun reject(leaveRequestId: Long, notes: String) {
        val state = _uiState.value as? TeacherLeaveReviewUiState.Success ?: return
        _uiState.value = state.copy(actioningRequestId = leaveRequestId, actionError = null)

        viewModelScope.launch {
            when (val result = leaveRepository.rejectLeaveRequest(leaveRequestId, notes)) {
                is Result.Success -> removeFromQueue(leaveRequestId)
                is Result.Error -> {
                    val current = _uiState.value as? TeacherLeaveReviewUiState.Success ?: state
                    _uiState.value = current.copy(actioningRequestId = null, actionError = result.error.message)
                }
            }
        }
    }

    /** A reviewed request leaves the PENDING queue -- it is removed rather than updated in place. */
    private fun removeFromQueue(leaveRequestId: Long) {
        val current = _uiState.value as? TeacherLeaveReviewUiState.Success ?: return
        _uiState.value = current.copy(
            requests = current.requests.filterNot { it.id == leaveRequestId },
            actioningRequestId = null,
            actionError = null
        )
    }
}