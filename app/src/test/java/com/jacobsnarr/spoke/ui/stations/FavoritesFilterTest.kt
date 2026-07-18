package com.jacobsnarr.spoke.ui.stations

import com.jacobsnarr.spoke.data.station.model.Station
import org.junit.Assert.assertEquals
import org.junit.Test

class FavoritesFilterTest {
    @Test
    fun `filters stations to favorites while preserving current order`() {
        val stations = listOf(station(3), station(1), station(2))

        val filtered = filterFavoriteStations(stations, favoriteStationIds = setOf(2, 3))

        assertEquals(listOf(3, 2), filtered.map { it.id })
    }

    @Test
    fun `returns empty list when no favorites match`() {
        val filtered = filterFavoriteStations(listOf(station(1), station(2)), favoriteStationIds = setOf(5))

        assertEquals(emptyList<Int>(), filtered.map { it.id })
    }

    private fun station(id: Int): Station = Station(
        id = id,
        name = "Station $id",
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
