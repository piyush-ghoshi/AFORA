package com.afora.android.ui.attendance

import com.afora.shared.data.api.AttendanceConfirmationResponse
import com.afora.shared.domain.model.AttendanceMethod
import com.afora.shared.domain.model.AttendanceStatus
import com.afora.shared.domain.model.SessionStatus

/**
 * UI state for the manual attendance marking screen.
 *
 * `Error` carries `rows` so a failed confirmation retains every status the
 * teacher selected (R12.14) -- guaranteed by the type rather than by
 * remembering to copy state across the failure path.
 */
sealed interface MarkAttendanceUiState {
    data object Loading : MarkAttendanceUiState

    data class Success(
        val header: SessionHeader,
        val rows: List<AttendanceRow>,
        val presentCount: Int,
        val absentCount: Int,
        val onLeaveCount: Int,
        val isSubmitting: Boolean = false,
        val confirmation: AttendanceConfirmationResponse? = null,
        val amendmentWindowOpen: Boolean = false,
        val amendmentReason: String = ""
    ) : MarkAttendanceUiState

    data class Error(
        val message: String,
        val rows: List<AttendanceRow>,
        val header: SessionHeader? = null,
        val amendmentWindowOpen: Boolean = false,
        val amendmentReason: String = ""
    ) : MarkAttendanceUiState
}

/**
 * The session-level fields of a [com.afora.shared.data.api.RosterResponse]
 * that are not per-student, projected separately from [AttendanceRow] so the
 * screen can render a header without repeating these values on every row.
 */
data class SessionHeader(
    val sessionId: Long,
    val classSectionId: Long,
    val subjectId: Long,
    val subjectCode: String,
    val subjectName: String,
    val sessionDate: String,
    val startTime: String,
    val endTime: String,
    val sessionStatus: SessionStatus,
    val attendanceMethod: AttendanceMethod?,
    val totalStudents: Int?
)

/**
 * One student's row on the marking screen.
 *
 * `selectedStatus` is what the teacher has chosen (or the default), distinct
 * from `storedStatus` (what is currently persisted) and `suggestedStatus`
 * (the server's advisory ON_LEAVE/PRESENT hint).
 */
data class AttendanceRow(
    val studentId: Long,
    val rollNumber: String,
    val fullName: String,
    val selectedStatus: AttendanceStatus,
    val storedStatus: AttendanceStatus?,
    val suggestedStatus: AttendanceStatus,
    val leaveReason: String?,
    val isCorrected: Boolean
)