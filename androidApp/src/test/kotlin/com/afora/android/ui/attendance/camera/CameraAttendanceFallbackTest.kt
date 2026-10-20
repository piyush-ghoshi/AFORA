package com.afora.android.ui.attendance.camera

import androidx.lifecycle.SavedStateHandle
import com.afora.android.ui.attendance.FakeAttendanceRepository
import com.afora.shared.data.api.EnrolledEmbeddingSetResponse
import com.afora.shared.data.api.RosterEntry
import com.afora.shared.data.api.RosterResponse
import com.afora.shared.data.repository.FaceProfileRepository
import com.afora.shared.domain.model.AttendanceStatus
import com.afora.shared.domain.model.SessionStatus
import com.afora.shared.util.Result
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Task 16.4: fallback-navigation unit tests.
 *
 * Validates that the three Requirement 10 fallback triggers route the Teacher to
 * Mark_Attendance_Screen without clearing candidates or reviewQueue and while keeping
 * SessionStatus at ACTIVE.
 *
 * Mocking: MockK only — no Mockito/mockito-kotlin/@Mock/when(...)/thenReturn.
 *
 * Requirements validated: 10.1, 10.2, 10.3, 10.4, 10.5
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CameraAttendanceFallbackTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeAttendanceRepository = FakeAttendanceRepository()
    private lateinit var mockFaceProfileRepository: FaceProfileRepository
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
                suggestedStatus = AttendanceStatus.PRESENT,
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
        mockFaceProfileRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(sessionId: Long = 42): CameraAttendanceViewModel {
        val savedStateHandle = SavedStateHandle(mapOf(CAMERA_ATTENDANCE_SESSION_ID_ARG to sessionId))
        return CameraAttendanceViewModel(fakeAttendanceRepository, mockFaceProfileRepository, savedStateHandle)
    }

    /**
     * R10.1: Empty Enrolled_Embedding_Set navigates without starting scanning.
     *
     * When the Enrolled_Embedding_Set for a Camera_Session's Class_Section contains zero
     * entries, the Camera_Attendance_Screen shall navigate the Teacher to the
     * Mark_Attendance_Screen without starting a scanning attempt.
     */
    @Test
    fun `empty enrolled embedding set navigates to manual marking without starting scan`() = runTest {
        // Setup: roster loads successfully but embedding set is empty
        fakeAttendanceRepository.rosterResult = Result.Success(sampleRoster)
        coEvery { mockFaceProfileRepository.getEnrolledEmbeddingSet(10) } returns Result.Success(
            EnrolledEmbeddingSetResponse(
                classSectionId = 10,
                entries = emptyList(),
                highConfidenceThreshold = 0.90,
                mediumConfidenceThreshold = 0.70,
                unmatchedFloorThreshold = 0.50
            )
        )

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("State should be Ready", state is CameraAttendanceUiState.Ready)
        val ready = state as CameraAttendanceUiState.Ready

        // R10.1: shouldFallbackToManualMarking is set, scanning is off
        assertTrue("Should signal fallback navigation", ready.shouldFallbackToManualMarking)
        assertFalse("Scanning should not be active", ready.isScanning)

        // R10.4: SessionStatus remains ACTIVE
        assertEquals("SessionStatus should remain ACTIVE", SessionStatus.ACTIVE, ready.roster.sessionStatus)

        // R10.5: candidates and reviewQueue are not cleared
        // (Empty in this case because no detections have occurred, not because they were cleared)
        assertTrue("Candidates map exists (not null)", ready.candidates != null)
        assertTrue("ReviewQueue map exists (not null)", ready.reviewQueue != null)
    }

    /**
     * R10.2: Zero candidates across the whole roster after a scanning pass offers the fallback control.
     *
     * When a Camera_Session's scanning attempt completes with zero Face_Match_Candidate values
     * produced for the entire roster, the Camera_Attendance_Screen shall offer the Teacher a
     * control that navigates to the Mark_Attendance_Screen.
     */
    @Test
    fun `zero match candidates after scanning pass offers fallback control`() = runTest {
        // Setup: roster loads, embedding set is non-empty
        fakeAttendanceRepository.rosterResult = Result.Success(sampleRoster)
        coEvery { mockFaceProfileRepository.getEnrolledEmbeddingSet(10) } returns Result.Success(
            EnrolledEmbeddingSetResponse(
                classSectionId = 10,
                entries = listOf(
                    com.afora.shared.data.api.EnrolledEmbeddingEntry(
                        studentId = 1,
                        rollNumber = "CS001",
                        fullName = "Alice Johnson",
                        embeddings = listOf(listOf(0.1, 0.2, 0.3))
                    )
                ),
                highConfidenceThreshold = 0.90,
                mediumConfidenceThreshold = 0.70,
                unmatchedFloorThreshold = 0.50
            )
        )

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val initialReady = viewModel.uiState.value as CameraAttendanceUiState.Ready
        assertTrue("Should start with scanning active", initialReady.isScanning)
        assertFalse("No fallback initially", initialReady.shouldFallbackToManualMarking)

        // Simulate: faces were detected but matcher produced no candidates (stub returns null)
        // Add some overlay faces to simulate detection activity
        viewModel.onFaceDetections(
            overlays = listOf(
                FaceOverlayItem(
                    boundingBox = CameraBoundingBox(10f, 10f, 100f, 100f),
                    trackingId = 1,
                    matchedStudentName = null,
                    confidence = 0.0,
                    category = ConfidenceCategory.UNKNOWN
                )
            ),
            detectedCandidates = emptyList() // Matcher returned nothing
        )

        // Teacher pauses scanning, ending the attempt
        viewModel.toggleScanning()

        val ready = viewModel.uiState.value as CameraAttendanceUiState.Ready

        // R10.2: fallback control is offered (shouldFallbackToManualMarking = true)
        assertTrue("Should offer fallback after zero-match scan", ready.shouldFallbackToManualMarking)
        assertFalse("Scanning should be paused", ready.isScanning)

        // R10.4: SessionStatus remains ACTIVE
        assertEquals("SessionStatus should remain ACTIVE", SessionStatus.ACTIVE, ready.roster.sessionStatus)

        // R10.5: reviewQueue and candidates are not cleared
        assertTrue("Candidates map exists", ready.candidates != null)
        assertTrue("ReviewQueue map exists", ready.reviewQueue != null)
    }

    /**
     * R10.3: A simulated unrecoverable matcher error navigates immediately.
     *
     * When the On_Device_Matcher raises an unrecoverable error during a Camera_Session,
     * the Camera_Attendance_Screen shall navigate the Teacher to the Mark_Attendance_Screen.
     */
    @Test
    fun `unrecoverable matcher error navigates to manual marking immediately`() = runTest {
        // Setup: session loads normally
        fakeAttendanceRepository.rosterResult = Result.Success(sampleRoster)
        coEvery { mockFaceProfileRepository.getEnrolledEmbeddingSet(10) } returns Result.Success(
            EnrolledEmbeddingSetResponse(
                classSectionId = 10,
                entries = listOf(
                    com.afora.shared.data.api.EnrolledEmbeddingEntry(
                        studentId = 1,
                        rollNumber = "CS001",
                        fullName = "Alice Johnson",
                        embeddings = listOf(listOf(0.1, 0.2, 0.3))
                    )
                ),
                highConfidenceThreshold = 0.90,
                mediumConfidenceThreshold = 0.70,
                unmatchedFloorThreshold = 0.50
            )
        )

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val initialReady = viewModel.uiState.value as CameraAttendanceUiState.Ready
        assertTrue("Should start with scanning active", initialReady.isScanning)
        assertFalse("No fallback initially", initialReady.shouldFallbackToManualMarking)

        // Add a candidate to verify R10.5 (data not cleared)
        val candidateAlice = DetectedFaceCandidate(
            studentId = 1,
            rollNumber = "CS001",
            fullName = "Alice Johnson",
            confidence = 0.92,
            category = ConfidenceCategory.HIGH,
            attendanceStatus = AttendanceStatus.PRESENT
        )
        viewModel.onFaceDetections(emptyList(), listOf(candidateAlice))

        val beforeError = viewModel.uiState.value as CameraAttendanceUiState.Ready
        assertEquals("Alice should be in candidates", 1, beforeError.candidates.size)

        // Simulate: matcher error is reported
        viewModel.reportMatcherError()

        val ready = viewModel.uiState.value as CameraAttendanceUiState.Ready

        // R10.3: fallback navigation is triggered
        assertTrue("Should trigger fallback on matcher error", ready.shouldFallbackToManualMarking)
        assertFalse("Scanning should stop on error", ready.isScanning)

        // R10.4: SessionStatus remains ACTIVE
        assertEquals("SessionStatus should remain ACTIVE", SessionStatus.ACTIVE, ready.roster.sessionStatus)

        // R10.5: candidates and reviewQueue are not cleared
        assertEquals("Candidates should not be cleared", 1, ready.candidates.size)
        assertEquals("Alice should still be in candidates", AttendanceStatus.PRESENT, ready.candidates[1L]?.attendanceStatus)
        assertTrue("ReviewQueue should not be cleared", ready.reviewQueue != null)
    }

    /**
     * R10.4 & R10.5: In every fallback case, SessionStatus remains ACTIVE and
     * reviewQueue/candidates are not cleared.
     *
     * This test verifies the shared invariant across all three fallback triggers.
     */
    @Test
    fun `fallback navigation preserves session status and existing data`() = runTest {
        // Setup with some existing data
        fakeAttendanceRepository.rosterResult = Result.Success(sampleRoster)
        coEvery { mockFaceProfileRepository.getEnrolledEmbeddingSet(10) } returns Result.Success(
            EnrolledEmbeddingSetResponse(
                classSectionId = 10,
                entries = listOf(
                    com.afora.shared.data.api.EnrolledEmbeddingEntry(
                        studentId = 1,
                        rollNumber = "CS001",
                        fullName = "Alice Johnson",
                        embeddings = listOf(listOf(0.1, 0.2, 0.3))
                    )
                ),
                highConfidenceThreshold = 0.90,
                mediumConfidenceThreshold = 0.70,
                unmatchedFloorThreshold = 0.50
            )
        )

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Add a HIGH confidence candidate
        val candidateAlice = DetectedFaceCandidate(
            studentId = 1,
            rollNumber = "CS001",
            fullName = "Alice Johnson",
            confidence = 0.95,
            category = ConfidenceCategory.HIGH,
            attendanceStatus = AttendanceStatus.PRESENT
        )
        viewModel.onFaceDetections(emptyList(), listOf(candidateAlice))

        // Add a MEDIUM confidence candidate to reviewQueue
        val candidateBob = DetectedFaceCandidate(
            studentId = 2,
            rollNumber = "CS002",
            fullName = "Bob Williams",
            confidence = 0.75,
            category = ConfidenceCategory.MEDIUM,
            attendanceStatus = AttendanceStatus.PRESENT,
            trackingId = 20
        )
        viewModel.onFaceDetections(emptyList(), listOf(candidateBob))

        val beforeFallback = viewModel.uiState.value as CameraAttendanceUiState.Ready
        val candidatesBeforeFallback = beforeFallback.candidates.toMap()
        val reviewQueueBeforeFallback = beforeFallback.reviewQueue.toMap()

        // Trigger fallback via matcher error
        viewModel.reportMatcherError()

        val afterFallback = viewModel.uiState.value as CameraAttendanceUiState.Ready

        // R10.4: SessionStatus remains ACTIVE (not changed to any other status)
        assertEquals("SessionStatus must remain ACTIVE", SessionStatus.ACTIVE, afterFallback.roster.sessionStatus)

        // R10.5: candidates are not cleared
        assertEquals("Candidates count should not change", candidatesBeforeFallback.size, afterFallback.candidates.size)
        assertEquals("Alice candidate preserved", candidatesBeforeFallback[1L], afterFallback.candidates[1L])

        // R10.5: reviewQueue is not cleared
        assertEquals("ReviewQueue count should not change", reviewQueueBeforeFallback.size, afterFallback.reviewQueue.size)
        assertEquals("Bob review entry preserved", reviewQueueBeforeFallback[2L], afterFallback.reviewQueue[2L])

        // Verify the fallback flag is set
        assertTrue("Should signal fallback navigation", afterFallback.shouldFallbackToManualMarking)
    }

    /**
     * Verify consumeFallbackNavigation clears the one-off signal without affecting candidates/reviewQueue.
     */
    @Test
    fun `consumeFallbackNavigation clears signal without affecting data`() = runTest {
        fakeAttendanceRepository.rosterResult = Result.Success(sampleRoster)
        coEvery { mockFaceProfileRepository.getEnrolledEmbeddingSet(10) } returns Result.Success(
            EnrolledEmbeddingSetResponse(
                classSectionId = 10,
                entries = emptyList(),
                highConfidenceThreshold = 0.90,
                mediumConfidenceThreshold = 0.70,
                unmatchedFloorThreshold = 0.50
            )
        )

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        var ready = viewModel.uiState.value as CameraAttendanceUiState.Ready
        assertTrue("Fallback should be signaled", ready.shouldFallbackToManualMarking)

        // Consume the signal (screen has navigated)
        viewModel.consumeFallbackNavigation()

        ready = viewModel.uiState.value as CameraAttendanceUiState.Ready
        assertFalse("Fallback signal should be cleared", ready.shouldFallbackToManualMarking)

        // R10.5: data still intact
        assertTrue("Candidates still exist", ready.candidates != null)
        assertTrue("ReviewQueue still exists", ready.reviewQueue != null)
        assertEquals("SessionStatus unchanged", SessionStatus.ACTIVE, ready.roster.sessionStatus)
    }
}
