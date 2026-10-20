package com.afora.android.ui.leave

import com.afora.shared.data.api.LeaveRequestResponse
import com.afora.shared.domain.model.LeaveStatus
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
class StudentLeaveViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeLeaveRepository = FakeLeaveRepository()
    private lateinit var viewModel: StudentLeaveViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = StudentLeaveViewModel(fakeLeaveRepository)

    @Test
    fun `initial state is Loading`() {
        viewModel = buildViewModel()
        assertTrue(viewModel.uiState.value is StudentLeaveUiState.Loading)
    }

    @Test
    fun `load succeeds and shows the student's own requests`() = runTest {
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is StudentLeaveUiState.Success)
        assertEquals(1, (state as StudentLeaveUiState.Success).requests.size)
    }

    @Test
    fun `load failure shows Error state`() = runTest {
        fakeLeaveRepository.myRequestsResult = Result.Error(AppError.NetworkError("Network error"))

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is StudentLeaveUiState.Error)
        assertEquals("Network error", (state as StudentLeaveUiState.Error).message)
    }

    @Test
    fun `submit success prepends the new request and clears submitting`() = runTest {
        fakeLeaveRepository.myRequestsResult = Result.Success(emptyList())
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        fakeLeaveRepository.submitResult = Result.Success(
            LeaveRequestResponse(
                id = 42, studentId = 201, studentName = "Alice", rollNumber = "2024CS001",
                classSectionId = null, classSectionName = null,
                fromDate = "2026-10-01", toDate = "2026-10-02", reason = "Trip",
                status = LeaveStatus.PENDING, reviewedByTeacherId = null, reviewedByTeacherName = null,
                reviewedAt = null, reviewNotes = null,
                createdAt = "2026-09-20T10:00:00", updatedAt = "2026-09-20T10:00:00"
            )
        )

        viewModel.submit("2026-10-01", "2026-10-02", "Trip")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as StudentLeaveUiState.Success
        assertEquals(1, state.requests.size)
        assertEquals(42L, state.requests.first().id)
        assertEquals(false, state.isSubmitting)
        assertNull(state.submitError)
        assertEquals(listOf("2026-10-01", "2026-10-02", "Trip", null), fakeLeaveRepository.lastSubmitArgs)
    }

    @Test
    fun `submit failure surfaces the error without touching the list`() = runTest {
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        fakeLeaveRepository.submitResult = Result.Error(AppError.ApiError(400, "Invalid dates"))

        viewModel.submit("2026-10-02", "2026-10-01", "Bad range")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as StudentLeaveUiState.Success
        assertEquals(1, state.requests.size) // unchanged
        assertEquals("Invalid dates", state.submitError)
    }

    @Test
    fun `cancel success updates the matching request in place`() = runTest {
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        fakeLeaveRepository.cancelResult = Result.Success(
            LeaveRequestResponse(
                id = 1, studentId = 201, studentName = "Alice Johnson", rollNumber = "2024CS001",
                classSectionId = 10, classSectionName = "CS101 A",
                fromDate = "2026-09-21", toDate = "2026-09-21", reason = "Medical appointment",
                status = LeaveStatus.CANCELLED, reviewedByTeacherId = null, reviewedByTeacherName = null,
                reviewedAt = null, reviewNotes = null,
                createdAt = "2026-09-15T10:00:00", updatedAt = "2026-09-15T10:00:00"
            )
        )

        viewModel.cancel(1)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as StudentLeaveUiState.Success
        assertEquals(LeaveStatus.CANCELLED, state.requests.first().status)
        assertEquals(1L, fakeLeaveRepository.lastCancelId)
    }

    @Test
    fun `load also fetches the class options for the scope picker`() = runTest {
        fakeLeaveRepository.classOptionsResult = Result.Success(
            listOf(
                com.afora.shared.data.api.LeaveClassOption(
                    classSectionId = 10, subjectCode = "CS101", subjectName = "Data Structures", sectionName = "CS101 A"
                )
            )
        )
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as StudentLeaveUiState.Success
        assertEquals(1, state.classOptions.size)
        assertEquals("CS101", state.classOptions.first().subjectCode)
    }

    @Test
    fun `a failing class options call does not block the screen`() = runTest {
        fakeLeaveRepository.classOptionsResult = Result.Error(AppError.NetworkError("offline"))
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as StudentLeaveUiState.Success
        assertTrue(state.classOptions.isEmpty())
        assertEquals(1, state.requests.size)
    }

    @Test
    fun `submitCount only increments on a successful submit so a failed one keeps the form`() = runTest {
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, (viewModel.uiState.value as StudentLeaveUiState.Success).submitCount)

        fakeLeaveRepository.submitResult = Result.Error(AppError.ApiError(400, "Invalid dates"))
        viewModel.submit("2026-10-01", "2026-10-02", "Trip")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, (viewModel.uiState.value as StudentLeaveUiState.Success).submitCount)

        fakeLeaveRepository.submitResult = null
        viewModel.submit("2026-10-01", "2026-10-02", "Trip")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, (viewModel.uiState.value as StudentLeaveUiState.Success).submitCount)
    }

    @Test
    fun `the selected class section is sent with the request`() = runTest {
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.submit("2026-10-01", "2026-10-02", "Trip", classSectionId = 10)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("2026-10-01", "2026-10-02", "Trip", 10L), fakeLeaveRepository.lastSubmitArgs)
    }
}