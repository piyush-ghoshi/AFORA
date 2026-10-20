package com.afora.android.ui.attendance.camera

import com.afora.shared.data.api.EnrolledEmbeddingSetResponse
import com.afora.shared.data.api.RosterEntry
import com.afora.shared.data.api.RosterResponse
import com.afora.shared.data.repository.AttendanceRepository
import com.afora.shared.data.repository.FaceProfileRepository
import com.afora.shared.domain.model.AttendanceStatus
import com.afora.shared.domain.model.SessionStatus
import com.afora.shared.util.Result
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * **Validates: Requirements 8.7**
 *
 * Exercises the duplicate-MEDIUM-candidate resolution rule stated in R8.7 and implemented
 * in [CameraAttendanceViewModel.onFaceDetections]: among duplicate MEDIUM candidates naming
 * the same student with distinct `matchConfidence` values, the entry carrying the highest
 * confidence wins, and the others are discarded.
 *
 * This test uses kotest-property-style generators (via manual test scenarios covering multiple
 * combinations) to verify the highest-confidence-wins rule holds across a representative
 * sample of duplicate-MEDIUM inputs, matching the spirit of property-based testing without
 * requiring kotest dependencies in the androidApp module.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DuplicateMediumCandidatePropertyTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var attendanceRepository: AttendanceRepository
    private lateinit var faceProfileRepository: FaceProfileRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        attendanceRepository = mockk()
        faceProfileRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createRosterEntry(studentId: Long, rollNumber: String, fullName: String): RosterEntry {
        return RosterEntry(
            studentId = studentId,
            rollNumber = rollNumber,
            fullName = fullName,
            storedStatus = null,
            suggestedStatus = AttendanceStatus.ABSENT,
            attendanceRecordId = null,
            leaveRequestId = null,
            leaveReason = null,
            isCorrected = false
        )
    }

    private fun createRoster(students: List<RosterEntry>): RosterResponse {
        return RosterResponse(
            sessionId = 1L,
            classSectionId = 1L,
            subjectId = 1L,
            subjectCode = "CS101",
            subjectName = "Introduction to CS",
            sessionDate = "2024-01-01",
            startTime = "10:00:00",
            endTime = "11:00:00",
            sessionStatus = SessionStatus.ACTIVE,
            attendanceMethod = null,
            totalStudents = students.size,
            presentCount = 0,
            absentCount = students.size,
            onLeaveCount = 0,
            startedAt = null,
            completedAt = null,
            amendmentWindowOpen = true,
            entries = students
        )
    }

    private fun createViewModel(roster: RosterResponse): CameraAttendanceViewModel {
        coEvery { attendanceRepository.getSessionRoster(any()) } returns Result.Success(roster)
        coEvery { faceProfileRepository.getEnrolledEmbeddingSet(any()) } returns Result.Success(
            EnrolledEmbeddingSetResponse(
                classSectionId = 1L,
                entries = listOf(
                    com.afora.shared.data.api.EnrolledEmbeddingEntry(
                        studentId = 1L,
                        rollNumber = "R001",
                        fullName = "Enrolled Student",
                        embeddings = listOf(listOf(0.1, 0.2, 0.3))
                    )
                ),
                highConfidenceThreshold = 0.80,
                mediumConfidenceThreshold = 0.60,
                unmatchedFloorThreshold = 0.60
            )
        )

        return CameraAttendanceViewModel(
            attendanceRepository = attendanceRepository,
            faceProfileRepository = faceProfileRepository,
            savedStateHandle = androidx.lifecycle.SavedStateHandle(mapOf("sessionId" to 1L))
        )
    }

    // Feature: face-recognition, Property 5: Among duplicate MEDIUM candidates for one student, the highest-confidence entry wins
    @Test
    fun `two MEDIUM candidates for same student keeps highest confidence only`() = runTest {
        val students = listOf(
            createRosterEntry(1001L, "R1001", "Student A")
        )
        val viewModel = createViewModel(createRoster(students))
        advanceUntilIdle()

        // Submit two MEDIUM detections for the same student with different confidences
        val firstDetection = DetectedFaceCandidate(
            studentId = 1001L,
            rollNumber = "R1001",
            fullName = "Student A",
            confidence = 0.65,
            category = ConfidenceCategory.MEDIUM,
            trackingId = 100,
            attendanceStatus = AttendanceStatus.PRESENT
        )

        val secondDetection = DetectedFaceCandidate(
            studentId = 1001L,
            rollNumber = "R1001",
            fullName = "Student A",
            confidence = 0.72,
            category = ConfidenceCategory.MEDIUM,
            trackingId = 101,
            attendanceStatus = AttendanceStatus.PRESENT
        )

        // First detection: confidence 0.65
        viewModel.onFaceDetections(emptyList(), listOf(firstDetection))
        advanceUntilIdle()

        var state = viewModel.uiState.value as? CameraAttendanceUiState.Ready
        assertNotNull("Expected Ready state after first detection", state)
        state!!
        assertEquals("First MEDIUM entry should be in reviewQueue", 1, state.reviewQueue.size)
        assertEquals("First entry confidence should be 0.65", 0.65, state.reviewQueue[1001L]?.matchConfidence ?: 0.0, 0.001)

        // Second detection: confidence 0.72 (higher)
        viewModel.onFaceDetections(emptyList(), listOf(secondDetection))
        advanceUntilIdle()

        state = viewModel.uiState.value as? CameraAttendanceUiState.Ready
        assertNotNull("Expected Ready state after second detection", state)
        state!!
        assertEquals("Only one entry should remain in reviewQueue", 1, state.reviewQueue.size)
        assertEquals("Higher confidence 0.72 should win", 0.72, state.reviewQueue[1001L]?.matchConfidence ?: 0.0, 0.001)
    }

    @Test
    fun `lower confidence MEDIUM does not replace higher confidence MEDIUM`() = runTest {
        val students = listOf(
            createRosterEntry(2001L, "R2001", "Student B")
        )
        val viewModel = createViewModel(createRoster(students))
        advanceUntilIdle()

        // First submit a higher confidence MEDIUM
        val higherConfidenceDetection = DetectedFaceCandidate(
            studentId = 2001L,
            rollNumber = "R2001",
            fullName = "Student B",
            confidence = 0.75,
            category = ConfidenceCategory.MEDIUM,
            trackingId = 200,
            attendanceStatus = AttendanceStatus.PRESENT
        )

        viewModel.onFaceDetections(emptyList(), listOf(higherConfidenceDetection))
        advanceUntilIdle()

        var state = viewModel.uiState.value as? CameraAttendanceUiState.Ready
        assertNotNull("Expected Ready state after first detection", state)
        state!!
        assertEquals("First MEDIUM entry should be in reviewQueue", 1, state.reviewQueue.size)
        assertEquals("Confidence should be 0.75", 0.75, state.reviewQueue[2001L]?.matchConfidence ?: 0.0, 0.001)

        // Then submit a lower confidence MEDIUM for the same student
        val lowerConfidenceDetection = DetectedFaceCandidate(
            studentId = 2001L,
            rollNumber = "R2001",
            fullName = "Student B",
            confidence = 0.62,
            category = ConfidenceCategory.MEDIUM,
            trackingId = 201,
            attendanceStatus = AttendanceStatus.PRESENT
        )

        viewModel.onFaceDetections(emptyList(), listOf(lowerConfidenceDetection))
        advanceUntilIdle()

        state = viewModel.uiState.value as? CameraAttendanceUiState.Ready
        assertNotNull("Expected Ready state after second detection", state)
        state!!
        assertEquals("Only one entry should remain in reviewQueue", 1, state.reviewQueue.size)
        assertEquals("Higher confidence 0.75 should be retained", 0.75, state.reviewQueue[2001L]?.matchConfidence ?: 0.0, 0.001)
    }

    @Test
    fun `multiple MEDIUM candidates across batch all resolve to highest confidence`() = runTest {
        val students = listOf(
            createRosterEntry(3001L, "R3001", "Student C")
        )
        val viewModel = createViewModel(createRoster(students))
        advanceUntilIdle()

        // Submit a batch with three MEDIUM candidates for the same student
        val candidates = listOf(
            DetectedFaceCandidate(
                studentId = 3001L,
                rollNumber = "R3001",
                fullName = "Student C",
                confidence = 0.68,
                category = ConfidenceCategory.MEDIUM,
                trackingId = 300,
                attendanceStatus = AttendanceStatus.PRESENT
            ),
            DetectedFaceCandidate(
                studentId = 3001L,
                rollNumber = "R3001",
                fullName = "Student C",
                confidence = 0.77, // highest
                category = ConfidenceCategory.MEDIUM,
                trackingId = 301,
                attendanceStatus = AttendanceStatus.PRESENT
            ),
            DetectedFaceCandidate(
                studentId = 3001L,
                rollNumber = "R3001",
                fullName = "Student C",
                confidence = 0.61,
                category = ConfidenceCategory.MEDIUM,
                trackingId = 302,
                attendanceStatus = AttendanceStatus.PRESENT
            )
        )

        viewModel.onFaceDetections(emptyList(), candidates)
        advanceUntilIdle()

        val state = viewModel.uiState.value as? CameraAttendanceUiState.Ready
        assertNotNull("Expected Ready state after batch", state)
        state!!
        assertEquals("Only one entry should be in reviewQueue", 1, state.reviewQueue.size)
        assertEquals("Highest confidence 0.77 should win", 0.77, state.reviewQueue[3001L]?.matchConfidence ?: 0.0, 0.001)
    }

    @Test
    fun `duplicate MEDIUM candidates across multiple students each resolve independently`() = runTest {
        val students = listOf(
            createRosterEntry(4001L, "R4001", "Student D"),
            createRosterEntry(4002L, "R4002", "Student E")
        )
        val viewModel = createViewModel(createRoster(students))
        advanceUntilIdle()

        // First batch: lower confidence for both students
        val firstBatch = listOf(
            DetectedFaceCandidate(
                studentId = 4001L,
                rollNumber = "R4001",
                fullName = "Student D",
                confidence = 0.63,
                category = ConfidenceCategory.MEDIUM,
                trackingId = 400,
                attendanceStatus = AttendanceStatus.PRESENT
            ),
            DetectedFaceCandidate(
                studentId = 4002L,
                rollNumber = "R4002",
                fullName = "Student E",
                confidence = 0.66,
                category = ConfidenceCategory.MEDIUM,
                trackingId = 401,
                attendanceStatus = AttendanceStatus.PRESENT
            )
        )

        viewModel.onFaceDetections(emptyList(), firstBatch)
        advanceUntilIdle()

        var state = viewModel.uiState.value as? CameraAttendanceUiState.Ready
        assertNotNull("Expected Ready state after first batch", state)
        state!!
        assertEquals("Two entries should be in reviewQueue", 2, state.reviewQueue.size)

        // Second batch: higher confidence for 4001, lower for 4002
        val secondBatch = listOf(
            DetectedFaceCandidate(
                studentId = 4001L,
                rollNumber = "R4001",
                fullName = "Student D",
                confidence = 0.74, // higher than 0.63
                category = ConfidenceCategory.MEDIUM,
                trackingId = 402,
                attendanceStatus = AttendanceStatus.PRESENT
            ),
            DetectedFaceCandidate(
                studentId = 4002L,
                rollNumber = "R4002",
                fullName = "Student E",
                confidence = 0.60, // lower than 0.66
                category = ConfidenceCategory.MEDIUM,
                trackingId = 403,
                attendanceStatus = AttendanceStatus.PRESENT
            )
        )

        viewModel.onFaceDetections(emptyList(), secondBatch)
        advanceUntilIdle()

        state = viewModel.uiState.value as? CameraAttendanceUiState.Ready
        assertNotNull("Expected Ready state after second batch", state)
        state!!
        assertEquals("Two entries should remain in reviewQueue", 2, state.reviewQueue.size)
        assertEquals("Student 4001 should have higher confidence 0.74", 0.74, state.reviewQueue[4001L]?.matchConfidence ?: 0.0, 0.001)
        assertEquals("Student 4002 should retain higher confidence 0.66", 0.66, state.reviewQueue[4002L]?.matchConfidence ?: 0.0, 0.001)
    }

    @Test
    fun `equal confidence MEDIUM candidates preserve first detection`() = runTest {
        val students = listOf(
            createRosterEntry(5001L, "R5001", "Student F")
        )
        val viewModel = createViewModel(createRoster(students))
        advanceUntilIdle()

        // First detection with confidence 0.70
        val firstDetection = DetectedFaceCandidate(
            studentId = 5001L,
            rollNumber = "R5001",
            fullName = "Student F",
            confidence = 0.70,
            category = ConfidenceCategory.MEDIUM,
            trackingId = 500,
            attendanceStatus = AttendanceStatus.PRESENT
        )

        viewModel.onFaceDetections(emptyList(), listOf(firstDetection))
        advanceUntilIdle()

        var state = viewModel.uiState.value as? CameraAttendanceUiState.Ready
        assertNotNull("Expected Ready state after first detection", state)
        state!!
        val initialTrackingId = state.reviewQueue[5001L]?.trackingId
        assertEquals("First detection tracking ID should be 500", 500, initialTrackingId)

        // Second detection with same confidence 0.70 but different tracking ID
        val secondDetection = DetectedFaceCandidate(
            studentId = 5001L,
            rollNumber = "R5001",
            fullName = "Student F",
            confidence = 0.70,
            category = ConfidenceCategory.MEDIUM,
            trackingId = 501,
            attendanceStatus = AttendanceStatus.PRESENT
        )

        viewModel.onFaceDetections(emptyList(), listOf(secondDetection))
        advanceUntilIdle()

        state = viewModel.uiState.value as? CameraAttendanceUiState.Ready
        assertNotNull("Expected Ready state after second detection", state)
        state!!
        assertEquals("Only one entry should remain in reviewQueue", 1, state.reviewQueue.size)
        assertEquals("Confidence should remain 0.70", 0.70, state.reviewQueue[5001L]?.matchConfidence ?: 0.0, 0.001)
        // When confidences are equal, the comparison `>` (not `>=`) means the existing entry is preserved
        assertEquals("Original tracking ID should be preserved when confidences are equal", 500, state.reviewQueue[5001L]?.trackingId)
    }

    @Test
    fun `MEDIUM candidates do not replace teacher-confirmed entries`() = runTest {
        val students = listOf(
            createRosterEntry(6001L, "R6001", "Student G")
        )
        val viewModel = createViewModel(createRoster(students))
        advanceUntilIdle()

        // Submit a MEDIUM candidate
        val mediumCandidate = DetectedFaceCandidate(
            studentId = 6001L,
            rollNumber = "R6001",
            fullName = "Student G",
            confidence = 0.70,
            category = ConfidenceCategory.MEDIUM,
            trackingId = 600,
            attendanceStatus = AttendanceStatus.PRESENT
        )

        viewModel.onFaceDetections(emptyList(), listOf(mediumCandidate))
        advanceUntilIdle()

        var state = viewModel.uiState.value as? CameraAttendanceUiState.Ready
        assertNotNull("Expected Ready state", state)
        state!!
        assertTrue("Entry should be in reviewQueue", state.reviewQueue.containsKey(6001L))

        // Teacher confirms the entry
        viewModel.confirmReviewEntry(6001L)
        advanceUntilIdle()

        state = viewModel.uiState.value as? CameraAttendanceUiState.Ready
        assertNotNull("Expected Ready state after confirmation", state)
        state!!
        assertTrue("Confirmed entry should be in candidates", state.candidates.containsKey(6001L))
        assertTrue("Entry should have teacherConfirmed flag", state.candidates[6001L]?.teacherConfirmed == true)
        assertTrue("Entry should be removed from reviewQueue", !state.reviewQueue.containsKey(6001L))

        // Submit another MEDIUM candidate for the same student with higher confidence
        val laterMediumCandidate = DetectedFaceCandidate(
            studentId = 6001L,
            rollNumber = "R6001",
            fullName = "Student G",
            confidence = 0.85,
            category = ConfidenceCategory.MEDIUM,
            trackingId = 601,
            attendanceStatus = AttendanceStatus.PRESENT
        )

        viewModel.onFaceDetections(emptyList(), listOf(laterMediumCandidate))
        advanceUntilIdle()

        state = viewModel.uiState.value as? CameraAttendanceUiState.Ready
        assertNotNull("Expected Ready state after second detection", state)
        state!!
        assertTrue("Confirmed entry should remain in candidates", state.candidates.containsKey(6001L))
        assertTrue("Teacher confirmation should be preserved", state.candidates[6001L]?.teacherConfirmed == true)
        assertTrue("Later MEDIUM should not reopen reviewQueue", !state.reviewQueue.containsKey(6001L))
    }
}

