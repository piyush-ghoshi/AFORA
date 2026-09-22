package com.academia.android.ui.auth

import com.academia.android.ui.auth.login.LoginViewModel
import com.academia.shared.data.repository.AuthRepository
import com.academia.shared.data.repository.AuthResult
import com.academia.shared.domain.model.User
import com.academia.shared.util.AppError
import com.academia.shared.util.Result
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
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeAuthRepository = FakeAuthRepository()
    private val fakeUserRepository  = FakeUserRepository()
    private val fakeUserPreferences = FakeUserPreferences()
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = LoginViewModel(fakeAuthRepository, fakeUserRepository, fakeUserPreferences)
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
        viewModel.onPasswordChange("12345")

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
        val user = User(1, "uid123", "test@example.com", "John", "Doe", "STUDENT")
        fakeAuthRepository.loginResult = Result.Success(
            AuthResult(user = user, idToken = "token123", expiresIn = 3600L)
        )
        // Backend sync returns the same user
        fakeUserRepository.syncResult = Result.Success(user)

        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("123456")
        viewModel.onLoginClick()
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertTrue(uiState is AuthUiState.Success)
        assertEquals("test@example.com", (uiState as AuthUiState.Success).user.email)
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
        val user = User(1, "google_uid", "google@example.com", "Google", "User", "STUDENT")
        fakeAuthRepository.googleSignInResult = Result.Success(
            AuthResult(user = user, idToken = "google_token", expiresIn = 3600L)
        )
        fakeUserRepository.syncResult = Result.Success(user)

        viewModel.onGoogleSignIn("google_id_token")
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertTrue(uiState is AuthUiState.Success)
        assertEquals("google@example.com", (uiState as AuthUiState.Success).user.email)
    }
}

// ── Shared fakes ──────────────────────────────────────────────────────────────

/**
 * Fake AuthRepository shared by all auth-related tests.
 */
class FakeAuthRepository : AuthRepository {
    var isAuthenticatedResult = false
    var loginResult: Result<AuthResult>      = Result.Error(AppError.AuthenticationError("Not set"))
    var registerResult: Result<AuthResult>   = Result.Error(AppError.AuthenticationError("Not set"))
    var googleSignInResult: Result<AuthResult> = Result.Error(AppError.AuthenticationError("Not set"))
    var currentUserResult: Result<com.academia.shared.domain.model.User> =
        Result.Error(AppError.AuthenticationError("Not authenticated"))

    override suspend fun login(email: String, password: String) = loginResult
    override suspend fun register(email: String, password: String, firstName: String, lastName: String, role: String) = registerResult
    override suspend fun signInWithGoogle(idToken: String) = googleSignInResult
    override suspend fun logout(): Result<Unit> { isAuthenticatedResult = false; return Result.Success(Unit) }
    override suspend fun getCurrentUser() = currentUserResult
    override suspend fun getIdToken(forceRefresh: Boolean) = Result.Success("fake-token")
    override suspend fun isAuthenticated() = isAuthenticatedResult
    override suspend fun sendPasswordResetEmail(email: String) = Result.Success(Unit)
    override suspend fun updatePassword(currentPassword: String, newPassword: String) = Result.Success(Unit)
    override suspend fun reauthenticate(password: String) = Result.Success(Unit)
}
