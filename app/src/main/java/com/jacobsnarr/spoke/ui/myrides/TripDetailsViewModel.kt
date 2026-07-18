package com.jacobsnarr.spoke.ui.myrides

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jacobsnarr.spoke.data.auth.AuthRepository
import com.jacobsnarr.spoke.data.debug.DebugSettingsRepository
import com.jacobsnarr.spoke.data.prefs.PreferencesStore
import com.jacobsnarr.spoke.data.prefs.UnitSystem
import com.jacobsnarr.spoke.data.remote.dto.TripDto
import com.jacobsnarr.spoke.data.ride.MyRidesRepository
import com.jacobsnarr.spoke.data.station.StationRepository
import com.jacobsnarr.spoke.data.system.SystemRepository
import com.jacobsnarr.spoke.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TripDetailsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val trip: TripDto? = null,
    val isSupported: Boolean = true,
    val isLoggedIn: Boolean = false,
)

class TripDetailsViewModel(
    private val tripId: Long,
    private val myRidesRepository: MyRidesRepository,
    private val authRepository: AuthRepository,
    private val systemRepository: SystemRepository,
    private val preferencesStore: PreferencesStore,
    private val stationRepository: StationRepository,
    private val debugSettingsRepository: DebugSettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TripDetailsUiState())
    val uiState: StateFlow<TripDetailsUiState> = _uiState.asStateFlow()
    val unitSystem: StateFlow<UnitSystem> = preferencesStore.unitSystem

    init {
        load()
        viewModelScope.launch {
            authRepository.isLoggedIn.drop(1).collect { load() }
        }
    }

    private fun load() {
        if (debugSettingsRepository.debugDummyTripsEnabled.value) {
            val demoTrip = buildDummyTrip(tripId = tripId, stationNames = stationNamesForDummy())
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isSupported = true,
                    isLoggedIn = true,
                    trip = demoTrip,
                    error = null,
                )
            }
            return
        }

        if (!authRepository.isCurrentlyLoggedIn()) {
            _uiState.update { it.copy(isLoading = false, isLoggedIn = false) }
            return
        }
        if (systemRepository.current.trips == null) {
            _uiState.update { it.copy(isLoading = false, isLoggedIn = true, isSupported = false) }
            return
        }
        myRidesRepository.cachedTripById(tripId)?.let { trip ->
            val visibleTrip = sanitizeTripForDebug(trip)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    isSupported = true,
                    trip = visibleTrip,
                    error = null,
                )
            }
            return
        }
        val today = LocalDate.now()
        myRidesRepository.cachedPastTrips(today.monthValue, today.year)?.let { trips ->
            val matched = trips.firstOrNull { it.tripId == tripId }?.let(::sanitizeTripForDebug)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    isSupported = true,
                    trip = matched,
                    error = if (matched == null) "Trip not found." else null,
                )
            }
            if (matched == null || myRidesRepository.isPastTripsStale(today.monthValue, today.year)) {
                refreshTrip(today.monthValue, today.year)
            }
            return
        }
        _uiState.update { it.copy(isLoading = true, isLoggedIn = true, isSupported = true, error = null) }
        refreshTrip(today.monthValue, today.year)
    }

    private fun refreshTrip(month: Int, year: Int) {
        viewModelScope.launch {
            myRidesRepository.getPastTrips(month, year, forceRefresh = true)
                .onSuccess { trips ->
                    val matched =
                        trips.firstOrNull { it.tripId == tripId }
                            ?: myRidesRepository.cachedTripById(tripId)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            trip = matched?.let(::sanitizeTripForDebug),
                            error = if (matched == null) "Trip not found." else null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        if (state.trip != null) {
                            state.copy(isLoading = false)
                        } else {
                            state.copy(isLoading = false, error = error.message ?: "Couldn't load trip details.")
                        }
                    }
                }
        }
    }

    private fun sanitizeTripForDebug(trip: TripDto): TripDto {
        if (!debugSettingsRepository.debugDummyTripsEnabled.value) return trip
        return redactTrips(listOf(trip), stationNames = stationNamesForDummy()).first()
    }

    private fun stationNamesForDummy(): List<String> = stationRepository.cachedStations().map { it.name }

    companion object {
        fun provideFactory(container: AppContainer, tripId: Long) = viewModelFactory {
            initializer {
                TripDetailsViewModel(
                    tripId = tripId,
                    myRidesRepository = container.myRidesRepository,
                    authRepository = container.authRepository,
                    systemRepository = container.systemRepository,
                    preferencesStore = container.preferencesStore,
                    stationRepository = container.stationRepository,
                    debugSettingsRepository = container.debugSettingsRepository,
                )
            }
        }
    }
}
