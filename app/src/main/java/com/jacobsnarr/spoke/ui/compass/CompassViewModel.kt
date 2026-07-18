package com.jacobsnarr.spoke.ui.compass

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jacobsnarr.spoke.data.station.StationRepository
import com.jacobsnarr.spoke.data.station.model.Station
import com.jacobsnarr.spoke.di.AppContainer
import com.jacobsnarr.spoke.location.LocationProvider
import com.jacobsnarr.spoke.location.UserLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class CompassUiState(
    val isLoading: Boolean = true,
    val station: Station? = null,
    val userLocation: UserLocation? = null,
    val locationDenied: Boolean = false,
    val error: String? = null,
) {
    /** Bearing (degrees, true north referenced) from the user to the station, if both are known. */
    val bearingToStation: Float?
        get() {
            val station = station ?: return null
            val user = userLocation ?: return null
            return bearingDegrees(user.latitude, user.longitude, station.latitude, station.longitude)
        }

    /** Distance in meters from the user to the station, if both locations are known. */
    val distanceMeters: Double?
        get() {
            val station = station ?: return null
            val user = userLocation ?: return null
            return haversineMeters(user.latitude, user.longitude, station.latitude, station.longitude)
        }
}

class CompassViewModel(
    private val stationId: Int,
    private val stationRepository: StationRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CompassUiState())
    val uiState: StateFlow<CompassUiState> = _uiState.asStateFlow()

    init {
        loadStation()
        if (locationProvider.hasPermission()) {
            loadLocation()
        }
    }

    fun hasLocationPermission(): Boolean = locationProvider.hasPermission()

    /** Called after the runtime location permission has been granted. */
    fun onPermissionGranted() {
        _uiState.update { it.copy(locationDenied = false) }
        loadLocation()
    }

    fun onPermissionDenied() {
        _uiState.update { it.copy(locationDenied = true) }
    }

    private fun loadStation() {
        val cached = stationRepository.findById(stationId)
        if (cached != null) {
            _uiState.update { it.copy(isLoading = false, station = cached) }
            return
        }
        viewModelScope.launch {
            stationRepository
                .getStations()
                .onSuccess {
                    val station = stationRepository.findById(stationId)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            station = station,
                            error = if (station == null) "Station not found." else it.error,
                        )
                    }
                }.onFailure {
                    _uiState.update { it.copy(isLoading = false, error = "Couldn't load station.") }
                }
        }
    }

    private fun loadLocation() {
        viewModelScope.launch {
            val location = locationProvider.getCurrentLocation()
            _uiState.update {
                it.copy(
                    userLocation = location,
                    locationDenied = location == null && !locationProvider.hasPermission(),
                )
            }
        }
    }

    companion object {
        fun provideFactory(container: AppContainer, stationId: Int) = viewModelFactory {
            initializer {
                CompassViewModel(
                    stationId = stationId,
                    stationRepository = container.stationRepository,
                    locationProvider = container.locationProvider,
                )
            }
        }
    }
}

/** Initial bearing of the great-circle path from (lat1, lon1) to (lat2, lon2), in degrees. */
private fun bearingDegrees(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val deltaLambda = Math.toRadians(lon2 - lon1)
    val y = sin(deltaLambda) * cos(phi2)
    val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
    val bearing = Math.toDegrees(atan2(y, x))
    return ((bearing + 360.0) % 360.0).toFloat()
}

/** Haversine distance in meters between two lat/lng points. */
private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6_371_000.0 // Earth radius in meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a =
        sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    return r * 2 * atan2(Math.sqrt(a), Math.sqrt(1 - a))
}
