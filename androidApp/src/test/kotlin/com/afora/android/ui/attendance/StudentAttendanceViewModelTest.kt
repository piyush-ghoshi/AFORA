package com.afora.android.ui.attendance

import com.afora.android.ui.auth.FakeUserRepository
import com.afora.shared.data.api.SessionAttendanceEntry
import com.afora.shared.data.api.StudentAttendanceResponse
import com.afora.shared.data.api.SubjectAttendanceSummary
import com.afora.shared.domain.model.AttendanceStatus
import com.afora.shared.domain.model.User
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StudentAttendanceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeUserRepository = FakeUserRepository()
    private val fakeAttendanceRepository = FakeAttendanceRepository()
    private lateinit var viewModel: StudentAttendanceViewModel

    private val sampleResponse = StudentAttendanceResponse(
        studentId = 7,
        semesterId = "sem-123",
        overallPercentage = 80.0,
        totalLectures = 10,
        presentCount = 8,
        absentCount = 2,
        onLeaveCount = 0,
        subjects = listOf(
            SubjectAttendanceSummary(
                subjectId = 101,
                subjectCode = "CS101",
                subjectName = "Intro to Computer Science",
                totalLectures = 5,
                presentCount = 4,
                absentCount = 1,
                onLeaveCount = 0,
                attendancePercentage = 80.0,
                sessions = listOf(
                    SessionAttendanceEntry(
                        sessionId = 1,
                        sessionDate = "2026-10-10",
                        startTime = "09:00:00",
                        endTime = "10:30:00",
                        status = AttendanceStatus.PRESENT,
                        isCorrected = false,
                        notes = null
                    )
                )
            )
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = StudentAttendanceViewModel(
        userRepository = fakeUserRepository,
        attendanceRepository = fakeAttendanceRepository
    )

    @Test
    fun `initial state is Loading`() {
        viewModel = buildViewModel()
        assertTrue(viewModel.uiState.value is StudentAttendanceUiState.Loading)
    }

    @Test
    fun `when profile fails, sets Error state`() = runTest {
        fakeUserRepository.profileResult = Result.Error(AppError.NetworkError("Network down"))

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is StudentAttendanceUiState.Error)
        assertEquals("Network down", (state as StudentAttendanceUiState.Error).message)
    }

    @Test
    fun `when studentId is missing, sets profile setup error`() = runTest {
        fakeUserRepository.profileResult = Result.Success(
            User(1, "uid123", "student@afora.edu", "John", "Doe", "STUDENT", studentId = null)
        )

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is StudentAttendanceUiState.Error)
        assertTrue((state as StudentAttendanceUiState.Error).message.contains("student profile is not set up"))
    }

    @Test
    fun `when attendance detail succeeds, sets Success state`() = runTest {
        fakeUserRepository.profileResult = Result.Success(
            User(1, "uid123", "student@afora.edu", "John", "Doe", "STUDENT", studentId = 7)
        )
        fakeAttendanceRepository.studentAttendanceDetailResult = Result.Success(sampleResponse)

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is StudentAttendanceUiState.Success)
        val success = state as StudentAttendanceUiState.Success
        assertEquals(80.0, success.response.overallPercentage, 0.01)
        assertEquals(1, success.response.subjects.size)
        assertNull(success.expandedSubjectId)
    }

    @Test
    fun `toggleSubject expands and collapses accordion`() = runTest {
        fakeUserRepository.profileResult = Result.Success(
            User(1, "uid123", "student@afora.edu", "John", "Doe", "STUDENT", studentId = 7)
        )
        fakeAttendanceRepository.studentAttendanceDetailResult = Result.Success(sampleResponse)

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Initially null
        var success = viewModel.uiState.value as StudentAttendanceUiState.Success
        assertNull(success.expandedSubjectId)

        // Expand subject 101
        viewModel.toggleSubject(101)
        success = viewModel.uiState.value as StudentAttendanceUiState.Success
        assertEquals(101L, success.expandedSubjectId)

        // Toggle again collapses
        viewModel.toggleSubject(101)
        success = viewModel.uiState.value as StudentAttendanceUiState.Success
        assertNull(success.expandedSubjectId)
    }

    @Test
    fun `when attendance repo returns error, sets Error state`() = runTest {
        fakeUserRepository.profileResult = Result.Success(
            User(1, "uid123", "student@afora.edu", "John", "Doe", "STUDENT", studentId = 7)
        )
        fakeAttendanceRepository.studentAttendanceDetailResult = Result.Error(AppError.ApiError(500, "500 Internal error"))

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is StudentAttendanceUiState.Error)
        assertEquals("500 Internal error", (state as StudentAttendanceUiState.Error).message)
    }
}
