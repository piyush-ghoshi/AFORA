package com.academia.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.academia.android.navigation.AUTH_GRAPH_ROUTE
import com.academia.android.navigation.authNavGraph
import com.academia.android.ui.theme.AcademiaTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for AFORA Android app.
 * Entry point for the Compose UI with navigation.
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
 * Root composable for AFORA app.
 * Manages top-level navigation between auth and main app flows.
 */
@Composable
fun AforaApp() {
    val navController = rememberNavController()
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(
            navController = navController,
            startDestination = AUTH_GRAPH_ROUTE
        ) {
            // Authentication flow
            authNavGraph(
                navController = navController,
                onAuthSuccess = {
                    // Navigate to home screen after successful authentication
                    navController.navigate("home") {
                        // Clear auth backstack
                        popUpTo(AUTH_GRAPH_ROUTE) {
                            inclusive = true
                        }
                    }
                }
            )
            
            // Home screen (temporary placeholder for Phase A2)
            composable("home") {
                HomeScreen()
            }
        }
    }
}

/**
 * Home screen placeholder for Phase A2.
 * Shown after successful authentication.
 * Will be replaced with actual dashboard in Phase A3.
 */
@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "✓ Authentication Successful",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Phase A2: Authentication UI Complete",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "✓ Welcome Screen\n✓ Login Screen\n✓ Registration Screen\n✓ Firebase Integration\n✓ Form Validation\n✓ Navigation Flow",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Next: Phase A3 - User Dashboard & Profile",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
