package com.afora.android.ui.dashboard.student

import com.afora.android.ui.auth.FakeAuthRepository
import com.afora.android.ui.auth.FakeUserRepository
import com.afora.android.ui.dashboard.FakeDashboardRepository
import com.afora.android.ui.dashboard.StudentDashboardUiState
import com.afora.shared.data.repository.StudentDashboardStats
import com.afora.shared.domain.model.TimetableSlot
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
class StudentDashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeAuthRepository = FakeAuthRepository()
    private val fakeUserRepository = FakeUserRepository()
    private val fakeDashboardRepository = FakeDashboardRepository()
    private lateinit var viewModel: StudentDashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = StudentDashboardViewModel(
        fakeAuthRepository,
        fakeUserRepository,
        fakeDashboardRepository
    )

    @Test
    fun `initial state is Loading`() {
        viewModel = buildViewModel()
        
        assertTrue(viewModel.uiState.value is StudentDashboardUiState.Loading)
    }

    @Test
    fun `when user profile loads successfully and dashboard data loads shows Success state`() = runTest {
        // Setup: user profile succeeds
        fakeUserRepository.profileResult = Result.Success(
            User(1, "uid123", "student@afora.edu", "John", "Doe", "STUDENT")
        )
        
        // Setup: dashboard data succeeds
        fakeDashboardRepository.studentDashboardResult = Result.Success(
            StudentDashboardStats(
                attendancePercentage = 87.5,
                presentCount = 35,
                totalCount = 40,
                upcomingClasses = emptyList(),
                pendingLeaveRequests = 1
            )
        )

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is StudentDashboardUiState.Success)
        
        val successState = state as StudentDashboardUiState.Success
        assertEquals("John", successState.data.user.firstName)
        assertEquals(87.5, successState.data.attendancePercentage, 0.01)
        assertEquals(35, successState.data.presentCount)
        assertEquals(40, successState.data.totalCount)
        assertEquals(1, successState.data.pendingLeaveRequests)
    }

    @Test
    fun `when user profile fails to load shows Error state`() = runTest {
        // Setup: user profile fails
        fakeUserRepository.profileResult = Result.Error(AppError.NetworkError("Network error"))

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is StudentDashboardUiState.Error)
        assertEquals("Network error", (state as StudentDashboardUiState.Error).message)
    }

    @Test
    fun `when dashboard data fails to load shows Error state`() = runTest {
        // Setup: user profile succeeds
        fakeUserRepository.profileResult = Result.Success(
            User(1, "uid123", "student@afora.edu", "John", "Doe", "STUDENT")
        )
        
        // Setup: dashboard data fails
        fakeDashboardRepository.studentDashboardResult = Result.Error(
            AppError.ServerError("Server error")
        )

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is StudentDashboardUiState.Error)
        assertEquals("Server error", (state as StudentDashboardUiState.Error).message)
    }

    @Test
    fun `when dashboard has upcoming classes they are included in Success state`() = runTest {
        // Setup: user profile succeeds
        fakeUserRepository.profileResult = Result.Success(
            User(1, "uid123", "student@afora.edu", "Jane", "Smith", "STUDENT")
        )
        
        // Setup: dashboard with upcoming classes
        val upcomingClass = TimetableSlot(
            id = 1,
            classSectionId = 10,
            subjectId = 101,
            subjectCode = "CS101",
            subjectName = "Data Structures",
            teacherId = 20,
            teacherName = "Dr. Smith",
            dayOfWeek = 1,
            startTime = "09:00",
            endTime = "10:30",
            roomNumber = "A-101"
        )
        
        fakeDashboardRepository.studentDashboardResult = Result.Success(
            StudentDashboardStats(
                attendancePercentage = 75.0,
                presentCount = 30,
                totalCount = 40,
                upcomingClasses = listOf(upcomingClass),
                pendingLeaveRequests = 0
            )
        )

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is StudentDashboardUiState.Success)
        
        val successState = state as StudentDashboardUiState.Success
        assertEquals(1, successState.data.upcomingClasses.size)
        assertEquals("Data Structures", successState.data.upcomingClasses[0].subjectName)
        assertEquals("Dr. Smith", successState.data.upcomingClasses[0].teacherName)
    }

    @Test
    fun `retry loads dashboard data again`() = runTest {
        // Setup: first load fails
        fakeUserRepository.profileResult = Result.Error(AppError.NetworkError("Network error"))

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is StudentDashboardUiState.Error)

        // Fix the error and retry
        fakeUserRepository.profileResult = Result.Success(
            User(1, "uid123", "student@afora.edu", "John", "Doe", "STUDENT")
        )
        fakeDashboardRepository.studentDashboardResult = Result.Success(
            StudentDashboardStats(
                attendancePercentage = 90.0,
                presentCount = 36,
                totalCount = 40,
                upcomingClasses = emptyList(),
                pendingLeaveRequests = 0
            )
        )

        viewModel.load()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is StudentDashboardUiState.Success)
        assertEquals(90.0, (state as StudentDashboardUiState.Success).data.attendancePercentage, 0.01)
    }
}
