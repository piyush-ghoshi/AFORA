package com.academia.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.academia.android.navigation.AUTH_GRAPH_ROUTE
import com.academia.android.navigation.MAIN_GRAPH_ROUTE
import com.academia.android.navigation.MainRoute
import com.academia.android.navigation.authNavGraph
import com.academia.android.navigation.mainNavGraph
import com.academia.android.ui.theme.AcademiaTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for AFORA Android app.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AcademiaTheme {
                AforaApp()
            }
        }
    }
}

/**
 * Root composable.
 *
 * Delegates start-destination resolution to [AppViewModel]:
 *  - Not authenticated → auth graph (Welcome screen)
 *  - STUDENT           → main graph → StudentDashboard
 *  - TEACHER           → main graph → TeacherDashboard
 */
@Composable
fun AforaApp() {
    val navController = rememberNavController()
    val appViewModel: AppViewModel = hiltViewModel()
    val startDest by appViewModel.startDestination.collectAsState()

    // Wait until the start destination is resolved before rendering NavHost
    val resolvedStart = startDest ?: return

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(
            navController = navController,
            startDestination = resolvedStart
        ) {
            // Auth flow (Welcome → Login / Register)
            authNavGraph(
                navController = navController,
                onAuthSuccess = {
                    // Re-resolve role after successful login
                    appViewModel.resolveStartDestination()
                    // AppViewModel emits new startDestination which triggers recompose
                    // but we also need to navigate — collect via effect below
                }
            )

            // Main app flow (role-based dashboards + profile)
            mainNavGraph(
                navController = navController,
                startRoute = when (resolvedStart) {
                    MAIN_GRAPH_ROUTE -> MainRoute.TeacherDashboard.route  // fallback
                    else -> resolvedStart
                },
                onLogout = {
                    appViewModel.logout()
                    navController.navigate(AUTH_GRAPH_ROUTE) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // After auth success, navigate to the correct main destination
        LaunchedEffect(startDest) {
            val dest = startDest ?: return@LaunchedEffect
            if (dest == MainRoute.StudentDashboard.route ||
                dest == MainRoute.TeacherDashboard.route
            ) {
                navController.navigate(MAIN_GRAPH_ROUTE) {
                    popUpTo(AUTH_GRAPH_ROUTE) { inclusive = true }
                }
            }
        }
    }
}
