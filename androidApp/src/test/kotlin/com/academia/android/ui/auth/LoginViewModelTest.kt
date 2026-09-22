package com.academia.android.ui.auth

import com.academia.android.ui.auth.login.LoginViewModel
import com.academia.shared.data.repository.AuthRepository
import com.academia.shared.data.repository.AuthResult
import com.academia.shared.domain.model.User
import com.academia.shared.util.AppError
import com.academia.shared.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeAuthRepository = FakeAuthRepository()
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = LoginViewModel(fakeAuthRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial form state is invalid`() {
        val formState = viewModel.formState.value
        assertEquals("", formState.email)
        assertEquals("", formState.password)
        assertFalse(formState.rememberMe)
        assertFalse(formState.validation.isValid)
    }

    @Test
    fun `invalid email sets email error`() {
        viewModel.onEmailChange("invalid-email")

        val formState = viewModel.formState.value
        assertNotNull(formState.validation.emailError)
        assertFalse(formState.validation.isValid)
    }

    @Test
    fun `short password sets password error`() {
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("12345") // less than 6 chars

        val formState = viewModel.formState.value
        assertNull(formState.validation.emailError)
        assertNotNull(formState.validation.passwordError)
        assertFalse(formState.validation.isValid)
    }

    @Test
    fun `valid credentials enables form submission`() {
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("123456")

        val formState = viewModel.formState.value
        assertNull(formState.validation.emailError)
        assertNull(formState.validation.passwordError)
        assertTrue(formState.validation.isValid)
    }

    @Test
    fun `onLoginClick success updates uiState to Success`() = runTest {
        fakeAuthRepository.loginResult = Result.Success(
            AuthResult(
                user = User(1, "uid123", "test@example.com", "John", "Doe", "STUDENT"),
                idToken = "token123",
                expiresIn = 3600L
            )
        )

        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("123456")
        viewModel.onLoginClick()
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertTrue(uiState is AuthUiState.Success)
        val successUser = (uiState as AuthUiState.Success).user
        assertEquals("test@example.com", successUser.email)
    }

    @Test
    fun `onLoginClick failure updates uiState to Error`() = runTest {
        fakeAuthRepository.loginResult = Result.Error(
            AppError.AuthenticationError("Invalid credentials")
        )

        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("123456")
        viewModel.onLoginClick()
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertTrue(uiState is AuthUiState.Error)
        assertEquals("Invalid credentials", (uiState as AuthUiState.Error).message)
    }

    @Test
    fun `onGoogleSignIn success updates uiState to Success`() = runTest {
        fakeAuthRepository.googleSignInResult = Result.Success(
            AuthResult(
                user = User(1, "google_uid", "google@example.com", "Google", "User", "STUDENT"),
                idToken = "google_token",
                expiresIn = 3600L
            )
        )

        viewModel.onGoogleSignIn("google_id_token")
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertTrue(uiState is AuthUiState.Success)
        val successUser = (uiState as AuthUiState.Success).user
        assertEquals("google@example.com", successUser.email)
    }
}

/**
 * Fake AuthRepository implementation for testing.
 */
class FakeAuthRepository : AuthRepository {
    var isAuthenticatedResult = false
    var loginResult: Result<AuthResult> = Result.Error(AppError.AuthenticationError("Not set"))
    var registerResult: Result<AuthResult> = Result.Error(AppError.AuthenticationError("Not set"))
    var currentUserResult: Result<User> = Result.Error(AppError.AuthenticationError("Not authenticated"))

    var googleSignInResult: Result<AuthResult> = Result.Error(AppError.AuthenticationError("Not set"))

    override suspend fun login(email: String, password: String): Result<AuthResult> = loginResult

    override suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        role: String
    ): Result<AuthResult> = registerResult

    override suspend fun signInWithGoogle(idToken: String): Result<AuthResult> = googleSignInResult

    override suspend fun logout(): Result<Unit> {
        isAuthenticatedResult = false
        return Result.Success(Unit)
    }

    override suspend fun getCurrentUser(): Result<User> = currentUserResult

    override suspend fun getIdToken(forceRefresh: Boolean): Result<String> {
        return Result.Success("fake-token")
    }

    override suspend fun isAuthenticated(): Boolean = isAuthenticatedResult

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return Result.Success(Unit)
    }

    override suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return Result.Success(Unit)
    }

    override suspend fun reauthenticate(password: String): Result<Unit> {
        return Result.Success(Unit)
    }
}
