package com.jacobsnarr.spoke.ui.myrides

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jacobsnarr.spoke.data.auth.AuthRepository
import com.jacobsnarr.spoke.data.prefs.PreferencesStore
import com.jacobsnarr.spoke.data.prefs.UnitSystem
import com.jacobsnarr.spoke.data.remote.dto.UserSummaryDto
import com.jacobsnarr.spoke.data.ride.MyRidesRepository
import com.jacobsnarr.spoke.data.system.SystemRepository
import com.jacobsnarr.spoke.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyRidesUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val summary: UserSummaryDto? = null,
    val isSupported: Boolean = true,
    val isLoggedIn: Boolean = false,
)

class MyRidesViewModel(
    private val myRidesRepository: MyRidesRepository,
    private val authRepository: AuthRepository,
    private val systemRepository: SystemRepository,
    private val preferencesStore: PreferencesStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyRidesUiState())
    val uiState: StateFlow<MyRidesUiState> = _uiState.asStateFlow()
    val unitSystem: StateFlow<UnitSystem> = preferencesStore.unitSystem

    init {
        load()
        viewModelScope.launch {
            authRepository.isLoggedIn.drop(1).collect {
                load()
            }
        }
    }

    private fun load() {
        if (!authRepository.isCurrentlyLoggedIn()) {
            _uiState.update { it.copy(isLoading = false, isLoggedIn = false) }
            return
        }
        if (systemRepository.current.summary == null) {
            _uiState.update { it.copy(isLoading = false, isLoggedIn = true, isSupported = false) }
            return
        }
        myRidesRepository.cachedSummary()?.let { summary ->
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    isSupported = true,
                    error = null,
                    summary = summary,
                )
            }
            if (myRidesRepository.isSummaryStale()) {
                refreshSummary()
            }
            return
        }
        _uiState.update { it.copy(isLoading = true, isLoggedIn = true, isSupported = true, error = null) }
        refreshSummary()
    }

    private fun refreshSummary() {
        viewModelScope.launch {
            myRidesRepository.getSummary(forceRefresh = true)
                .onSuccess { summary ->
                    _uiState.update { it.copy(isLoading = false, summary = summary, error = null) }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        if (state.summary != null) {
                            state.copy(isLoading = false)
                        } else {
                            state.copy(isLoading = false, error = error.message ?: "Couldn't load stats.")
                        }
                    }
                }
        }
    }

    fun refresh() = load()

    companion object {
        fun provideFactory(container: AppContainer) = viewModelFactory {
            initializer {
                MyRidesViewModel(
                    myRidesRepository = container.myRidesRepository,
                    authRepository = container.authRepository,
                    systemRepository = container.systemRepository,
                    preferencesStore = container.preferencesStore,
                )
            }
        }
    }
}
