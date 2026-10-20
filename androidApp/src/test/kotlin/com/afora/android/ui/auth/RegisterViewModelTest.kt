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
        viewModel.onIdNumberChange("CS-2023-048")
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
        viewModel.onIdNumberChange("CS-2023-048")
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
        viewModel.onIdNumberChange("CS-2023-048")
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
        viewModel.onIdNumberChange("CS-2023-048")
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

        viewModel.onIdNumberChange("CS-2023-048")
        viewModel.onGoogleSignIn("google_id_token")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AuthUiState.Success)
    }
    private fun fillValidForm() {
        viewModel.onFirstNameChange("Jane")
        viewModel.onLastNameChange("Doe")
        viewModel.onIdNumberChange("CS-2023-048")
        viewModel.onEmailChange("jane.doe@example.com")
        viewModel.onPasswordChange("Pass1234")
        viewModel.onConfirmPasswordChange("Pass1234")
    }

    private fun firebaseAccount() = Result.Success(
        AuthResult(
            user      = User(1, "uid_jane", "jane.doe@example.com", "Jane", "Doe", "STUDENT"),
            idToken   = "token_jane",
            expiresIn = 3600L
        )
    )

    @Test
    fun `a missing roll number blocks submission`() {
        viewModel.onFirstNameChange("Jane")
        viewModel.onLastNameChange("Doe")
        viewModel.onEmailChange("jane.doe@example.com")
        viewModel.onPasswordChange("Pass1234")
        viewModel.onConfirmPasswordChange("Pass1234")

        val v = viewModel.formState.value.validation
        assertEquals("Roll Number is required", v.idNumberError)
        assertFalse(v.isValid)
    }

    @Test
    fun `the id label follows the chosen role`() {
        viewModel.onRoleChange(UserRole.TEACHER)
        assertEquals("Employee ID is required", viewModel.formState.value.validation.idNumberError)
    }

    @Test
    fun `the id number is sent to the backend trimmed with the chosen role`() = runTest {
        fakeAuthRepository.registerResult = firebaseAccount()
        viewModel.onRoleChange(UserRole.TEACHER)
        fillValidForm()
        viewModel.onIdNumberChange("  FAC-104  ")

        viewModel.onRegisterClick()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("FAC-104", fakeUserRepository.lastSyncIdNumber)
        assertEquals("TEACHER", fakeUserRepository.lastSyncRole)
    }

    @Test
    fun `a failed backend registration shows the server message and removes the new account`() = runTest {
        fakeAuthRepository.registerResult = firebaseAccount()
        fakeUserRepository.syncResult = Result.Error(AppError.ApiError(409, "Roll number CS-2023-048 is already registered"))
        fillValidForm()

        viewModel.onRegisterClick()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Error)
        assertEquals("Roll number CS-2023-048 is already registered", (state as AuthUiState.Error).message)
        assertEquals(1, fakeAuthRepository.deleteCalls)
        assertNull("nothing may be cached for an account that was rolled back", fakeUserPreferences.getRole())
    }

    @Test
    fun `if removing the account fails the user is at least signed out`() = runTest {
        fakeAuthRepository.registerResult = firebaseAccount()
        fakeAuthRepository.deleteResult = Result.Error(AppError.UnknownError("boom"))
        fakeUserRepository.syncResult = Result.Error(AppError.NetworkError("offline"))
        fillValidForm()

        viewModel.onRegisterClick()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AuthUiState.Error)
        assertEquals(1, fakeAuthRepository.logoutCalls)
    }

    @Test
    fun `a backend failure no longer falls back to the role typed into the form`() = runTest {
        fakeAuthRepository.registerResult = firebaseAccount()
        fakeUserRepository.syncResult = Result.Error(AppError.NetworkError("offline"))
        fillValidForm()

        viewModel.onRegisterClick()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value is AuthUiState.Success)
    }

    @Test
    fun `google sign-up without a roll number is refused before touching firebase`() = runTest {
        viewModel.onGoogleSignIn("google_id_token")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AuthUiState.Error)
        assertEquals(0, fakeUserRepository.syncCalls)
    }

    @Test
    fun `a google sign-up whose backend registration fails is signed out but not deleted`() = runTest {
        fakeAuthRepository.googleSignInResult = Result.Success(
            AuthResult(
                user      = User(2, "google_reg_uid", "greg@example.com", "G", "Reg", "STUDENT"),
                idToken   = "google_reg_token",
                expiresIn = 3600L
            )
        )
        fakeUserRepository.syncResult = Result.Error(AppError.ApiError(409, "Roll number taken"))
        viewModel.onIdNumberChange("CS-2023-048")

        viewModel.onGoogleSignIn("google_id_token")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AuthUiState.Error)
        assertEquals(0, fakeAuthRepository.deleteCalls)
        assertEquals(1, fakeAuthRepository.logoutCalls)
    }
    // -- an email that already exists in Firebase without a backend profile ------------

    private fun emailAlreadyInUse() = Result.Error(
        AppError.ValidationError(listOf(com.afora.shared.data.repository.AuthMessages.EMAIL_ALREADY_IN_USE))
    )

    @Test
    fun `an existing firebase account with the right password is completed instead of rejected`() = runTest {
        fakeAuthRepository.registerResult = emailAlreadyInUse()
        fakeAuthRepository.loginResult = firebaseAccount()
        fillValidForm()

        viewModel.onRegisterClick()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AuthUiState.Success)
        assertEquals("CS-2023-048", fakeUserRepository.lastSyncIdNumber)
        assertEquals("an account that already existed must never be deleted", 0, fakeAuthRepository.deleteCalls)
    }

    @Test
    fun `an existing firebase account with the wrong password gets a clear message and no backend call`() = runTest {
        fakeAuthRepository.registerResult = emailAlreadyInUse()
        fakeAuthRepository.loginResult = Result.Error(AppError.AuthenticationError("Incorrect email or password"))
        fillValidForm()

        viewModel.onRegisterClick()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Error)
        assertTrue((state as AuthUiState.Error).message.contains("already exists"))
        assertEquals(0, fakeUserRepository.syncCalls)
        assertEquals(0, fakeAuthRepository.deleteCalls)
    }

    @Test
    fun `if completing an existing account fails it is signed out and never deleted`() = runTest {
        fakeAuthRepository.registerResult = emailAlreadyInUse()
        fakeAuthRepository.loginResult = firebaseAccount()
        fakeUserRepository.syncResult = Result.Error(AppError.ApiError(409, "Roll number CS-2023-048 is already registered"))
        fillValidForm()

        viewModel.onRegisterClick()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AuthUiState.Error)
        assertEquals(0, fakeAuthRepository.deleteCalls)
        assertEquals(1, fakeAuthRepository.logoutCalls)
    }

    @Test
    fun `other firebase errors are not mistaken for an existing account`() = runTest {
        fakeAuthRepository.registerResult = Result.Error(AppError.ValidationError(listOf("Password is too weak")))
        fakeAuthRepository.loginResult = firebaseAccount()   // must NOT be used
        fillValidForm()

        viewModel.onRegisterClick()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AuthUiState.Error)
        assertEquals(0, fakeUserRepository.syncCalls)
    }
}