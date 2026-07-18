package com.jacobsnarr.spoke.ui.myrides

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jacobsnarr.spoke.data.auth.AuthRepository
import com.jacobsnarr.spoke.data.remote.dto.UnpaidFeesDto
import com.jacobsnarr.spoke.data.ride.MyRidesRepository
import com.jacobsnarr.spoke.data.system.SystemRepository
import com.jacobsnarr.spoke.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UnpaidFeesUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val fees: UnpaidFeesDto? = null,
    val isSupported: Boolean = true,
    val isLoggedIn: Boolean = false,
)

class UnpaidFeesViewModel(
    private val myRidesRepository: MyRidesRepository,
    private val authRepository: AuthRepository,
    private val systemRepository: SystemRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UnpaidFeesUiState())
    val uiState: StateFlow<UnpaidFeesUiState> = _uiState.asStateFlow()

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
        myRidesRepository.cachedUnpaidFees()?.let { fees ->
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    isSupported = true,
                    error = null,
                    fees = fees,
                )
            }
            if (myRidesRepository.isUnpaidFeesStale()) {
                refreshUnpaidFees()
            }
            return
        }
        _uiState.update { it.copy(isLoading = true, isLoggedIn = true, isSupported = true, error = null) }
        refreshUnpaidFees()
    }

    private fun refreshUnpaidFees() {
        viewModelScope.launch {
            myRidesRepository.getUnpaidFees(forceRefresh = true)
                .onSuccess { fees ->
                    _uiState.update { it.copy(isLoading = false, fees = fees, error = null) }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        if (state.fees != null) {
                            state.copy(isLoading = false)
                        } else {
                            state.copy(isLoading = false, error = error.message ?: "Couldn't load unpaid fees.")
                        }
                    }
                }
        }
    }

    fun refresh() = load()

    companion object {
        fun provideFactory(container: AppContainer) = viewModelFactory {
            initializer {
                UnpaidFeesViewModel(
                    myRidesRepository = container.myRidesRepository,
                    authRepository = container.authRepository,
                    systemRepository = container.systemRepository,
                )
            }
        }
    }
}
