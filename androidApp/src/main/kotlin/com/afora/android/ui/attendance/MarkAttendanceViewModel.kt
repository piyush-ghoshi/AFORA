package com.afora.android.ui.attendance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afora.shared.data.api.AmendAttendanceRequest
import com.afora.shared.data.api.AmendmentEntry
import com.afora.shared.data.api.ConfirmAttendanceRequest
import com.afora.shared.data.api.RosterResponse
import com.afora.shared.data.api.SubmittedAttendanceEntry
import com.afora.shared.data.repository.AttendanceRepository
import com.afora.shared.domain.model.AttendanceStatus
import com.afora.shared.domain.model.SessionStatus
import com.afora.shared.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** The navigation argument key under which the lecture session id arrives. */
const val MARK_ATTENDANCE_SESSION_ID_ARG = "sessionId"

/**
 * ViewModel for the manual attendance marking screen (R12).
 *
 * Reads the session identifier from [savedStateHandle] under
 * [MARK_ATTENDANCE_SESSION_ID_ARG] -- the navigation route added by task 26.1
 * must supply it under that same key.
 */
@HiltViewModel
class MarkAttendanceViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val sessionId: Long = checkNotNull(savedStateHandle[MARK_ATTENDANCE_SESSION_ID_ARG]) {
        "MarkAttendanceViewModel requires a '$MARK_ATTENDANCE_SESSION_ID_ARG' navigation argument"
    }

    private val _uiState = MutableStateFlow<MarkAttendanceUiState>(MarkAttendanceUiState.Loading)
    val uiState: StateFlow<MarkAttendanceUiState> = _uiState.asStateFlow()

    /** The session header from the last successful roster/confirm/amend response. */
    private var header: SessionHeader? = null

    /** Whether the server said the amendment window was open at the last roster load. */
    private var amendmentWindowOpen: Boolean = false

    /**
     * The submissionId of the current confirmation attempt. Generated once when
     * the teacher presses confirm (R12.9) and reused by every retry until the
     * attempt is acknowledged (R12.10). Cleared on success or on an explicit
     * edit after a failure, which starts a new attempt.
     */
    private var attemptSubmissionId: String? = null

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = MarkAttendanceUiState.Loading
            when (val result = attendanceRepository.getSessionRoster(sessionId)) {
                is Result.Success -> applyRoster(result.data)
                is Result.Error -> _uiState.value = MarkAttendanceUiState.Error(
                    message = result.error.message,
                    rows = emptyList()
                )
            }
        }
    }

    private fun applyRoster(roster: RosterResponse) {
        this.amendmentWindowOpen = roster.amendmentWindowOpen
        header = SessionHeader(
            sessionId = roster.sessionId,
            classSectionId = roster.classSectionId,
            subjectId = roster.subjectId,
            subjectCode = roster.subjectCode,
            subjectName = roster.subjectName,
            sessionDate = roster.sessionDate,
            startTime = roster.startTime,
            endTime = roster.endTime,
            sessionStatus = roster.sessionStatus,
            attendanceMethod = roster.attendanceMethod,
            totalStudents = roster.totalStudents
        )
        val rows = roster.entries.map { entry ->
            AttendanceRow(
                studentId = entry.studentId,
                rollNumber = entry.rollNumber,
                fullName = entry.fullName,
                // R12.5, R12.16: one expression covers the stored branch, the
                // suggested branch and the FINALIZED case, where every stored
                // status is non-null.
                selectedStatus = entry.storedStatus ?: entry.suggestedStatus,
                storedStatus = entry.storedStatus,
                suggestedStatus = entry.suggestedStatus,
                leaveReason = entry.leaveReason,
                isCorrected = entry.isCorrected
            )
        }
        _uiState.value = MarkAttendanceUiState.Success(
            header = requireNotNull(header),
            rows = rows,
            presentCount = presentCount(rows),
            absentCount = absentCount(rows),
            onLeaveCount = onLeaveCount(rows),
            amendmentWindowOpen = roster.amendmentWindowOpen
        )
    }

    /** R7.2: PRESENT and LATE both count as attended -- the server's definition, mirrored here. */
    private fun presentCount(rows: List<AttendanceRow>): Int =
        rows.count { it.selectedStatus == AttendanceStatus.PRESENT || it.selectedStatus == AttendanceStatus.LATE }

    private fun absentCount(rows: List<AttendanceRow>): Int =
        rows.count { it.selectedStatus == AttendanceStatus.ABSENT }

    /** R7.4: ON_LEAVE and EXCUSED are both neutral -- the server's definition, mirrored here. */
    private fun onLeaveCount(rows: List<AttendanceRow>): Int =
        rows.count { it.selectedStatus == AttendanceStatus.ON_LEAVE || it.selectedStatus == AttendanceStatus.EXCUSED }

    /**
     * The editable Success state, rebuilt from an Error state when a confirm or
     * amend failed. Without this, pressing Confirm after a failure did nothing
     * because only Success was accepted -- the teacher could not retry without
     * first changing a row. Rebuilding keeps the selections and, because
     * [attemptSubmissionId] is untouched, a plain retry reuses the same
     * submissionId (R12.10).
     */
    private fun successOrRecovered(): MarkAttendanceUiState.Success? {
        return when (val state = _uiState.value) {
            is MarkAttendanceUiState.Success -> state
            is MarkAttendanceUiState.Loading -> null
            is MarkAttendanceUiState.Error -> {
                val recoveredHeader = state.header ?: header
                if (recoveredHeader == null || state.rows.isEmpty()) {
                    null
                } else {
                    MarkAttendanceUiState.Success(
                        header = recoveredHeader,
                        rows = state.rows,
                        presentCount = presentCount(state.rows),
                        absentCount = absentCount(state.rows),
                        onLeaveCount = onLeaveCount(state.rows),
                        amendmentWindowOpen = state.amendmentWindowOpen,
                        amendmentReason = state.amendmentReason
                    )
                }
            }
        }
    }

    /** The rows currently on screen, whichever non-Loading state holds them. */
    private fun currentRows(): List<AttendanceRow> = when (val state = _uiState.value) {
        is MarkAttendanceUiState.Success -> state.rows
        is MarkAttendanceUiState.Error -> state.rows
        is MarkAttendanceUiState.Loading -> emptyList()
    }

    /**
     * A teacher's row edit. Running counts update immediately (R12.7). Clears
     * [attemptSubmissionId] so an edit made after a failed confirmation starts
     * a genuinely new attempt rather than retrying the stale one.
     */
    fun updateStatus(studentId: Long, status: AttendanceStatus) {
        val updatedRows = currentRows().map { row ->
            if (row.studentId == studentId) row.copy(selectedStatus = status) else row
        }
        attemptSubmissionId = null
        val h = header ?: return
        _uiState.value = MarkAttendanceUiState.Success(
            header = h,
            rows = updatedRows,
            presentCount = presentCount(updatedRows),
            absentCount = absentCount(updatedRows),
            onLeaveCount = onLeaveCount(updatedRows),
            amendmentWindowOpen = amendmentWindowOpen
        )
    }

    /** R12.8: one entry per row, every time -- there is never an omitted student from this screen. */
    fun confirm() {
        val state = successOrRecovered() ?: return
        val submissionId = attemptSubmissionId
            ?: UUID.randomUUID().toString().also { attemptSubmissionId = it }

        _uiState.value = state.copy(isSubmitting = true)

        viewModelScope.launch {
            val request = ConfirmAttendanceRequest(
                submissionId = submissionId,
                markUnspecifiedAsAbsent = false,
                entries = state.rows.map { row ->
                    SubmittedAttendanceEntry(studentId = row.studentId, status = row.selectedStatus, notes = null)
                }
            )
            when (val result = attendanceRepository.confirmManualAttendance(sessionId, request)) {
                is Result.Success -> {
                    attemptSubmissionId = null
                    header = header?.copy(sessionStatus = SessionStatus.FINALIZED)
                    _uiState.value = state.copy(
                        header = header ?: state.header,
                        isSubmitting = false,
                        confirmation = result.data
                    )
                }
                is Result.Error -> _uiState.value = MarkAttendanceUiState.Error(
                    message = result.error.message,
                    rows = state.rows,
                    header = header ?: state.header,
                    amendmentWindowOpen = state.amendmentWindowOpen,
                    amendmentReason = state.amendmentReason
                )
            }
        }
    }

    /**
     * Activate a SCHEDULED session so it can be marked. The backend only
     * accepts a confirmation for an ACTIVE session (R2.9, R6.8), so without
     * this step a teacher opening a scheduled class could never confirm it.
     * Activation is idempotent server-side, so a retry after a failure is safe.
     */
    fun startSession() {
        val state = successOrRecovered() ?: return
        if (state.header.sessionStatus != SessionStatus.SCHEDULED) return

        _uiState.value = state.copy(isSubmitting = true)

        viewModelScope.launch {
            when (val result = attendanceRepository.activateSession(sessionId)) {
                is Result.Success -> {
                    val summary = result.data
                    header = state.header.copy(
                        sessionStatus = summary.sessionStatus,
                        attendanceMethod = summary.attendanceMethod,
                        totalStudents = summary.totalStudents
                    )
                    _uiState.value = state.copy(
                        header = header ?: state.header,
                        isSubmitting = false
                    )
                }
                is Result.Error -> _uiState.value = MarkAttendanceUiState.Error(
                    message = result.error.message,
                    rows = state.rows,
                    header = state.header,
                    amendmentWindowOpen = state.amendmentWindowOpen,
                    amendmentReason = state.amendmentReason
                )
            }
        }
    }

    /** R12.18: duplicates the server's blank-reason rule client-side for immediate feedback. */
    fun updateAmendmentReason(reason: String) {
        val state = _uiState.value as? MarkAttendanceUiState.Success ?: return
        _uiState.value = state.copy(amendmentReason = reason)
    }

    /** Submits only the rows whose selected status differs from what is stored. */
    fun submitAmendment() {
        val state = successOrRecovered() ?: return
        if (!state.amendmentWindowOpen || state.amendmentReason.isBlank()) return

        val changedRows = state.rows.filter { it.selectedStatus != it.storedStatus }
        if (changedRows.isEmpty()) return

        _uiState.value = state.copy(isSubmitting = true)

        viewModelScope.launch {
            val request = AmendAttendanceRequest(
                reason = state.amendmentReason,
                entries = changedRows.map { row -> AmendmentEntry(studentId = row.studentId, status = row.selectedStatus) }
            )
            when (val result = attendanceRepository.amendAttendance(sessionId, request)) {
                is Result.Success -> {
                    val updatedRows = state.rows.map { row ->
                        if (changedRows.any { it.studentId == row.studentId }) {
                            row.copy(storedStatus = row.selectedStatus, isCorrected = true)
                        } else row
                    }
                    _uiState.value = state.copy(
                        rows = updatedRows,
                        isSubmitting = false,
                        amendmentReason = ""
                    )
                }
                is Result.Error -> _uiState.value = MarkAttendanceUiState.Error(
                    message = result.error.message,
                    rows = state.rows,
                    header = header ?: state.header,
                    amendmentWindowOpen = state.amendmentWindowOpen,
                    amendmentReason = state.amendmentReason
                )
            }
        }
    }
}