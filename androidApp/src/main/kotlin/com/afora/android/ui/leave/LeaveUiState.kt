package com.afora.android.ui.leave

import com.afora.shared.data.api.LeaveClassOption
import com.afora.shared.data.api.LeaveRequestResponse

/**
 * UI state for the student's leave screen: the submit form above the list of
 * the student's own requests.
 *
 * [submitCount] increments on every successful submit. The form keys its
 * remembered input on it, so a successful submit clears the form while a
 * failed one leaves what the student typed in place.
 */
sealed interface StudentLeaveUiState {
    data object Loading : StudentLeaveUiState

    data class Success(
        val requests: List<LeaveRequestResponse>,
        val classOptions: List<LeaveClassOption> = emptyList(),
        val isSubmitting: Boolean = false,
        val submitError: String? = null,
        val submitCount: Int = 0
    ) : StudentLeaveUiState

    data class Error(val message: String) : StudentLeaveUiState
}

/**
 * UI state for a teacher's leave review queue: every PENDING request against
 * a class section they teach, oldest first.
 */
sealed interface TeacherLeaveReviewUiState {
    data object Loading : TeacherLeaveReviewUiState

    data class Success(
        val requests: List<LeaveRequestResponse>,
        val actioningRequestId: Long? = null,
        val actionError: String? = null
    ) : TeacherLeaveReviewUiState

    data class Error(val message: String) : TeacherLeaveReviewUiState
}