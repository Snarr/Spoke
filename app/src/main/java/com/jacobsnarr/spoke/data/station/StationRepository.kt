package com.jacobsnarr.spoke.data.station

import com.jacobsnarr.spoke.data.remote.StationStatusApi
import com.jacobsnarr.spoke.data.remote.dto.StationPropertiesDto
import com.jacobsnarr.spoke.data.station.model.Bike
import com.jacobsnarr.spoke.data.station.model.Station
import com.jacobsnarr.spoke.data.system.SystemRepository
import com.jacobsnarr.spoke.location.DistanceCalculator

class StationRepository(private val stationStatusApi: StationStatusApi, private val systemRepository: SystemRepository) {
    @Volatile
    private var cache: List<Station> = emptyList()

    suspend fun getStations(): Result<List<Station>> {
        val networkResult = runCatching {
            val response = stationStatusApi.getStations(systemRepository.current.stationPath)
            val stations = response.features.map { it.properties.toStation() }
            cache = stations
            stations
        }
        return resolveStationsResult(networkResult, cache)
    }
    fun findById(id: Int): Station? = cache.firstOrNull { it.id == id }
    fun cachedStations(): List<Station> = cache

    /** Drops cached stations, e.g. when switching bikeshare systems or logging out. */
    fun clearCache() {
        cache = emptyList()
    }

    /**
     * Returns stations sorted alphabetically by name, or by proximity when the user's
     * coordinates are provided. An optional [query] filters by name (case-insensitive).
     */
    fun sortAndFilter(stations: List<Station>, query: String, userLat: Double?, userLng: Double?): List<Station> {
        val filtered =
            if (query.isBlank()) {
                stations
            } else {
                stations.filter { it.name.contains(query.trim(), ignoreCase = true) }
            }
        return if (userLat != null && userLng != null) {
            filtered
                .map { it.copy(distanceMeters = DistanceCalculator.haversineMeters(userLat, userLng, it.latitude, it.longitude)) }
                .sortedBy { it.distanceMeters }
        } else {
            filtered.sortedBy { it.name.lowercase() }
        }
    }

    private fun StationPropertiesDto.toStation(): Station = Station(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        totalDocks = totalDocks,
        docksAvailable = docksAvailable,
        bikesAvailable = bikesAvailable,
        classicBikesAvailable = classicBikesAvailable,
        electricBikesAvailable = electricBikesAvailable,
        kioskStatus = kioskStatus,
        kioskType = kioskType,
        addressStreet = addressStreet,
        addressCity = addressCity,
        addressState = addressState,
        addressZipCode = addressZipCode,
        bikes =
        bikes
            .map { Bike(it.dockNumber, it.isElectric, it.isAvailable, it.battery) }
            .sortedBy { !it.isAvailable },
    )
}

internal fun resolveStationsResult(networkResult: Result<List<Station>>, cache: List<Station>): Result<List<Station>> = networkResult.fold(
    onSuccess = { Result.success(it) },
    onFailure = { error ->
        if (cache.isNotEmpty()) {
            Result.success(cache)
        } else {
            Result.failure(error)
        }
    },
)
