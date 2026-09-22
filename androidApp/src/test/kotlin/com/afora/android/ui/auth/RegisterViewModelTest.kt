package com.afora.android.ui.auth

import com.afora.android.ui.auth.register.RegisterViewModel
import com.afora.shared.data.repository.AuthResult
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    private val testDispatcher      = StandardTestDispatcher()
    private val fakeAuthRepository  = FakeAuthRepository()
    private val fakeUserRepository  = FakeUserRepository()
    private val fakeUserPreferences = FakeUserPreferences()
    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = RegisterViewModel(fakeAuthRepository, fakeUserRepository, fakeUserPreferences)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial form state defaults to STUDENT role and is invalid`() {
        val formState = viewModel.formState.value
        assertEquals(UserRole.STUDENT, formState.role)
        assertFalse(formState.validation.isValid)
    }

    @Test
    fun `changing role updates role in form state`() {
        viewModel.onRoleChange(UserRole.TEACHER)
        assertEquals(UserRole.TEACHER, viewModel.formState.value.role)
    }

    @Test
    fun `weak password shows password validation error`() {
        viewModel.onFirstNameChange("Jane")
        viewModel.onLastNameChange("Doe")
        viewModel.onEmailChange("jane.doe@example.com")
        viewModel.onPasswordChange("weak")
        viewModel.onConfirmPasswordChange("weak")

        assertNotNull(viewModel.formState.value.validation.passwordError)
        assertFalse(viewModel.formState.value.validation.isValid)
    }

    @Test
    fun `valid registration details enables form submission`() {
        viewModel.onFirstNameChange("Jane")
        viewModel.onLastNameChange("Doe")
        viewModel.onEmailChange("jane.doe@example.com")
        viewModel.onPasswordChange("Pass1234")
        viewModel.onConfirmPasswordChange("Pass1234")

        val v = viewModel.formState.value.validation
        assertNull(v.nameError)
        assertNull(v.emailError)
        assertNull(v.passwordError)
        assertTrue(v.isValid)
    }

    @Test
    fun `onRegisterClick success sets uiState to Success`() = runTest {
        fakeAuthRepository.registerResult = Result.Success(
            AuthResult(
                user      = User(1, "uid_jane", "jane.doe@example.com", "Jane", "Doe", "STUDENT"),
                idToken   = "token_jane",
                expiresIn = 3600L
            )
        )

        viewModel.onFirstNameChange("Jane")
        viewModel.onLastNameChange("Doe")
        viewModel.onEmailChange("jane.doe@example.com")
        viewModel.onPasswordChange("Pass1234")
        viewModel.onConfirmPasswordChange("Pass1234")

        viewModel.onRegisterClick()
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertTrue(uiState is AuthUiState.Success)
        val user = (uiState as AuthUiState.Success).user
        assertEquals("Jane", user.firstName)
        assertEquals("Doe", user.lastName)
    }

    @Test
    fun `onRegisterClick failure sets uiState to Error`() = runTest {
        fakeAuthRepository.registerResult = Result.Error(
            AppError.ValidationError(listOf("Email already in use"))
        )

        viewModel.onFirstNameChange("Jane")
        viewModel.onLastNameChange("Doe")
        viewModel.onEmailChange("jane.doe@example.com")
        viewModel.onPasswordChange("Pass1234")
        viewModel.onConfirmPasswordChange("Pass1234")

        viewModel.onRegisterClick()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AuthUiState.Error)
    }

    @Test
    fun `onGoogleSignIn success sets uiState to Success`() = runTest {
        fakeAuthRepository.googleSignInResult = Result.Success(
            AuthResult(
                user      = User(2, "google_reg_uid", "greg@example.com", "G", "Reg", "STUDENT"),
                idToken   = "google_reg_token",
                expiresIn = 3600L
            )
        )

        viewModel.onGoogleSignIn("google_id_token")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AuthUiState.Success)
    }
}
