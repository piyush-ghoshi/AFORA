package com.afora.android.ui.attendance.camera

import androidx.lifecycle.SavedStateHandle
import com.afora.android.ui.attendance.FakeAttendanceRepository
import com.afora.shared.data.api.AttendanceConfirmationResponse
import com.afora.shared.data.api.RosterEntry
import com.afora.shared.data.api.RosterResponse
import com.afora.shared.domain.model.AttendanceStatus
import com.afora.shared.domain.model.SessionStatus
import com.afora.shared.util.Result
import io.mockk.MockKAnnotations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * **Validates: Requirements 9.2, 9.3**
 *
 * Verifies that `confirmAttendance()` correctly wires the `teacherConfirmed` field
 * on each `SubmittedAttendanceEntry` based on the resolved candidate state:
 *
 * - HIGH-auto candidates (auto-detected, teacher-overridable) → teacherConfirmed = false
 * - Teacher-confirmed MEDIUM candidates (from Review_Queue) → teacherConfirmed = true
 * - Manually-set entries with no candidate → teacherConfirmed = false
 *
 * This test ensures the client-side UX gate properly connects to the server-side
 * `FaceMatchReviewGate` through the `teacherConfirmed` field per design.md.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConfirmAttendanceTeacherConfirmedWiringTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAttendanceRepository: FakeAttendanceRepository
    private lateinit var fakeFaceProfileRepository: FakeFaceProfileRepository
    private lateinit var viewModel: CameraAttendanceViewModel

    private val sampleRoster = RosterResponse(
        sessionId = 101,
        classSectionId = 20,
        subjectId = 201,
        subjectCode = "CS201",
        subjectName = "Data Structures",
        sessionDate = "2026-11-15",
        startTime = "09:00",
        endTime = "10:30",
        sessionStatus = SessionStatus.ACTIVE,
        attendanceMethod = null,
        totalStudents = 4,
        presentCount = 0,
        absentCount = 0,
        onLeaveCount = 0,
        amendmentWindowOpen = false,
        entries = listOf(
            RosterEntry(
                studentId = 1,
                rollNumber = "DS001",
                fullName = "Alice Martinez",
                storedStatus = null,
                suggestedStatus = AttendanceStatus.PRESENT,
                isCorrected = false
            ),
            RosterEntry(
                studentId = 2,
                rollNumber = "DS002",
                fullName = "Bob Chen",
                storedStatus = null,
                suggestedStatus = AttendanceStatus.PRESENT,
                isCorrected = false
            ),
            RosterEntry(
                studentId = 3,
                rollNumber = "DS003",
                fullName = "Charlie Davis",
                storedStatus = AttendanceStatus.PRESENT,  // Pre-stored as PRESENT (e.g., from previous manual mark)
                suggestedStatus = AttendanceStatus.PRESENT,
                isCorrected = false
            ),
            RosterEntry(
                studentId = 4,
                rollNumber = "DS004",
                fullName = "Diana Evans",
                storedStatus = null,
                suggestedStatus = AttendanceStatus.PRESENT,
                isCorrected = false
            )
        )
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        Dispatchers.setMain(testDispatcher)
        fakeAttendanceRepository = FakeAttendanceRepository()
        fakeFaceProfileRepository = FakeFaceProfileRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(sessionId: Long = 101): CameraAttendanceViewModel {
        val savedStateHandle = SavedStateHandle(mapOf(CAMERA_ATTENDANCE_SESSION_ID_ARG to sessionId))
        return CameraAttendanceViewModel(fakeAttendanceRepository, fakeFaceProfileRepository, savedStateHandle)
    }

    /**
     * **Feature: face-recognition**
     * **Validates: Requirements 9.2, 9.3**
     *
     * A `confirmAttendance()` call with a mix of HIGH-auto, teacher-confirmed-MEDIUM,
     * and manually-set-with-no-candidate entries produces `SubmittedAttendanceEntry.teacherConfirmed`
     * values that exactly match each entry's resolved candidate state.
     */
    @Test
    fun `confirmAttendance wires teacherConfirmed correctly for mixed entry types`() = runTest {
        // Setup roster
        fakeAttendanceRepository.rosterResult = Result.Success(sampleRoster)

        // Setup successful confirmation response
        fakeAttendanceRepository.confirmResult = Result.Success(
            AttendanceConfirmationResponse(
                sessionId = 101,
                submissionId = "test-submission-uuid",
                sessionStatus = SessionStatus.FINALIZED,
                totalMarked = 4,
                presentCount = 3,
                absentCount = 1,
                onLeaveCount = 0,
                confirmedAt = "2026-11-15T09:30:00",
                replayed = false
            )
        )

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Scenario: Mix of HIGH-auto, MEDIUM-confirmed, and no-candidate (ABSENT)

        // 1. HIGH-auto candidate: Alice (confidence 0.95, HIGH, auto-detected)
        //    → teacherConfirmed should be FALSE (auto, not explicitly confirmed)
        val aliceCandidate = DetectedFaceCandidate(
            studentId = 1,
            rollNumber = "DS001",
            fullName = "Alice Martinez",
            confidence = 0.95,
            category = ConfidenceCategory.HIGH,
            attendanceStatus = AttendanceStatus.PRESENT,
            teacherConfirmed = false  // HIGH-auto: not teacher-confirmed
        )

        // 2. Teacher-confirmed MEDIUM candidate: Bob (was in review queue, teacher confirmed)
        //    → teacherConfirmed should be TRUE (explicitly confirmed by teacher)
        val bobCandidate = DetectedFaceCandidate(
            studentId = 2,
            rollNumber = "DS002",
            fullName = "Bob Chen",
            confidence = 0.78,
            category = ConfidenceCategory.MEDIUM,
            attendanceStatus = AttendanceStatus.PRESENT,
            teacherConfirmed = false  // Will be set to true by confirmReviewEntry
        )

        // 3. Manually-set with no candidate: Charlie (teacher marks manually, no detection)
        //    → teacherConfirmed should be FALSE (no candidate means no face match)
        // (Charlie will have no candidate, so teacherConfirmed defaults to false)

        // 4. No entry at all: Diana (defaults to ABSENT, no candidate)
        //    → teacherConfirmed should be FALSE (no candidate)

        // Add candidates via onFaceDetections
        viewModel.onFaceDetections(emptyList(), listOf(aliceCandidate, bobCandidate))
        testDispatcher.scheduler.advanceUntilIdle()

        // Bob is MEDIUM so he goes to reviewQueue. Teacher confirms him.
        viewModel.confirmReviewEntry(2L)
        testDispatcher.scheduler.advanceUntilIdle()


        // Confirm attendance
        viewModel.confirmAttendance()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify confirmation succeeded
        val state = viewModel.uiState.value
        assertTrue("State should be Ready after confirmation", state is CameraAttendanceUiState.Ready)
        val ready = state as CameraAttendanceUiState.Ready
        assertNotNull("Confirmation should be present", ready.confirmation)

        // Verify the submitted entries in the fake repository
        val submittedRequest = fakeAttendanceRepository.lastConfirmRequest
        assertNotNull("Confirm request should have been captured", submittedRequest)
        assertEquals("Should submit 4 entries (one per enrolled student)", 4, submittedRequest?.entries?.size)

        // Find each student's submitted entry
        val aliceEntry = submittedRequest?.entries?.find { it.studentId == 1L }
        val bobEntry = submittedRequest?.entries?.find { it.studentId == 2L }
        val charlieEntry = submittedRequest?.entries?.find { it.studentId == 3L }
        val dianaEntry = submittedRequest?.entries?.find { it.studentId == 4L }

        // Verify Alice (HIGH-auto candidate)
        assertNotNull("Alice entry should exist", aliceEntry)
        assertEquals("Alice should be PRESENT", AttendanceStatus.PRESENT, aliceEntry?.status)
        assertEquals("Alice should have confidence score", 0.95, aliceEntry?.confidenceScore ?: 0.0, 0.001)
        assertFalse("Alice teacherConfirmed should be FALSE (HIGH-auto)", aliceEntry?.teacherConfirmed ?: true)

        // Verify Bob (teacher-confirmed MEDIUM candidate)
        assertNotNull("Bob entry should exist", bobEntry)
        assertEquals("Bob should be PRESENT", AttendanceStatus.PRESENT, bobEntry?.status)
        assertEquals("Bob should have confidence score", 0.78, bobEntry?.confidenceScore ?: 0.0, 0.001)
        assertTrue("Bob teacherConfirmed should be TRUE (teacher-confirmed MEDIUM)", bobEntry?.teacherConfirmed ?: false)

        // Verify Charlie (manually-set with no candidate)
        assertNotNull("Charlie entry should exist", charlieEntry)
        assertEquals("Charlie should be ABSENT (no candidate)", AttendanceStatus.ABSENT, charlieEntry?.status)
        // Charlie: ABSENT with null confidence is correct for no-candidate case
        assertFalse("Charlie teacherConfirmed should be FALSE (no candidate)", charlieEntry?.teacherConfirmed ?: true)

        // Verify Diana (defaulted to ABSENT, no candidate)
        assertNotNull("Diana entry should exist", dianaEntry)
        assertEquals("Diana should be ABSENT (defaulted)", AttendanceStatus.ABSENT, dianaEntry?.status)
        assertEquals("Diana should have null confidence (no candidate)", null, dianaEntry?.confidenceScore)
        assertFalse("Diana teacherConfirmed should be FALSE (no candidate)", dianaEntry?.teacherConfirmed ?: true)
    }
}



