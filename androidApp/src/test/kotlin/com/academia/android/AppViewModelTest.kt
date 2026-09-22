package com.academia.android

import com.academia.android.navigation.AUTH_GRAPH_ROUTE
import com.academia.android.navigation.MAIN_GRAPH_ROUTE
import com.academia.android.ui.auth.FakeAuthRepository
import com.academia.shared.domain.model.User
import com.academia.shared.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeAuthRepository = FakeAuthRepository()
    private lateinit var viewModel: AppViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when unauthenticated startDestination is AUTH_GRAPH_ROUTE`() = runTest {
        fakeAuthRepository.isAuthenticatedResult = false

        viewModel = AppViewModel(fakeAuthRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AUTH_GRAPH_ROUTE, viewModel.startDestination.value)
    }

    @Test
    fun `when authenticated as STUDENT startDestination is MAIN_GRAPH_ROUTE and role is STUDENT`() = runTest {
        fakeAuthRepository.isAuthenticatedResult = true
        fakeAuthRepository.currentUserResult = Result.Success(
            User(1, "uid123", "student@afora.edu", "Student", "User", "STUDENT")
        )

        viewModel = AppViewModel(fakeAuthRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(MAIN_GRAPH_ROUTE, viewModel.startDestination.value)
        assertEquals("STUDENT", viewModel.userRole.value)
    }

    @Test
    fun `when authenticated as TEACHER startDestination is MAIN_GRAPH_ROUTE and role is TEACHER`() = runTest {
        fakeAuthRepository.isAuthenticatedResult = true
        fakeAuthRepository.currentUserResult = Result.Success(
            User(2, "uid456", "teacher@afora.edu", "Teacher", "User", "TEACHER")
        )

        viewModel = AppViewModel(fakeAuthRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(MAIN_GRAPH_ROUTE, viewModel.startDestination.value)
        assertEquals("TEACHER", viewModel.userRole.value)
    }

    @Test
    fun `logout sets startDestination to AUTH_GRAPH_ROUTE`() = runTest {
        fakeAuthRepository.isAuthenticatedResult = true
        fakeAuthRepository.currentUserResult = Result.Success(
            User(1, "uid123", "student@afora.edu", "Student", "User", "STUDENT")
        )

        viewModel = AppViewModel(fakeAuthRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.logout()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AUTH_GRAPH_ROUTE, viewModel.startDestination.value)
    }
}
