package com.jacobsnarr.spoke.ui.stations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jacobsnarr.spoke.data.auth.AuthRepository
import com.jacobsnarr.spoke.data.network.ConnectivityRepository
import com.jacobsnarr.spoke.data.prefs.PreferencesStore
import com.jacobsnarr.spoke.data.ride.RideRepository
import com.jacobsnarr.spoke.data.station.StationRepository
import com.jacobsnarr.spoke.data.station.model.Station
import com.jacobsnarr.spoke.data.system.SystemRepository
import com.jacobsnarr.spoke.di.AppContainer
import com.jacobsnarr.spoke.location.LocationProvider
import com.jacobsnarr.spoke.location.UserLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StationsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val userLocation: UserLocation? = null,
    val locationLoading: Boolean = false,
    val stations: List<Station> = emptyList(),
    val showFavoritesOnly: Boolean = false,
)

class StationsViewModel(
    private val stationRepository: StationRepository,
    private val locationProvider: LocationProvider,
    private val preferencesStore: PreferencesStore,
    private val systemRepository: SystemRepository,
    private val authRepository: AuthRepository,
    private val rideRepository: RideRepository,
    private val connectivityRepository: ConnectivityRepository,
) : ViewModel() {
    private var allStations: List<Station> = emptyList()

    private val _uiState = MutableStateFlow(StationsUiState())
    val uiState: StateFlow<StationsUiState> = _uiState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            // Reload when the user switches bikeshare systems (skip the current value on start).
            systemRepository.currentSystem.drop(1).collect { refresh() }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            stationRepository
                .getStations()
                .onSuccess { stations ->
                    allStations = stations
                    _uiState.update { it.copy(isLoading = false, error = null) }
                    recompute()
                }.onFailure {
                    val errorMessage =
                        if (connectivityRepository.isOffline.value && allStations.isEmpty()) {
                            "Offline mode: no cached stations yet. Reconnect and tap refresh."
                        } else {
                            "Couldn't load stations. Tap refresh to retry."
                        }
                    _uiState.update {
                        it.copy(isLoading = false, error = errorMessage)
                    }
                }
        }
        // Spec: refresh should also re-sort by proximity when location sort is active.
        refreshLocationIfEnabled()
        syncTripsNow()
    }

    /** True when the one-time first-launch location prompt has not yet been shown. */
    fun shouldPromptForLocation(): Boolean = !preferencesStore.locationPromptShown && !locationProvider.hasPermission()

    fun markLocationPromptShown() {
        preferencesStore.locationPromptShown = true
    }

    fun onLocationRequested() {
        preferencesStore.locationSortEnabled = true
        _uiState.update { it.copy(locationLoading = true) }
        viewModelScope.launch {
            val location = locationProvider.getCurrentLocation()
            _uiState.update { it.copy(locationLoading = false, userLocation = location) }
            recompute()
        }
    }

    /** Re-fetches the location when proximity sort is already on, so the list follows the user. */
    fun refreshLocationIfEnabled() {
        if (preferencesStore.locationSortEnabled && locationProvider.hasPermission()) {
            onLocationRequested()
        }
    }

    /** Recomputes visible rows from latest favorites after returning from nested screens. */
    fun onResume() {
        recompute()
        refreshLocationIfEnabled()
    }

    fun toggleFavoritesFilter() {
        _uiState.update { it.copy(showFavoritesOnly = !it.showFavoritesOnly) }
        recompute()
    }

    /** Forces a one-shot trips reconciliation so ended rides clear without waiting for poll cadence. */
    private fun syncTripsNow() {
        if (!authRepository.isCurrentlyLoggedIn()) return
        viewModelScope.launch {
            val token = authRepository.ensureValidToken()
            rideRepository.syncFromTrips(token)
        }
    }

    private fun recompute() {
        val state = _uiState.value
        val sorted =
            stationRepository.sortAndFilter(
                stations = allStations,
                query = "",
                userLat = state.userLocation?.latitude,
                userLng = state.userLocation?.longitude,
            )
        val visible =
            if (state.showFavoritesOnly) {
                filterFavoriteStations(
                    stations = sorted,
                    favoriteStationIds = preferencesStore.getFavoriteStationIds(systemRepository.current.id),
                )
            } else {
                sorted
            }
        _uiState.update { it.copy(stations = visible) }
    }

    companion object {
        fun provideFactory(container: AppContainer) = viewModelFactory {
            initializer {
                StationsViewModel(
                    container.stationRepository,
                    container.locationProvider,
                    container.preferencesStore,
                    container.systemRepository,
                    container.authRepository,
                    container.rideRepository,
                    container.connectivityRepository,
                )
            }
        }
    }
}

internal fun filterFavoriteStations(stations: List<Station>, favoriteStationIds: Set<Int>): List<Station> =
    stations.filter { favoriteStationIds.contains(it.id) }
