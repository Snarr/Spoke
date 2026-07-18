package com.jacobsnarr.spoke.ui.stationdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jacobsnarr.spoke.data.auth.AuthRepository
import com.jacobsnarr.spoke.data.prefs.PreferencesStore
import com.jacobsnarr.spoke.data.prefs.UnitSystem
import com.jacobsnarr.spoke.data.ride.CheckoutResult
import com.jacobsnarr.spoke.data.ride.RideRepository
import com.jacobsnarr.spoke.data.station.StationRepository
import com.jacobsnarr.spoke.data.station.model.Bike
import com.jacobsnarr.spoke.data.station.model.Station
import com.jacobsnarr.spoke.data.system.SystemRepository
import com.jacobsnarr.spoke.di.AppContainer
import com.jacobsnarr.spoke.location.DistanceCalculator
import com.jacobsnarr.spoke.location.LocationProvider
import com.jacobsnarr.spoke.location.UserLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StationDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val station: Station? = null,
    val checkoutSupported: Boolean = true,
    val pendingUnlockDock: Int? = null,
    val unlockingDock: Int? = null,
    val userLocation: UserLocation? = null,
    val locationAccuracy: LocationAccuracyStatus = LocationAccuracyStatus.UNKNOWN,
    val isFavorited: Boolean = false,
)

enum class LocationAccuracyStatus {
    GOOD, // < 50m
    MEDIUM, // 50-150m
    POOR, // > 150m
    UNAVAILABLE,
    UNKNOWN,
}

class StationDetailViewModel(
    private val stationId: Int,
    private val stationRepository: StationRepository,
    private val rideRepository: RideRepository,
    private val authRepository: AuthRepository,
    private val systemRepository: SystemRepository,
    private val locationProvider: LocationProvider,
    private val preferencesStore: PreferencesStore,
) : ViewModel() {
    private val _uiState =
        MutableStateFlow(
            StationDetailUiState(
                checkoutSupported = canCheckout(),
                isFavorited = isFavorited(),
            ),
        )
    val uiState: StateFlow<StationDetailUiState> = _uiState.asStateFlow()
    val unitSystem: StateFlow<UnitSystem> = preferencesStore.unitSystem

    init {
        load()
        fetchLocation()
        viewModelScope.launch {
            // Unlock is only offered to signed-in users on systems with a checkout endpoint;
            // observe login state so signing in reflects here without an app restart.
            authRepository.isLoggedIn.collect {
                _uiState.update { state -> state.copy(checkoutSupported = canCheckout()) }
            }
        }
    }

    private fun fetchLocation() {
        viewModelScope.launch {
            val location = locationProvider.getCurrentLocation()
            if (location != null) {
                val accuracy = classifyAccuracy(location.accuracyMeters)
                _uiState.update {
                    it.copy(
                        userLocation = location,
                        locationAccuracy = accuracy,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(locationAccuracy = LocationAccuracyStatus.UNAVAILABLE)
                }
            }
        }
    }

    private fun canCheckout(): Boolean = systemRepository.current.checkout != null && authRepository.isCurrentlyLoggedIn()

    private fun isFavorited(): Boolean = preferencesStore.isFavoritedStation(systemRepository.current.id, stationId)

    private fun load() {
        val cached = stationRepository.findById(stationId)
        if (cached != null) {
            _uiState.update { it.copy(isLoading = false, station = cached) }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            stationRepository
                .getStations()
                .onSuccess {
                    val station = stationRepository.findById(stationId)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            station = station,
                            error = if (station == null) "Station not found." else null,
                        )
                    }
                }.onFailure {
                    _uiState.update { it.copy(isLoading = false, error = "Couldn't load station.") }
                }
        }
    }

    fun toggleFavorite() {
        val favorited =
            preferencesStore.toggleFavoriteStation(
                systemId = systemRepository.current.id,
                stationId = stationId,
            )
        _uiState.update { it.copy(isFavorited = favorited) }
    }

    fun onBikeSelected(bike: Bike) {
        if (!bike.isAvailable) return
        if (!_uiState.value.checkoutSupported) return
        _uiState.update { it.copy(pendingUnlockDock = bike.dockNumber) }
    }

    private fun classifyAccuracy(accuracyMeters: Float?): LocationAccuracyStatus = when {
        accuracyMeters == null -> LocationAccuracyStatus.UNAVAILABLE
        accuracyMeters < 50 -> LocationAccuracyStatus.GOOD
        accuracyMeters < 150 -> LocationAccuracyStatus.MEDIUM
        else -> LocationAccuracyStatus.POOR
    }

    fun distanceToStation(userLocation: UserLocation, station: Station): Double = DistanceCalculator.haversineMeters(
        userLocation.latitude,
        userLocation.longitude,
        station.latitude,
        station.longitude,
    )

    fun dismissUnlock() {
        _uiState.update { it.copy(pendingUnlockDock = null) }
    }

    fun confirmUnlock(onUnlocked: () -> Unit) {
        val station = _uiState.value.station ?: return
        val dock = _uiState.value.pendingUnlockDock ?: return
        _uiState.update { it.copy(pendingUnlockDock = null, unlockingDock = dock) }
        viewModelScope.launch {
            val token = authRepository.ensureValidToken()
            if (token == null) {
                _uiState.update {
                    it.copy(
                        unlockingDock = null,
                        error = "Couldn't verify your session right now. Check your connection and try again.",
                    )
                }
                return@launch
            }
            val result =
                rideRepository.requestUnlock(
                    stationId = station.id,
                    stationName = station.name,
                    bikeDockNumber = dock,
                    bearerToken = token,
                )
            when (result) {
                is CheckoutResult.Success -> {
                    _uiState.update { it.copy(unlockingDock = null) }
                    onUnlocked()
                }
                is CheckoutResult.Declined -> {
                    val message =
                        if (station.isEnergySaving) {
                            "Checkout declined. Press the button on the bike dock to activate the " +
                                "station, then try again."
                        } else {
                            "Checkout declined. Please try again."
                        }
                    _uiState.update { it.copy(unlockingDock = null, error = message) }
                }
                is CheckoutResult.Error -> {
                    _uiState.update { it.copy(unlockingDock = null, error = result.message) }
                }
            }
        }
    }

    companion object {
        fun provideFactory(container: AppContainer, stationId: Int) = viewModelFactory {
            initializer {
                StationDetailViewModel(
                    stationId = stationId,
                    stationRepository = container.stationRepository,
                    rideRepository = container.rideRepository,
                    authRepository = container.authRepository,
                    systemRepository = container.systemRepository,
                    locationProvider = container.locationProvider,
                    preferencesStore = container.preferencesStore,
                )
            }
        }
    }
}
