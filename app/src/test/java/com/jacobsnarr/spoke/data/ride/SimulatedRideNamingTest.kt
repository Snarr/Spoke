package com.jacobsnarr.spoke.data.ride

import org.junit.Assert.assertEquals
import org.junit.Test

class SimulatedRideNamingTest {
    @Test
    fun `simulated ride uses provided station name`() {
        val trip = createSimulatedTrip(stationName = "Broad & Spring Garden", startedAtMillis = 1234L)
        val state = createSimulatedRideState(trip)

        assertEquals("Broad & Spring Garden", trip.stationName)
        assertEquals("Broad & Spring Garden", state.stationName)
        assertEquals(-1L, state.tripId)
        assertEquals(1234L, state.startedAtMillis)
    }
}
