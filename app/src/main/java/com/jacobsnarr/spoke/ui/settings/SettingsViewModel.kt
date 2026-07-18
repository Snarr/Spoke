package com.jacobsnarr.spoke.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jacobsnarr.spoke.BuildConfig
import com.jacobsnarr.spoke.data.auth.AuthRepository
import com.jacobsnarr.spoke.data.system.BikeSystem
import com.jacobsnarr.spoke.data.system.SystemRepository
import com.jacobsnarr.spoke.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val system: BikeSystem,
    val isLoggedIn: Boolean,
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val appVersion: String = BuildConfig.VERSION_NAME,
) {
    val canSignIn: Boolean
        get() = system.auth != null

    val canSubmit: Boolean
        get() = email.isNotBlank() && password.isNotBlank() && !isLoading
}

class SettingsViewModel(private val authRepository: AuthRepository, private val systemRepository: SystemRepository) : ViewModel() {
    private val _uiState =
        MutableStateFlow(
            SettingsUiState(
                system = systemRepository.current,
                isLoggedIn = authRepository.isCurrentlyLoggedIn(),
            ),
        )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                systemRepository.currentSystem,
                authRepository.isLoggedIn,
            ) { system, loggedIn -> system to loggedIn }
                .collect { (system, loggedIn) ->
                    _uiState.update { it.copy(system = system, isLoggedIn = loggedIn) }
                }
        }
    }

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }

    fun login() {
        val state = _uiState.value
        if (!state.canSubmit) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            authRepository
                .login(systemRepository.current, state.email, state.password)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, email = "", password = "") }
                }.onFailure {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Login failed. Check your email and password.",
                        )
                    }
                }
        }
    }

    fun logout() {
        authRepository.logout()
        _uiState.update { it.copy(email = "", password = "", error = null) }
    }

    companion object {
        fun provideFactory(container: AppContainer) = viewModelFactory {
            initializer { SettingsViewModel(container.authRepository, container.systemRepository) }
        }
    }
}
