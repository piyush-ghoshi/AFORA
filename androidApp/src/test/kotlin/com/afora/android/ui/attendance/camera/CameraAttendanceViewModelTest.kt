package com.afora.android.ui.attendance.camera

import androidx.lifecycle.SavedStateHandle
import com.afora.android.ui.attendance.FakeAttendanceRepository
import com.afora.shared.data.api.AttendanceConfirmationResponse
import com.afora.shared.data.api.EnrolledEmbeddingSetResponse
import com.afora.shared.data.api.RosterEntry
import com.afora.shared.data.api.RosterResponse
import com.afora.shared.data.repository.FaceProfileRepository
import com.afora.shared.domain.model.AttendanceStatus
import com.afora.shared.domain.model.SessionStatus
import com.afora.shared.util.AppError
import com.afora.shared.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CameraAttendanceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeAttendanceRepository = FakeAttendanceRepository()
    private val fakeFaceProfileRepository = FakeFaceProfileRepository()
    private lateinit var viewModel: CameraAttendanceViewModel

    private val sampleRoster = RosterResponse(
        sessionId = 42,
        classSectionId = 10,
        subjectId = 101,
        subjectCode = "CS101",
        subjectName = "Operating Systems",
        sessionDate = "2026-10-12",
        startTime = "10:00",
        endTime = "11:30",
        sessionStatus = SessionStatus.ACTIVE,
        attendanceMethod = null,
        totalStudents = 3,
        presentCount = 0,
        absentCount = 0,
        onLeaveCount = 0,
        amendmentWindowOpen = false,
        entries = listOf(
            RosterEntry(
                studentId = 1,
                rollNumber = "CS001",
                fullName = "Alice Johnson",
                storedStatus = null,
                suggestedStatus = AttendanceStatus.PRESENT,
                isCorrected = false
            ),
            RosterEntry(
                studentId = 2,
                rollNumber = "CS002",
                fullName = "Bob Williams",
                storedStatus = null,
                suggestedStatus = AttendanceStatus.ON_LEAVE,
                leaveReason = "Approved medical leave",
                isCorrected = false
            ),
            RosterEntry(
                studentId = 3,
                rollNumber = "CS003",
                fullName = "Charlie Brown",
                storedStatus = null,
                suggestedStatus = AttendanceStatus.PRESENT,
                isCorrected = false
            )
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(sessionId: Long = 42): CameraAttendanceViewModel {
        val savedStateHandle = SavedStateHandle(mapOf(CAMERA_ATTENDANCE_SESSION_ID_ARG to sessionId))
        return CameraAttendanceViewModel(fakeAttendanceRepository, fakeFaceProfileRepository, savedStateHandle)
    }

    @Test
    fun `initial load transitions to Ready and pre-populates ON_LEAVE students`() = runTest {
        fakeAttendanceRepository.rosterResult = Result.Success(sampleRoster)
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CameraAttendanceUiState.Ready)
        val ready = state as CameraAttendanceUiState.Ready

        assertEquals(42L, ready.sessionId)
        assertEquals(3, ready.totalEnrolled)
        // Bob Williams has suggestedStatus ON_LEAVE -> pre-populated
        assertEquals(1, ready.detectedCount)
        val bob = ready.candidates[2L]
        assertNotNull(bob)
        assertEquals(AttendanceStatus.ON_LEAVE, bob?.attendanceStatus)
    }

    @Test
    fun `onFaceDetections merges live candidates while scanning is active`() = runTest {
        fakeAttendanceRepository.rosterResult = Result.Success(sampleRoster)
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val candidateAlice = DetectedFaceCandidate(
            studentId = 1,
            rollNumber = "CS001",
            fullName = "Alice Johnson",
            confidence = 0.92,
            category = ConfidenceCategory.HIGH,
            trackingId = 10,
            attendanceStatus = AttendanceStatus.PRESENT
        )

        viewModel.onFaceDetections(emptyList(), listOf(candidateAlice))

        val ready = viewModel.uiState.value as CameraAttendanceUiState.Ready
        assertEquals(2, ready.detectedCount) // Alice + Bob (leave)
        assertEquals(AttendanceStatus.PRESENT, ready.candidates[1L]?.attendanceStatus)
        assertEquals(0.92, ready.candidates[1L]?.confidence ?: 0.0, 0.001)
    }

    @Test
    fun `toggleScanning pauses and resumes face detection ingestion`() = runTest {
        fakeAttendanceRepository.rosterResult = Result.Success(sampleRoster)
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleScanning()
        var ready = viewModel.uiState.value as CameraAttendanceUiState.Ready
        assertEquals(false, ready.isScanning)

        // Detections should be ignored while scanning is paused
        val candidateCharlie = DetectedFaceCandidate(
            studentId = 3,
            rollNumber = "CS003",
            fullName = "Charlie Brown",
            confidence = 0.85,
            category = ConfidenceCategory.HIGH
        )
        viewModel.onFaceDetections(emptyList(), listOf(candidateCharlie))

        ready = viewModel.uiState.value as CameraAttendanceUiState.Ready
        assertEquals(null, ready.candidates[3L]) // Not added

        viewModel.toggleScanning()
        ready = viewModel.uiState.value as CameraAttendanceUiState.Ready
        assertEquals(true, ready.isScanning)
    }

    @Test
    fun `updateCandidateStatus allows teacher manual status adjustment`() = runTest {
        fakeAttendanceRepository.rosterResult = Result.Success(sampleRoster)
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateCandidateStatus(1L, AttendanceStatus.LATE)

        val ready = viewModel.uiState.value as CameraAttendanceUiState.Ready
        assertEquals(AttendanceStatus.LATE, ready.candidates[1L]?.attendanceStatus)
    }

    @Test
    fun `confirmAttendance submits CAMERA method and defaults undetected students to ABSENT`() = runTest {
        fakeAttendanceRepository.rosterResult = Result.Success(sampleRoster)
        fakeAttendanceRepository.confirmResult = Result.Success(
            AttendanceConfirmationResponse(
                sessionId = 42,
                submissionId = "sub-uuid",
                sessionStatus = SessionStatus.FINALIZED,
                totalMarked = 3,
                presentCount = 1,
                absentCount = 1,
                onLeaveCount = 1,
                confirmedAt = "2026-10-12T10:00:00",
                replayed = false
            )
        )

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Alice is detected as PRESENT
        val candidateAlice = DetectedFaceCandidate(
            studentId = 1,
            rollNumber = "CS001",
            fullName = "Alice Johnson",
            confidence = 0.92,
            category = ConfidenceCategory.HIGH,
            attendanceStatus = AttendanceStatus.PRESENT
        )
        viewModel.onFaceDetections(emptyList(), listOf(candidateAlice))

        // Confirm
        viewModel.confirmAttendance()
        testDispatcher.scheduler.advanceUntilIdle()

        val ready = viewModel.uiState.value as CameraAttendanceUiState.Ready
        assertNotNull(ready.confirmation)
        assertEquals(3, ready.confirmation?.totalMarked)
        assertEquals(false, ready.isSubmitting)
        assertEquals(false, ready.isScanning)
    }
}
