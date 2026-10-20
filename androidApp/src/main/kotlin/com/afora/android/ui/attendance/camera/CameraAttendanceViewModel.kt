package com.afora.android.ui.attendance.camera

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afora.shared.data.api.ConfirmAttendanceRequest
import com.afora.shared.data.api.RosterEntry
import com.afora.shared.data.api.SubmittedAttendanceEntry
import com.afora.shared.data.repository.AttendanceRepository
import com.afora.shared.data.repository.FaceProfileRepository
import com.afora.shared.domain.model.AttendanceStatus
import com.afora.shared.domain.model.SessionStatus
import com.afora.shared.util.AppError
import com.afora.shared.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

const val CAMERA_ATTENDANCE_SESSION_ID_ARG = "sessionId"

@HiltViewModel
class CameraAttendanceViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val faceProfileRepository: FaceProfileRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val sessionId: Long = checkNotNull(savedStateHandle[CAMERA_ATTENDANCE_SESSION_ID_ARG]) {
        "sessionId argument is required for CameraAttendanceViewModel"
    }

    private val _uiState = MutableStateFlow<CameraAttendanceUiState>(CameraAttendanceUiState.Loading)
    val uiState: StateFlow<CameraAttendanceUiState> = _uiState.asStateFlow()

    init {
        loadSession()
    }

    fun loadSession() {
        viewModelScope.launch {
            _uiState.value = CameraAttendanceUiState.Loading
            when (val rosterResult = attendanceRepository.getSessionRoster(sessionId)) {
                is Result.Success -> {
                    val roster = rosterResult.data
                    // Auto-activate session if it is still SCHEDULED
                    if (roster.sessionStatus == SessionStatus.SCHEDULED) {
                        when (val activateResult = attendanceRepository.activateSession(sessionId)) {
                            is Result.Success -> {
                                val activeRoster = roster.copy(sessionStatus = SessionStatus.ACTIVE)
                                initializeReadyState(activeRoster)
                            }
                            is Result.Error -> {
                                _uiState.value = CameraAttendanceUiState.Error(
                                    activateResult.error.message ?: "Failed to activate lecture session"
                                )
                            }
                        }
                    } else {
                        initializeReadyState(roster)
                    }
                }
                is Result.Error -> {
                    _uiState.value = CameraAttendanceUiState.Error(
                        rosterResult.error.message ?: "Failed to load session roster"
                    )
                }
            }
        }
    }

    private fun initializeReadyState(roster: com.afora.shared.data.api.RosterResponse) {
        val initialCandidates = mutableMapOf<Long, DetectedFaceCandidate>()

        // Pre-populate with existing confirmed / suggested leave records if any
        roster.entries.forEach { entry ->
            if (entry.suggestedStatus == AttendanceStatus.ON_LEAVE) {
                initialCandidates[entry.studentId] = DetectedFaceCandidate(
                    studentId = entry.studentId,
                    rollNumber = entry.rollNumber,
                    fullName = entry.fullName,
                    confidence = 1.0,
                    category = ConfidenceCategory.HIGH,
                    attendanceStatus = AttendanceStatus.ON_LEAVE,
                    notes = entry.leaveReason ?: "Approved leave"
                )
            }
        }

        _uiState.value = CameraAttendanceUiState.Ready(
            sessionId = sessionId,
            roster = roster,
            enrolledStudents = roster.entries,
            candidates = initialCandidates
        )

        // R5.9/R6.2: fetch the Enrolled_Embedding_Set for this Camera_Session's
        // Class_Section so the On_Device_Matcher has something to compare against,
        // and so an empty set can trigger the R10.1 fallback below.
        fetchEnrolledEmbeddingSet(roster.classSectionId)
    }

    /**
     * R10.1: an empty Enrolled_Embedding_Set navigates to Mark_Attendance_Screen
     * without starting a scanning attempt. A fetch failure is not one of
     * Requirement 10's three defined triggers (empty set / zero-match scan /
     * matcher error), so it is surfaced as a non-fatal error message instead of
     * forcing an unrequested fallback.
     */
    private fun fetchEnrolledEmbeddingSet(classSectionId: Long) {
        viewModelScope.launch {
            when (val result = faceProfileRepository.getEnrolledEmbeddingSet(classSectionId)) {
                is Result.Success -> {
                    if (result.data.entries.isEmpty()) {
                        _uiState.update { current ->
                            if (current is CameraAttendanceUiState.Ready) {
                                current.copy(isScanning = false, shouldFallbackToManualMarking = true)
                            } else {
                                current
                            }
                        }
                    }
                }
                is Result.Error -> {
                    _uiState.update { current ->
                        if (current is CameraAttendanceUiState.Ready) {
                            current.copy(
                                errorMessage = result.error.message ?: "Failed to load enrolled face profiles"
                            )
                        } else {
                            current
                        }
                    }
                }
            }
        }
    }

    fun onFaceDetections(
        overlays: List<FaceOverlayItem>,
        detectedCandidates: List<DetectedFaceCandidate>
    ) {
        _uiState.update { current ->
            if (current is CameraAttendanceUiState.Ready && current.isScanning) {
                val updatedCandidates = current.candidates.toMutableMap()
                val updatedReviewQueue = current.reviewQueue.toMutableMap()

                for (c in detectedCandidates) {
                    when (c.category) {
                        ConfidenceCategory.HIGH -> {
                            // R7.5: HIGH is pre-selected PRESENT directly into candidates,
                            // overridable by the Teacher; never overwrites a pre-populated
                            // approved-leave entry.
                            val existing = updatedCandidates[c.studentId]
                            if (existing == null || existing.attendanceStatus != AttendanceStatus.ON_LEAVE) {
                                updatedCandidates[c.studentId] = c
                            }
                            // A HIGH match supersedes any stale MEDIUM queue entry for the same student.
                            updatedReviewQueue.remove(c.studentId)
                        }
                        ConfidenceCategory.MEDIUM -> {
                            // R7.6: MEDIUM is added to the Review_Queue only, never pre-selected.
                            // Skip if the Teacher already explicitly confirmed this student, so a
                            // later re-detection cannot silently reopen a resolved decision.
                            val alreadyResolved = updatedCandidates[c.studentId]?.teacherConfirmed == true
                            if (!alreadyResolved) {
                                val candidateMatch = FaceMatchCandidate(
                                    studentId = c.studentId,
                                    rollNumber = c.rollNumber,
                                    fullName = c.fullName,
                                    matchConfidence = c.confidence,
                                    confidenceBand = ConfidenceBand.MEDIUM,
                                    trackingId = c.trackingId
                                )
                                val existingQueueEntry = updatedReviewQueue[c.studentId]
                                // R8.7: among duplicate MEDIUM candidates for one student, keep the
                                // one carrying the highest matchConfidence and discard the others.
                                if (existingQueueEntry == null ||
                                    candidateMatch.matchConfidence > existingQueueEntry.matchConfidence
                                ) {
                                    updatedReviewQueue[c.studentId] = candidateMatch
                                }
                            }
                        }
                        else -> Unit // LOW/UNKNOWN are never produced for detectedCandidates by the analyzer.
                    }
                }

                current.copy(
                    liveOverlayFaces = overlays,
                    candidates = updatedCandidates,
                    reviewQueue = updatedReviewQueue
                )
            } else {
                current
            }
        }
    }

    fun toggleScanning() {
        _uiState.update { current ->
            if (current is CameraAttendanceUiState.Ready) {
                val isPausing = current.isScanning
                // R10.2: a scanning attempt that produced zero Face_Match_Candidate values
                // for the entire roster offers the Teacher the fallback control. There is no
                // discrete "scan attempt" boundary in this flow today, so pausing scanning
                // (the Teacher-visible end of an attempt) after faces were actually seen but
                // nothing was ever matched is treated as that boundary.
                val producedNoMatches = current.candidates.values.none {
                    it.attendanceStatus != AttendanceStatus.ON_LEAVE
                } && current.reviewQueue.isEmpty()
                val offerFallback = isPausing && producedNoMatches && current.liveOverlayFaces.isNotEmpty()

                current.copy(
                    isScanning = !current.isScanning,
                    liveOverlayFaces = if (current.isScanning) emptyList() else current.liveOverlayFaces,
                    shouldFallbackToManualMarking = current.shouldFallbackToManualMarking || offerFallback
                )
            } else {
                current
            }
        }
    }

    /**
     * R10.3 hook: an unrecoverable On_Device_Matcher error navigates to
     * Mark_Attendance_Screen. Not yet invoked from a live call site -- see this
     * task's report for why `FaceDetectionAnalyzer`'s optional error callback
     * is currently unwired (it would require editing `CameraAttendanceScreen.kt`,
     * which is outside this task's single-owner file set).
     */
    fun reportMatcherError() {
        _uiState.update { current ->
            if (current is CameraAttendanceUiState.Ready) {
                current.copy(isScanning = false, shouldFallbackToManualMarking = true)
            } else {
                current
            }
        }
    }

    /**
     * Clears the one-off fallback-navigation signal after the screen has acted on
     * it, so it is not replayed on recomposition/rotation. R10.5: this never
     * clears `reviewQueue` or `candidates`.
     */
    fun consumeFallbackNavigation() {
        _uiState.update { current ->
            if (current is CameraAttendanceUiState.Ready) {
                current.copy(shouldFallbackToManualMarking = false)
            } else {
                current
            }
        }
    }

    /** R8.2: moves a Review_Queue entry into candidates, pre-selected PRESENT, Teacher-confirmed. */
    fun confirmReviewEntry(studentId: Long) {
        _uiState.update { current ->
            if (current is CameraAttendanceUiState.Ready) {
                val match = current.reviewQueue[studentId] ?: return@update current
                val updatedCandidates = current.candidates.toMutableMap()
                updatedCandidates[studentId] = DetectedFaceCandidate(
                    studentId = match.studentId,
                    rollNumber = match.rollNumber,
                    fullName = match.fullName,
                    confidence = match.matchConfidence,
                    category = ConfidenceCategory.MEDIUM,
                    trackingId = match.trackingId,
                    attendanceStatus = AttendanceStatus.PRESENT,
                    teacherConfirmed = true
                )
                current.copy(
                    candidates = updatedCandidates,
                    reviewQueue = current.reviewQueue - studentId
                )
            } else {
                current
            }
        }
    }

    /** R8.3: removes a Review_Queue entry with no automatic status set. */
    fun rejectReviewEntry(studentId: Long) {
        _uiState.update { current ->
            if (current is CameraAttendanceUiState.Ready) {
                current.copy(reviewQueue = current.reviewQueue - studentId)
            } else {
                current
            }
        }
    }

    fun toggleTorch() {
        _uiState.update { current ->
            if (current is CameraAttendanceUiState.Ready) {
                current.copy(isTorchOn = !current.isTorchOn)
            } else {
                current
            }
        }
    }

    fun setReviewMode(enabled: Boolean) {
        _uiState.update { current ->
            if (current is CameraAttendanceUiState.Ready) {
                current.copy(
                    isReviewing = enabled,
                    isScanning = !enabled // pause scanning while reviewing
                )
            } else {
                current
            }
        }
    }

    fun updateCandidateStatus(studentId: Long, newStatus: AttendanceStatus) {
        _uiState.update { current ->
            if (current is CameraAttendanceUiState.Ready) {
                val student = current.enrolledStudents.find { it.studentId == studentId }
                if (student != null) {
                    val updatedCandidates = current.candidates.toMutableMap()
                    val existing = updatedCandidates[studentId]
                    if (existing != null) {
                        updatedCandidates[studentId] = existing.copy(attendanceStatus = newStatus)
                    } else {
                        updatedCandidates[studentId] = DetectedFaceCandidate(
                            studentId = studentId,
                            rollNumber = student.rollNumber,
                            fullName = student.fullName,
                            confidence = 1.0,
                            category = ConfidenceCategory.HIGH,
                            attendanceStatus = newStatus
                        )
                    }
                    current.copy(candidates = updatedCandidates)
                } else {
                    current
                }
            } else {
                current
            }
        }
    }

    fun confirmAttendance() {
        val current = _uiState.value as? CameraAttendanceUiState.Ready ?: return
        if (current.isSubmitting) return

        viewModelScope.launch {
            _uiState.update { (it as CameraAttendanceUiState.Ready).copy(isSubmitting = true, errorMessage = null) }

            // Build submitted entries for all enrolled students
            val entries = current.enrolledStudents.map { student ->
                val candidate = current.candidates[student.studentId]
                val status = candidate?.attendanceStatus ?: AttendanceStatus.ABSENT
                val confidence = candidate?.confidence
                val notes = candidate?.notes ?: if (candidate != null) "Detected via Camera scanner" else null

                SubmittedAttendanceEntry(
                    studentId = student.studentId,
                    status = status,
                    notes = notes,
                    confidenceScore = confidence,
                    // R9.2/R9.3: connects the client-side UX gate to the real
                    // server-side FaceMatchReviewGate.
                    teacherConfirmed = candidate?.teacherConfirmed ?: false
                )
            }

            val request = ConfirmAttendanceRequest(
                submissionId = UUID.randomUUID().toString(),
                markUnspecifiedAsAbsent = true,
                entries = entries,
                method = "CAMERA"
            )

            when (val result = attendanceRepository.confirmManualAttendance(sessionId, request)) {
                is Result.Success -> {
                    _uiState.update {
                        (it as CameraAttendanceUiState.Ready).copy(
                            isSubmitting = false,
                            confirmation = result.data,
                            isReviewing = true,
                            isScanning = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        (it as CameraAttendanceUiState.Ready).copy(
                            isSubmitting = false,
                            errorMessage = result.error.message ?: "Failed to confirm camera attendance"
                        )
                    }
                }
            }
        }
    }
}
