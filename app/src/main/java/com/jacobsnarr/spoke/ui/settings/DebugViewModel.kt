package com.jacobsnarr.spoke.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jacobsnarr.spoke.data.debug.DebugSettingsRepository
import com.jacobsnarr.spoke.data.ride.RideRepository
import com.jacobsnarr.spoke.data.ride.RideState
import com.jacobsnarr.spoke.data.station.StationRepository
import com.jacobsnarr.spoke.di.AppContainer
import com.jacobsnarr.spoke.location.LocationProvider
import com.jacobsnarr.spoke.location.UserLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DebugUiState(
    val rideActive: Boolean = false,
    val usingTestLocation: Boolean = false,
    val compassEnabled: Boolean = false,
    val dummyTripsEnabled: Boolean = false,
    val statusMessage: String? = null,
)

class DebugViewModel(
    private val rideRepository: RideRepository,
    private val stationRepository: StationRepository,
    private val debugSettingsRepository: DebugSettingsRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DebugUiState())
    val uiState: StateFlow<DebugUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                rideRepository.rideState,
                debugSettingsRepository.debugLocationOverride,
                debugSettingsRepository.debugCompassEnabled,
                debugSettingsRepository.debugDummyTripsEnabled,
            ) { rideState, locationOverride, compassEnabled, dummyTripsEnabled ->
                DebugUiState(
                    rideActive = rideState is RideState.Active,
                    usingTestLocation = locationOverride != null,
                    compassEnabled = compassEnabled,
                    dummyTripsEnabled = dummyTripsEnabled,
                    statusMessage = (_uiState.value.statusMessage),
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun toggleRide() {
        if (_uiState.value.rideActive) {
            rideRepository.stopSimulatedRide()
            _uiState.update { it.copy(statusMessage = null) }
            return
        }

        viewModelScope.launch {
            val stationName = pickDebugRideStationName(stationRepository)
            rideRepository.startSimulatedRide(stationName)
            _uiState.update { it.copy(statusMessage = stationName) }
        }
    }

    fun toggleTestLocation() {
        val current = _uiState.value.usingTestLocation
        if (current) {
            debugSettingsRepository.setDebugLocationOverride(null)
            locationProvider.setDebugLocationOverride(null)
            return
        }

        val fakeLocation = UserLocation(latitude = TEST_LATITUDE, longitude = TEST_LONGITUDE, accuracyMeters = TEST_ACCURACY_METERS)
        debugSettingsRepository.setDebugLocationOverride(fakeLocation)
        locationProvider.setDebugLocationOverride(fakeLocation)
    }

    fun toggleCompass() {
        debugSettingsRepository.setDebugCompassEnabled(!_uiState.value.compassEnabled)
    }

    fun toggleDummyTrips() {
        debugSettingsRepository.setDebugDummyTripsEnabled(!_uiState.value.dummyTripsEnabled)
    }

    fun hideDebugMenuUntilRestart() {
        debugSettingsRepository.hideDebugMenuUntilRestart()
    }

    companion object {
        fun provideFactory(container: AppContainer) = viewModelFactory {
            initializer {
                DebugViewModel(
                    rideRepository = container.rideRepository,
                    stationRepository = container.stationRepository,
                    debugSettingsRepository = container.debugSettingsRepository,
                    locationProvider = container.locationProvider,
                )
            }
        }
    }
}

internal suspend fun pickDebugRideStationName(stationRepository: StationRepository): String {
    val stations = stationRepository.getStations().getOrElse { stationRepository.cachedStations() }
    return pickRandomStationName(stations.map { it.name })
}

internal fun pickRandomStationName(stationNames: List<String>): String {
    if (stationNames.isEmpty()) return FALLBACK_IMAGINARY_STATION
    return stationNames.random()
}

private const val TEST_LATITUDE = 39.96460
private const val TEST_LONGITUDE = -75.17957
private const val TEST_ACCURACY_METERS = 8f
private const val FALLBACK_IMAGINARY_STATION = "Phantom Cycle Terminal"
