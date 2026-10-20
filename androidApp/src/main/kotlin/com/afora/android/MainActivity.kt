package com.afora.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.afora.android.data.notification.AforaFirebaseMessagingService
import com.afora.android.navigation.AUTH_GRAPH_ROUTE
import com.afora.android.navigation.MAIN_GRAPH_ROUTE
import com.afora.android.navigation.MainRoute
import com.afora.android.navigation.authNavGraph
import com.afora.android.navigation.mainNavGraph
import com.afora.android.ui.theme.AforaTheme
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for AFORA Android app.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            Log.i("MainActivity", "Notification permission granted: $isGranted")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Fetch FCM token and sync with backend
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful && task.result != null) {
                val token = task.result
                Log.i("MainActivity", "Device FCM Token: $token")
                AforaFirebaseMessagingService.syncTokenWithBackend(token)
            } else {
                Log.w("MainActivity", "Fetching FCM registration token failed", task.exception)
            }
        }

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
