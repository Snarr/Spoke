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
import java.time.Instant
import java.time.YearMonth

data class TripHistoryUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMoreMonths: Boolean = false,
    val error: String? = null,
    val trips: List<TripDto> = emptyList(),
    val isSupported: Boolean = true,
    val isLoggedIn: Boolean = false,
)

class TripHistoryViewModel(
    private val myRidesRepository: MyRidesRepository,
    private val authRepository: AuthRepository,
    private val systemRepository: SystemRepository,
    private val preferencesStore: PreferencesStore,
    private val stationRepository: StationRepository,
    private val debugSettingsRepository: DebugSettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TripHistoryUiState())
    val uiState: StateFlow<TripHistoryUiState> = _uiState.asStateFlow()
    val unitSystem: StateFlow<UnitSystem> = preferencesStore.unitSystem
    private val allTrips = mutableListOf<TripDto>()
    private val loadedMonthSet = mutableSetOf<YearMonth>()
    private var nextMonthCursor: YearMonth? = null
    private var loadedMonths = 0
    private var emptyMonthStreak = 0
    private var loadingMore = false

    init {
        load()
        viewModelScope.launch {
            authRepository.isLoggedIn.drop(1).collect {
                load()
            }
        }
    }

    private fun load() {
        resetPagination()
        if (debugSettingsRepository.debugDummyTripsEnabled.value) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    isSupported = true,
                    error = null,
                    trips = buildDummyTrips(stationNamesForDummy()),
                    hasMoreMonths = false,
                )
            }
            return
        }

        if (!authRepository.isCurrentlyLoggedIn()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isLoggedIn = false,
                    trips = emptyList(),
                    error = null,
                    hasMoreMonths = false,
                )
            }
            return
        }
        if (systemRepository.current.trips == null) {
            _uiState.update { it.copy(isLoading = false, isLoggedIn = true, isSupported = false) }
            return
        }
        val currentMonth = YearMonth.now()
        myRidesRepository.cachedPastTrips(currentMonth.monthValue, currentMonth.year)?.let { trips ->
            val visibleTrips = sanitizeTripsForDebug(trips)
            recordLoadedMonth(currentMonth)
            updateMonthStats(visibleTrips)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    isSupported = true,
                    error = null,
                    trips = mergeIntoAllTrips(visibleTrips),
                    hasMoreMonths = shouldContinueLoading(loadedMonths, emptyMonthStreak),
                )
            }
            if (myRidesRepository.isPastTripsStale(currentMonth.monthValue, currentMonth.year)) {
                refreshMonth(currentMonth)
            }
            maybeAutoloadOlderMonths()
            return
        }
        _uiState.update { it.copy(isLoading = true, isLoggedIn = true, isSupported = true, error = null, hasMoreMonths = false) }
        refreshMonth(currentMonth)
    }

    private fun refreshMonth(month: YearMonth) {
        viewModelScope.launch {
            myRidesRepository.getPastTrips(month.monthValue, month.year, forceRefresh = true)
                .onSuccess { trips ->
                    val visibleTrips = sanitizeTripsForDebug(trips)
                    recordLoadedMonth(month)
                    updateMonthStats(visibleTrips)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            trips = mergeIntoAllTrips(visibleTrips),
                            hasMoreMonths = shouldContinueLoading(loadedMonths, emptyMonthStreak),
                            error = null,
                        )
                    }
                    maybeAutoloadOlderMonths()
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        if (state.trips.isNotEmpty()) {
                            state.copy(isLoading = false)
                        } else {
                            state.copy(isLoading = false, error = error.message ?: "Couldn't load trips.")
                        }
                    }
                }
        }
    }

    fun loadMore() {
        if (loadingMore) return
        if (!shouldContinueLoading(loadedMonths, emptyMonthStreak)) return
        val month = nextMonthCursor ?: return
        loadingMore = true
        _uiState.update { it.copy(isLoadingMore = true, error = null) }
        viewModelScope.launch {
            val cached = myRidesRepository.cachedPastTrips(month.monthValue, month.year)
            if (cached != null) {
                val visibleTrips = sanitizeTripsForDebug(cached)
                recordLoadedMonth(month)
                updateMonthStats(visibleTrips)
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        trips = mergeIntoAllTrips(visibleTrips),
                        hasMoreMonths = shouldContinueLoading(loadedMonths, emptyMonthStreak),
                    )
                }
                loadingMore = false
                if (myRidesRepository.isPastTripsStale(month.monthValue, month.year)) {
                    refreshOlderMonth(month)
                }
                return@launch
            }
            refreshOlderMonth(month)
        }
    }

    private fun refreshOlderMonth(month: YearMonth) {
        viewModelScope.launch {
            myRidesRepository.getPastTrips(month.monthValue, month.year, forceRefresh = true)
                .onSuccess { trips ->
                    val visibleTrips = sanitizeTripsForDebug(trips)
                    recordLoadedMonth(month)
                    updateMonthStats(visibleTrips)
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            trips = mergeIntoAllTrips(visibleTrips),
                            hasMoreMonths = shouldContinueLoading(loadedMonths, emptyMonthStreak),
                            error = null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            isLoadingMore = false,
                            hasMoreMonths = shouldContinueLoading(loadedMonths, emptyMonthStreak),
                            error = if (state.trips.isEmpty()) {
                                error.message ?: "Couldn't load trips."
                            } else {
                                state.error
                            },
                        )
                    }
                }
            loadingMore = false
        }
    }

    private fun recordLoadedMonth(month: YearMonth) {
        if (loadedMonthSet.add(month)) {
            loadedMonths += 1
            nextMonthCursor = month.minusMonths(1)
        }
    }

    private fun updateMonthStats(trips: List<TripDto>) {
        if (trips.isEmpty()) {
            emptyMonthStreak += 1
        } else {
            emptyMonthStreak = 0
        }
    }

    private fun maybeAutoloadOlderMonths() {
        if (_uiState.value.trips.isNotEmpty()) return
        if (!shouldContinueLoading(loadedMonths, emptyMonthStreak)) return
        loadMore()
    }

    private fun resetPagination() {
        allTrips.clear()
        loadedMonthSet.clear()
        nextMonthCursor = null
        loadedMonths = 0
        emptyMonthStreak = 0
        loadingMore = false
    }

    private fun mergeIntoAllTrips(newTrips: List<TripDto>): List<TripDto> {
        val merged = mergeTripsAndSort(allTrips, newTrips)
        allTrips.clear()
        allTrips.addAll(merged)
        return merged
    }

    private fun sanitizeTripsForDebug(trips: List<TripDto>): List<TripDto> {
        if (!debugSettingsRepository.debugDummyTripsEnabled.value) return trips
        return redactTrips(trips, stationNames = stationNamesForDummy())
    }

    private fun stationNamesForDummy(): List<String> = stationRepository.cachedStations().map { it.name }

    fun refresh() = load()

    companion object {
        fun provideFactory(container: AppContainer) = viewModelFactory {
            initializer {
                TripHistoryViewModel(
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

internal fun mergeTripsAndSort(existingTrips: List<TripDto>, newTrips: List<TripDto>): List<TripDto> {
    val mergedById = (existingTrips + newTrips).associateBy { it.tripId }.values.toList()
    return mergedById.sortedMostRecentFirst()
}

internal fun shouldContinueLoading(
    loadedMonths: Int,
    emptyMonthStreak: Int,
    maxLookbackMonths: Int = MAX_LOOKBACK_MONTHS,
    maxEmptyMonths: Int = MAX_EMPTY_MONTH_STREAK,
): Boolean = loadedMonths < maxLookbackMonths && emptyMonthStreak < maxEmptyMonths

private fun List<TripDto>.sortedMostRecentFirst(): List<TripDto> = sortedByDescending { trip ->
    parseTripInstantMillis(trip.checkOutDate)
}

private fun parseTripInstantMillis(value: String?): Long {
    if (value.isNullOrBlank()) return Long.MIN_VALUE
    return runCatching { Instant.parse(value).toEpochMilli() }
        .getOrDefault(Long.MIN_VALUE)
}

private const val MAX_LOOKBACK_MONTHS = 12
private const val MAX_EMPTY_MONTH_STREAK = 2
