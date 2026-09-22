package com.academia.android

import com.academia.android.navigation.AUTH_GRAPH_ROUTE
import com.academia.android.navigation.MAIN_GRAPH_ROUTE
import com.academia.android.ui.auth.FakeAuthRepository
import com.academia.android.ui.auth.FakeUserPreferences
import com.academia.android.ui.auth.FakeUserRepository
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

    private val testDispatcher      = StandardTestDispatcher()
    private val fakeAuthRepository  = FakeAuthRepository()
    private val fakeUserRepository  = FakeUserRepository()
    private val fakeUserPreferences = FakeUserPreferences()
    private lateinit var viewModel: AppViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = AppViewModel(
        fakeAuthRepository,
        fakeUserRepository,
        fakeUserPreferences
    )

    @Test
    fun `when unauthenticated startDestination is AUTH_GRAPH_ROUTE`() = runTest {
        fakeAuthRepository.isAuthenticatedResult = false

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AUTH_GRAPH_ROUTE, viewModel.startDestination.value)
    }

    @Test
    fun `when authenticated and DataStore has STUDENT role routes to MAIN_GRAPH_ROUTE`() = runTest {
        fakeAuthRepository.isAuthenticatedResult = true
        // Pre-populate DataStore cache (simulates a returning user)
        fakeUserPreferences.saveUser("uid123", "student@afora.edu", "Student User", "STUDENT")

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(MAIN_GRAPH_ROUTE, viewModel.startDestination.value)
        assertEquals("STUDENT", viewModel.userRole.value)
    }

    @Test
    fun `when authenticated and DataStore has TEACHER role routes to MAIN_GRAPH_ROUTE`() = runTest {
        fakeAuthRepository.isAuthenticatedResult = true
        fakeUserPreferences.saveUser("uid456", "teacher@afora.edu", "Teacher User", "TEACHER")

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(MAIN_GRAPH_ROUTE, viewModel.startDestination.value)
        assertEquals("TEACHER", viewModel.userRole.value)
    }

    @Test
    fun `when authenticated and DataStore empty falls back to backend profile`() = runTest {
        fakeAuthRepository.isAuthenticatedResult = true
        // DataStore is empty (no cached role)
        fakeUserRepository.profileResult = Result.Success(
            User(2, "uid456", "teacher@afora.edu", "Teacher", "User", "TEACHER")
        )

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(MAIN_GRAPH_ROUTE, viewModel.startDestination.value)
        assertEquals("TEACHER", viewModel.userRole.value)
    }

    @Test
    fun `logout clears DataStore and sets startDestination to AUTH_GRAPH_ROUTE`() = runTest {
        fakeAuthRepository.isAuthenticatedResult = true
        fakeUserPreferences.saveUser("uid123", "student@afora.edu", "Student User", "STUDENT")

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.logout()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AUTH_GRAPH_ROUTE, viewModel.startDestination.value)
        assertEquals(null, fakeUserPreferences.getRole())  // DataStore cleared
    }
}
