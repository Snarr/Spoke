package com.jacobsnarr.spoke.ui.search

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

data class SearchUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val query: String = "",
    val results: List<Station> = emptyList(),
    val userLocation: UserLocation? = null,
)

class SearchViewModel(private val stationRepository: StationRepository, private val locationProvider: LocationProvider) : ViewModel() {
    private var allStations: List<Station> = emptyList()

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        load()
        fetchLocation()
    }

    private fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            stationRepository
                .getStations()
                .onSuccess { stations ->
                    allStations = stations
                    _uiState.update { it.copy(isLoading = false, error = null) }
                    recompute()
                }.onFailure {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Couldn't load stations.")
                    }
                }
        }
    }

    private fun fetchLocation() {
        if (!locationProvider.hasPermission()) return
        viewModelScope.launch {
            val location = locationProvider.getCurrentLocation()
            _uiState.update { it.copy(userLocation = location) }
            recompute()
        }
    }

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
        recompute()
    }

    /** Repolls the stations read API; filtering still runs against the local list. */
    fun refresh() = load()

    private fun recompute() {
        val state = _uiState.value
        val results =
            if (state.query.isBlank()) {
                emptyList()
            } else {
                stationRepository.sortAndFilter(
                    stations = allStations,
                    query = state.query,
                    userLat = state.userLocation?.latitude,
                    userLng = state.userLocation?.longitude,
                )
            }
        _uiState.update { it.copy(results = results) }
    }

    companion object {
        fun provideFactory(container: AppContainer) = viewModelFactory {
            initializer { SearchViewModel(container.stationRepository, container.locationProvider) }
        }
    }
}
