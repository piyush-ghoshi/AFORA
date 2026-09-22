package com.afora.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.afora.android.ui.auth.AuthUiEvent
import com.afora.android.ui.auth.login.LoginScreen
import com.afora.android.ui.auth.login.LoginViewModel
import com.afora.android.ui.auth.register.RegisterScreen
import com.afora.android.ui.auth.register.RegisterViewModel
import com.afora.android.ui.auth.welcome.WelcomeScreen
import kotlinx.coroutines.flow.collectLatest

/**
 * Authentication navigation routes.
 */
sealed class AuthRoute(val route: String) {
    data object Welcome : AuthRoute("welcome")
    data object Login : AuthRoute("login")
    data object Register : AuthRoute("register")
}

/** Root graph route used as the NavHost startDestination. */
const val AUTH_GRAPH_ROUTE = "auth"

/**
 * Authentication navigation graph.
 *
 * Flow: Welcome → Login ↔ Register → (onAuthSuccess)
 */
fun NavGraphBuilder.authNavGraph(
    navController: NavHostController,
    onAuthSuccess: () -> Unit
) {
    navigation(
        startDestination = AuthRoute.Welcome.route,
        route = AUTH_GRAPH_ROUTE
    ) {

        // ── Welcome ──────────────────────────────────────────────────────────
        composable(AuthRoute.Welcome.route) {
            WelcomeScreen(
                onGetStarted = {
                    navController.navigate(AuthRoute.Register.route)
                },
                onSignIn = {
                    navController.navigate(AuthRoute.Login.route)
                }
            )
        }

        // ── Login ─────────────────────────────────────────────────────────────
        composable(AuthRoute.Login.route) {
            val viewModel: LoginViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            val formState by viewModel.formState.collectAsState()

            // Collect one-time navigation events
            LaunchedEffect(Unit) {
                viewModel.events.collectLatest { event ->
                    when (event) {
                        is AuthUiEvent.NavigateToHome -> onAuthSuccess()
                        else -> Unit
                    }
                }
            }

            LoginScreen(
                uiState = uiState,
                formState = formState,
                onEmailChange = viewModel::onEmailChange,
                onPasswordChange = viewModel::onPasswordChange,
                onRememberMeChange = viewModel::onRememberMeChange,
                onLoginClick = viewModel::onLoginClick,
                onGoogleSignInClick = viewModel::onGoogleSignIn,
                onForgotPasswordClick = { /* Phase A3 */ },
                onCreateAccountClick = {
                    navController.navigate(AuthRoute.Register.route)
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // ── Register ──────────────────────────────────────────────────────────
        composable(AuthRoute.Register.route) {
            val viewModel: RegisterViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            val formState by viewModel.formState.collectAsState()

            // Collect one-time navigation events
            LaunchedEffect(Unit) {
                viewModel.events.collectLatest { event ->
                    when (event) {
                        is AuthUiEvent.NavigateToHome -> onAuthSuccess()
                        else -> Unit
                    }
                }
            }

            RegisterScreen(
                uiState = uiState,
                formState = formState,
                onRoleChange = viewModel::onRoleChange,
                onFirstNameChange = viewModel::onFirstNameChange,
                onLastNameChange = viewModel::onLastNameChange,
                onEmailChange = viewModel::onEmailChange,
                onPasswordChange = viewModel::onPasswordChange,
                onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
                onRegisterClick = viewModel::onRegisterClick,
                onGoogleSignInClick = viewModel::onGoogleSignIn,
                onSignInClick = {
                    navController.navigate(AuthRoute.Login.route) {
                        popUpTo(AuthRoute.Register.route) { inclusive = true }
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
