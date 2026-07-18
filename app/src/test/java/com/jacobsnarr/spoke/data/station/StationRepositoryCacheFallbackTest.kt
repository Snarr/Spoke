package com.jacobsnarr.spoke.data.station

import com.jacobsnarr.spoke.data.station.model.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StationRepositoryCacheFallbackTest {
    @Test
    fun `returns network result when request succeeds`() {
        val networkStations = listOf(sampleStation(id = 1, name = "Network"))
        val cacheStations = listOf(sampleStation(id = 2, name = "Cache"))

        val resolved = resolveStationsResult(Result.success(networkStations), cacheStations)

        assertEquals(networkStations, resolved.getOrNull())
    }

    @Test
    fun `returns cache when request fails and cache exists`() {
        val cacheStations = listOf(sampleStation(id = 2, name = "Cache"))

        val resolved = resolveStationsResult(Result.failure(IllegalStateException("offline")), cacheStations)

        assertEquals(cacheStations, resolved.getOrNull())
    }

    @Test
    fun `returns failure when request fails and cache empty`() {
        val resolved = resolveStationsResult(Result.failure(IllegalStateException("offline")), emptyList())

        assertTrue(resolved.isFailure)
    }

    private fun sampleStation(id: Int, name: String): Station = Station(
        id = id,
        name = name,
        latitude = 0.0,
        longitude = 0.0,
        totalDocks = 10,
        docksAvailable = 5,
        bikesAvailable = 5,
        classicBikesAvailable = 3,
        electricBikesAvailable = 2,
        kioskStatus = null,
        kioskType = null,
        addressStreet = null,
        addressCity = null,
        addressState = null,
        addressZipCode = null,
        bikes = emptyList(),
    )
}
