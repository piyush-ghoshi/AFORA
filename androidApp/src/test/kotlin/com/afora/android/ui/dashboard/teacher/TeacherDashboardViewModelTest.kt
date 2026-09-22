package com.afora.android.ui.dashboard.teacher

import com.afora.android.ui.auth.FakeAuthRepository
import com.afora.android.ui.auth.FakeUserRepository
import com.afora.android.ui.dashboard.FakeDashboardRepository
import com.afora.android.ui.dashboard.TeacherDashboardUiState
import com.afora.shared.data.repository.TeacherDashboardStats
import com.afora.shared.domain.model.LectureSession
import com.afora.shared.domain.model.SessionStatus
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
class TeacherDashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeAuthRepository = FakeAuthRepository()
    private val fakeUserRepository = FakeUserRepository()
    private val fakeDashboardRepository = FakeDashboardRepository()
    private lateinit var viewModel: TeacherDashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = TeacherDashboardViewModel(
        fakeAuthRepository,
        fakeUserRepository,
        fakeDashboardRepository
    )

    @Test
    fun `initial state is Loading`() {
        viewModel = buildViewModel()
        
        assertTrue(viewModel.uiState.value is TeacherDashboardUiState.Loading)
    }

    @Test
    fun `when user profile loads successfully and dashboard data loads shows Success state`() = runTest {
        // Setup: user profile succeeds
        fakeUserRepository.profileResult = Result.Success(
            User(1, "uid123", "teacher@afora.edu", "Jane", "Smith", "TEACHER")
        )
        
        // Setup: dashboard data succeeds
        fakeDashboardRepository.teacherDashboardResult = Result.Success(
            TeacherDashboardStats(
                todaySchedule = listOf(
                    LectureSession(
                        id = 1,
                        subjectId = 101,
                        classSectionId = 10,
                        teacherId = 1,
                        semesterId = 5,
                        date = "2026-09-17",
                        startTime = "09:00:00",
                        endTime = "10:30:00",
                        roomNumber = "A-101",
                        status = SessionStatus.SCHEDULED
                    )
                ),
                pendingLeaveRequests = 5,
                pendingQueries = 3
            )
        )

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is TeacherDashboardUiState.Success)
        
        val successState = state as TeacherDashboardUiState.Success
        assertEquals("Jane", successState.data.user.firstName)
        assertEquals(1, successState.data.todaySchedule.size)
        assertEquals(5, successState.data.pendingLeaveRequests)
        assertEquals(3, successState.data.pendingQueries)
    }

    @Test
    fun `when user profile fails to load shows Error state`() = runTest {
        // Setup: user profile fails
        fakeUserRepository.profileResult = Result.Error(AppError.NetworkError("Network error"))

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is TeacherDashboardUiState.Error)
        assertEquals("Network error", (state as TeacherDashboardUiState.Error).message)
    }

    @Test
    fun `when dashboard data fails to load shows Error state`() = runTest {
        // Setup: user profile succeeds
        fakeUserRepository.profileResult = Result.Success(
            User(1, "uid123", "teacher@afora.edu", "Jane", "Smith", "TEACHER")
        )
        
        // Setup: dashboard data fails
        fakeDashboardRepository.teacherDashboardResult = Result.Error(
            AppError.NetworkError("Server error")
        )

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is TeacherDashboardUiState.Error)
        assertEquals("Server error", (state as TeacherDashboardUiState.Error).message)
    }

    @Test
    fun `when dashboard has no classes today shows empty schedule`() = runTest {
        // Setup: user profile succeeds
        fakeUserRepository.profileResult = Result.Success(
            User(1, "uid123", "teacher@afora.edu", "John", "Doe", "TEACHER")
        )
        
        // Setup: dashboard with no classes
        fakeDashboardRepository.teacherDashboardResult = Result.Success(
            TeacherDashboardStats(
                todaySchedule = emptyList(),
                pendingLeaveRequests = 0,
                pendingQueries = 0
            )
        )

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is TeacherDashboardUiState.Success)
        
        val successState = state as TeacherDashboardUiState.Success
        assertEquals(0, successState.data.todaySchedule.size)
        assertEquals(0, successState.data.pendingLeaveRequests)
        assertEquals(0, successState.data.pendingQueries)
    }

    @Test
    fun `when dashboard has multiple classes they are all included`() = runTest {
        // Setup: user profile succeeds
        fakeUserRepository.profileResult = Result.Success(
            User(1, "uid123", "teacher@afora.edu", "Jane", "Doe", "TEACHER")
        )
        
        // Setup: dashboard with multiple classes
        val class1 = LectureSession(
            id = 1,
            subjectId = 101,
            classSectionId = 10,
            teacherId = 1,
            semesterId = 5,
            date = "2026-09-17",
            startTime = "09:00:00",
            endTime = "10:30:00",
            roomNumber = "A-101",
            status = SessionStatus.SCHEDULED
        )
        
        val class2 = LectureSession(
            id = 2,
            subjectId = 102,
            classSectionId = 11,
            teacherId = 1,
            semesterId = 5,
            date = "2026-09-17",
            startTime = "11:00:00",
            endTime = "12:30:00",
            roomNumber = "B-202",
            status = SessionStatus.SCHEDULED
        )
        
        fakeDashboardRepository.teacherDashboardResult = Result.Success(
            TeacherDashboardStats(
                todaySchedule = listOf(class1, class2),
                pendingLeaveRequests = 10,
                pendingQueries = 5
            )
        )

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is TeacherDashboardUiState.Success)
        
        val successState = state as TeacherDashboardUiState.Success
        assertEquals(2, successState.data.todaySchedule.size)
        assertEquals("A-101", successState.data.todaySchedule[0].roomNumber)
        assertEquals("B-202", successState.data.todaySchedule[1].roomNumber)
    }

    @Test
    fun `retry loads dashboard data again`() = runTest {
        // Setup: first load fails
        fakeUserRepository.profileResult = Result.Error(AppError.NetworkError("Network error"))

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is TeacherDashboardUiState.Error)

        // Fix the error and retry
        fakeUserRepository.profileResult = Result.Success(
            User(1, "uid123", "teacher@afora.edu", "Jane", "Smith", "TEACHER")
        )
        fakeDashboardRepository.teacherDashboardResult = Result.Success(
            TeacherDashboardStats(
                todaySchedule = emptyList(),
                pendingLeaveRequests = 2,
                pendingQueries = 1
            )
        )

        viewModel.load()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is TeacherDashboardUiState.Success)
        assertEquals(2, (state as TeacherDashboardUiState.Success).data.pendingLeaveRequests)
    }
}
