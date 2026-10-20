package com.afora.android.ui.dashboard.profile

import com.afora.android.ui.auth.FakeUserRepository
import com.afora.android.ui.dashboard.ProfileUiState
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
class ProfileViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val users = FakeUserRepository()

    private val student = User(
        id = 1, firebaseUid = "uid", email = "alex@univ.edu", firstName = "Alex", lastName = "Morgan",
        role = "STUDENT", studentId = 7, rollNumber = "CS-2023-048", department = "Unassigned", batch = "Unassigned"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        users.profileResult = Result.Success(student)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ProfileViewModel(users)

    @Test
    fun `loads the profile from the backend`() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as ProfileUiState.Ready
        assertEquals("Alex", state.firstName)
        assertEquals("CS-2023-048", state.user.rollNumber)
    }

    @Test
    fun `a load failure is shown`() = runTest {
        users.profileResult = Result.Error(AppError.NetworkError("offline"))
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("offline", (vm.uiState.value as ProfileUiState.Error).message)
    }

    @Test
    fun `save calls the backend and reports success only after it answers`() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        users.updateResult = Result.Success(student.copy(firstName = "Alexander"))

        vm.onFirstNameChange("Alexander")
        vm.onSave()
        assertTrue((vm.uiState.value as ProfileUiState.Ready).isSaving)
        assertEquals(false, (vm.uiState.value as ProfileUiState.Ready).saveSuccess)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as ProfileUiState.Ready
        assertEquals("Alexander" to "Morgan", users.lastUpdate)
        assertTrue(state.saveSuccess)
        assertEquals("Alexander", state.user.firstName)
    }

    @Test
    fun `a failed save shows the error and does not claim success`() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        users.updateResult = Result.Error(AppError.ApiError(500, "Internal server error"))

        vm.onLastNameChange("Smith")
        vm.onSave()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as ProfileUiState.Ready
        assertEquals(false, state.saveSuccess)
        assertEquals("Internal server error", state.errorMessage)
        assertEquals("Smith", state.lastName)   // the edit is kept so the user can retry
    }

    @Test
    fun `blank names are rejected without calling the backend`() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onFirstNameChange("  ")
        vm.onSave()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(users.lastUpdate)
        assertEquals("Name fields cannot be empty", (vm.uiState.value as ProfileUiState.Ready).errorMessage)
    }
}