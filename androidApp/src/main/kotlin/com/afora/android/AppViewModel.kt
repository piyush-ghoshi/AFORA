package com.afora.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afora.android.data.UserPreferencesStore
import com.afora.android.navigation.AUTH_GRAPH_ROUTE
import com.afora.android.navigation.MAIN_GRAPH_ROUTE
import com.afora.shared.data.repository.AuthRepository
import com.afora.shared.data.repository.UserRepository
import com.afora.shared.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * App-level ViewModel — resolves the initial navigation destination.
 *
 * Role resolution order (most → least authoritative):
 *  1. DataStore cache  (written by LoginViewModel/RegisterViewModel after backend sync)
 *  2. Backend GET /api/users/me (live fetch if DataStore is empty)
 *  3. Firebase custom claim (last resort if backend is unreachable)
 *
 * Result:
 *  null             → still resolving (NavHost not rendered yet)
 *  AUTH_GRAPH_ROUTE → not authenticated → Welcome screen
 *  MAIN_GRAPH_ROUTE → authenticated → role-based dashboard
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val userPreferences: UserPreferencesStore
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    // Resolved from DataStore → used by MainActivity to pick correct dashboard
    private val _userRole = MutableStateFlow<String?>(null)
    val userRole: StateFlow<String?> = _userRole.asStateFlow()

    init {
        resolveStartDestination()
    }

    fun resolveStartDestination() {
        viewModelScope.launch {
            // Not signed in at all → go to auth
            if (!authRepository.isAuthenticated()) {
                userPreferences.clear()
                _startDestination.value = AUTH_GRAPH_ROUTE
                return@launch
            }

            // ── Step 1: DataStore cache (fastest, no network) ─────────────────
            val cachedRole = userPreferences.getRole()
            if (cachedRole != null) {
                _userRole.value = cachedRole
                _startDestination.value = MAIN_GRAPH_ROUTE
                return@launch
            }

            // ── Step 2: Backend GET /api/users/me (authoritative role from DB) ─
            val backendResult = userRepository.getMyProfile()
            if (backendResult is Result.Success) {
                val role = backendResult.data.role
                _userRole.value = role

                // Populate DataStore so next cold start is instant
                userPreferences.saveUser(
                    firebaseUid = backendResult.data.firebaseUid,
                    email       = backendResult.data.email,
                    displayName = backendResult.data.fullName,
                    role        = role
                )

                _startDestination.value = MAIN_GRAPH_ROUTE
                return@launch
            }

            // ── Step 3: Firebase custom claim fallback ────────────────────────
            val firebaseResult = authRepository.getCurrentUser()
            if (firebaseResult is Result.Success) {
                val role = firebaseResult.data.role.uppercase().ifBlank { "STUDENT" }
                _userRole.value = role
                _startDestination.value = MAIN_GRAPH_ROUTE
                return@launch
            }

            // All methods failed — session is invalid, send to auth
            _startDestination.value = AUTH_GRAPH_ROUTE
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            userPreferences.clear()
            _userRole.value = null
            _startDestination.value = AUTH_GRAPH_ROUTE
        }
    }
}
