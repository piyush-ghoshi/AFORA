package com.afora.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.afora.android.navigation.AUTH_GRAPH_ROUTE
import com.afora.android.navigation.MAIN_GRAPH_ROUTE
import com.afora.android.navigation.MainRoute
import com.afora.android.navigation.authNavGraph
import com.afora.android.navigation.mainNavGraph
import com.afora.android.ui.theme.AforaTheme
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
            AforaTheme {
                AforaApp()
            }
        }
    }
}

/**
 * Root composable.
 *
 * Delegates start-destination resolution to [AppViewModel]:
 *  - Not authenticated → AUTH_GRAPH_ROUTE ("auth")
 *  - Authenticated     → MAIN_GRAPH_ROUTE ("main")
 */
@Composable
fun AforaApp() {
    val navController = rememberNavController()
    val appViewModel: AppViewModel = hiltViewModel()
    val startDest by appViewModel.startDestination.collectAsState()
    val userRole by appViewModel.userRole.collectAsState()

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
                    appViewModel.resolveStartDestination()
                    navController.navigate(MAIN_GRAPH_ROUTE) {
                        popUpTo(AUTH_GRAPH_ROUTE) { inclusive = true }
                    }
                }
            )

            // Main app flow (role-based dashboards + profile)
            mainNavGraph(
                navController = navController,
                startRoute = when (userRole?.uppercase()) {
                    "TEACHER" -> MainRoute.TeacherDashboard.route
                    else -> MainRoute.StudentDashboard.route
                },
                onLogout = {
                    appViewModel.logout()
                    navController.navigate(AUTH_GRAPH_ROUTE) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
