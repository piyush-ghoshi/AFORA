package com.academia.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.academia.android.navigation.AUTH_GRAPH_ROUTE
import com.academia.android.navigation.MainRoute
import com.academia.shared.data.repository.AuthRepository
import com.academia.shared.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * App-level ViewModel.
 *
 * Resolves the initial navigation destination based on the current auth state
 * and the authenticated user's role.
 *
 *  null            → resolving (show nothing / splash)
 *  AUTH_GRAPH_ROUTE → user is not logged in
 *  StudentDashboard.route → STUDENT role
 *  TeacherDashboard.route → TEACHER role
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    init {
        resolveStartDestination()
    }

    fun resolveStartDestination() {
        viewModelScope.launch {
            if (!authRepository.isAuthenticated()) {
                _startDestination.value = AUTH_GRAPH_ROUTE
                return@launch
            }

            when (val result = authRepository.getCurrentUser()) {
                is Result.Success -> {
                    _startDestination.value = when (result.data.role.uppercase()) {
                        "TEACHER" -> MainRoute.TeacherDashboard.route
                        "STUDENT" -> MainRoute.StudentDashboard.route
                        else      -> AUTH_GRAPH_ROUTE
                    }
                }
                is Result.Error -> {
                    // Token may be stale — send back to auth
                    _startDestination.value = AUTH_GRAPH_ROUTE
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _startDestination.value = AUTH_GRAPH_ROUTE
        }
    }
}
