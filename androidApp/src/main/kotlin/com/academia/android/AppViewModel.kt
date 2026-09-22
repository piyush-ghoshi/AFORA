package com.academia.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.academia.android.navigation.AUTH_GRAPH_ROUTE
import com.academia.android.navigation.MAIN_GRAPH_ROUTE
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
 * Resolves the initial navigation graph route based on auth state:
 *  - null             → resolving
 *  - AUTH_GRAPH_ROUTE → user is not logged in
 *  - MAIN_GRAPH_ROUTE → user is logged in
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    private val _userRole = MutableStateFlow<String?>("STUDENT")
    val userRole: StateFlow<String?> = _userRole.asStateFlow()

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
                    _userRole.value = result.data.role.uppercase()
                    _startDestination.value = MAIN_GRAPH_ROUTE
                }
                is Result.Error -> {
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
