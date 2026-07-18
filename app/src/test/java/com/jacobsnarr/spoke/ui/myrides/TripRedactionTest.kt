package com.jacobsnarr.spoke.ui.myrides

import com.jacobsnarr.spoke.data.remote.dto.TripDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripRedactionTest {
    @Test
    fun `redacts trip location names and coordinates`() {
        val source =
            TripDto(
                tripId = 1L,
                checkOutLocation = "18th & Walnut",
                checkInLocation = "City Hall",
                checkOutLat = 39.95,
                checkOutLon = -75.16,
                checkInLat = 39.96,
                checkInLon = -75.17,
            )

        val redacted = redactTrip(source)

        assertEquals("Origin Station", redacted.checkOutLocation)
        assertEquals("Destination Station", redacted.checkInLocation)
        assertEquals(0.0, redacted.checkOutLat, 0.0)
        assertEquals(0.0, redacted.checkOutLon, 0.0)
        assertEquals(0.0, redacted.checkInLat, 0.0)
        assertEquals(0.0, redacted.checkInLon, 0.0)
        assertEquals(source.tripId, redacted.tripId)
    }

    @Test
    fun `redacts list preserving order and ids`() {
        val trips =
            listOf(
                TripDto(tripId = 10L, checkOutLocation = "A", checkInLocation = "B"),
                TripDto(tripId = 11L, checkOutLocation = "C", checkInLocation = "D"),
            )

        val redacted = redactTrips(trips)

        assertEquals(listOf(10L, 11L), redacted.map { it.tripId })
        assertEquals(listOf("Origin Station", "Origin Station"), redacted.map { it.checkOutLocation })
        assertEquals(listOf("Destination Station", "Destination Station"), redacted.map { it.checkInLocation })
    }

    @Test
    fun `redaction uses system station names when available`() {
        val source = TripDto(tripId = 99L, checkOutLocation = "Real A", checkInLocation = "Real B")
        val stationNames = listOf("Walnut & 18th", "City Hall", "Broad & Spring Garden")

        val redacted = redactTrips(listOf(source), stationNames = stationNames).first()

        assertTrue(stationNames.contains(redacted.checkOutLocation))
        assertTrue(stationNames.contains(redacted.checkInLocation))
        assertNotNull(redacted.checkOutDate)
        assertNotNull(redacted.checkInDate)
        assertTrue(redacted.duration > 0)
        assertTrue(redacted.cost > 0.0)
        assertTrue(redacted.moneySaved > 0.0)
        assertTrue(redacted.miles > 0.0)
    }

    @Test
    fun `build dummy trips creates populated screenshot friendly rows`() {
        val stationNames = listOf("Walnut & 18th", "City Hall", "Broad & Spring Garden")
        val trips = buildDummyTrips(stationNames, count = 3)

        assertEquals(3, trips.size)
        trips.forEach { trip ->
            assertTrue(stationNames.contains(trip.checkOutLocation))
            assertTrue(stationNames.contains(trip.checkInLocation))
            assertNotNull(trip.checkOutDate)
            assertNotNull(trip.checkInDate)
            assertTrue(trip.duration > 0)
            assertTrue(trip.cost > 0.0)
            assertTrue(trip.moneySaved > 0.0)
            assertTrue(trip.miles > 0.0)
        }
    }
}
