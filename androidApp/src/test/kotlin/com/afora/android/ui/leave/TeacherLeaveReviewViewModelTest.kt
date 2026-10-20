package com.afora.android.ui.leave

import com.afora.android.ui.auth.FakeUserRepository
import com.afora.shared.data.api.LeaveRequestResponse
import com.afora.shared.domain.model.LeaveStatus
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TeacherLeaveReviewViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeUserRepository = FakeUserRepository()
    private val fakeLeaveRepository = FakeLeaveRepository()
    private lateinit var viewModel: TeacherLeaveReviewViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeUserRepository.profileResult = Result.Success(
            User(1, "uid_teacher", "teacher@afora.edu", "Dana", "Smith", "TEACHER", teacherId = 20)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = TeacherLeaveReviewViewModel(fakeLeaveRepository, fakeUserRepository)

    @Test
    fun `initial state is Loading`() {
        viewModel = buildViewModel()
        assertTrue(viewModel.uiState.value is TeacherLeaveReviewUiState.Loading)
    }

    @Test
    fun `load resolves teacherId from profile and shows the pending queue`() = runTest {
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is TeacherLeaveReviewUiState.Success)
        assertEquals(1, (state as TeacherLeaveReviewUiState.Success).requests.size)
    }

    @Test
    fun `profile load failure shows Error state`() = runTest {
        fakeUserRepository.profileResult = Result.Error(AppError.NetworkError("Network error"))

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is TeacherLeaveReviewUiState.Error)
        assertEquals("Network error", (state as TeacherLeaveReviewUiState.Error).message)
    }

    @Test
    fun `approve removes the request from the pending queue`() = runTest {
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        fakeLeaveRepository.approveResult = Result.Success(
            LeaveRequestResponse(
                id = 1, studentId = 201, studentName = "Alice Johnson", rollNumber = "2024CS001",
                classSectionId = 10, classSectionName = "CS101 A",
                fromDate = "2026-09-21", toDate = "2026-09-21", reason = "Medical appointment",
                status = LeaveStatus.APPROVED, reviewedByTeacherId = 20, reviewedByTeacherName = "Dana Smith",
                reviewedAt = "2026-09-16T09:00:00", reviewNotes = null,
                createdAt = "2026-09-15T10:00:00", updatedAt = "2026-09-16T09:00:00"
            )
        )

        viewModel.approve(1)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as TeacherLeaveReviewUiState.Success
        assertTrue(state.requests.isEmpty())
        assertEquals(1L, fakeLeaveRepository.lastApproveId)
    }

    @Test
    fun `reject removes the request and passes notes through`() = runTest {
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        fakeLeaveRepository.rejectResult = Result.Success(
            LeaveRequestResponse(
                id = 1, studentId = 201, studentName = "Alice Johnson", rollNumber = "2024CS001",
                classSectionId = 10, classSectionName = "CS101 A",
                fromDate = "2026-09-21", toDate = "2026-09-21", reason = "Medical appointment",
                status = LeaveStatus.REJECTED, reviewedByTeacherId = 20, reviewedByTeacherName = "Dana Smith",
                reviewedAt = "2026-09-16T09:00:00", reviewNotes = "No supporting document",
                createdAt = "2026-09-15T10:00:00", updatedAt = "2026-09-16T09:00:00"
            )
        )

        viewModel.reject(1, "No supporting document")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as TeacherLeaveReviewUiState.Success
        assertTrue(state.requests.isEmpty())
        assertEquals(1L to "No supporting document", fakeLeaveRepository.lastRejectArgs)
    }

    @Test
    fun `action failure keeps the request and surfaces the error`() = runTest {
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        fakeLeaveRepository.approveResult = Result.Error(AppError.ApiError(409, "Already decided"))

        viewModel.approve(1)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as TeacherLeaveReviewUiState.Success
        assertEquals(1, state.requests.size)
        assertEquals("Already decided", state.actionError)
    }
}