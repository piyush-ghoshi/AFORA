package com.academia.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.academia.android.ui.dashboard.profile.ProfileScreen
import com.academia.android.ui.dashboard.profile.ProfileViewModel
import com.academia.android.ui.dashboard.student.StudentDashboardScreen
import com.academia.android.ui.dashboard.student.StudentDashboardViewModel
import com.academia.android.ui.dashboard.teacher.TeacherDashboardScreen
import com.academia.android.ui.dashboard.teacher.TeacherDashboardViewModel

/** Root graph route for the main app (post-auth). */
const val MAIN_GRAPH_ROUTE = "main"

/**
 * Main app navigation routes.
 */
sealed class MainRoute(val route: String) {
    data object StudentDashboard : MainRoute("student_dashboard")
    data object TeacherDashboard : MainRoute("teacher_dashboard")
    data object Profile : MainRoute("profile")
}

/**
 * Main app navigation graph.
 *
 * Entry is determined by role:
 *  - STUDENT  → StudentDashboard
 *  - TEACHER  → TeacherDashboard
 *
 * Both roles can navigate to Profile.
 */
fun NavGraphBuilder.mainNavGraph(
    navController: NavHostController,
    startRoute: String,
    onLogout: () -> Unit
) {
    navigation(
        startDestination = startRoute,
        route = MAIN_GRAPH_ROUTE
    ) {

        // ── Student Dashboard ─────────────────────────────────────────────────
        composable(MainRoute.StudentDashboard.route) {
            val viewModel: StudentDashboardViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            StudentDashboardScreen(
                uiState = uiState,
                onNavigateToProfile = {
                    navController.navigate(MainRoute.Profile.route)
                },
                onLogout = onLogout,
                onRetry = viewModel::load
            )
        }

        // ── Teacher Dashboard ─────────────────────────────────────────────────
        composable(MainRoute.TeacherDashboard.route) {
            val viewModel: TeacherDashboardViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            TeacherDashboardScreen(
                uiState = uiState,
                onNavigateToProfile = {
                    navController.navigate(MainRoute.Profile.route)
                },
                onLogout = onLogout,
                onRetry = viewModel::load
            )
        }

        // ── Profile (shared) ──────────────────────────────────────────────────
        composable(MainRoute.Profile.route) {
            val viewModel: ProfileViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            ProfileScreen(
                uiState = uiState,
                onSave = viewModel::onSave,
                onFirstNameChange = viewModel::onFirstNameChange,
                onLastNameChange = viewModel::onLastNameChange,
                onBackClick = { navController.popBackStack() },
                onLogout = onLogout
            )
        }
    }
}
