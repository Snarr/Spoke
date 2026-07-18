package com.jacobsnarr.spoke.data.ride

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RideSyncStateResolverTest {
    @Test
    fun `adopts server active trip and preserves local station fallback`() {
        val current = RideState.Active(tripId = null, stationName = "Local", bikeDockNumber = 7, startedAtMillis = 10L)
        val activeTrip = ActiveTrip(tripId = 42L, stationName = null, startedAtMillis = 1000L)

        val resolved =
            resolveSyncedRideState(
                current = current,
                activeTrip = activeTrip,
                lastLocalUnlockAtMillis = 0L,
                nowMillis = 2_000L,
            )

        assertTrue(resolved is RideState.Active)
        resolved as RideState.Active
        assertEquals(42L, resolved.tripId)
        assertEquals("Local", resolved.stationName)
        assertEquals(7, resolved.bikeDockNumber)
        assertEquals(1000L, resolved.startedAtMillis)
    }

    @Test
    fun `keeps active ride during unlock grace window when server has no active trip`() {
        val current = RideState.Active(tripId = 1L, stationName = "A", bikeDockNumber = 2, startedAtMillis = 100L)

        val resolved =
            resolveSyncedRideState(
                current = current,
                activeTrip = null,
                lastLocalUnlockAtMillis = 1_500L,
                nowMillis = 2_000L,
                graceMillis = 1_000L,
            )

        assertEquals(current, resolved)
    }

    @Test
    fun `ends active ride after grace window when server has no active trip`() {
        val current = RideState.Active(tripId = 1L, stationName = "A", bikeDockNumber = 2, startedAtMillis = 100L)

        val resolved =
            resolveSyncedRideState(
                current = current,
                activeTrip = null,
                lastLocalUnlockAtMillis = 500L,
                nowMillis = 2_000L,
                graceMillis = 1_000L,
            )

        assertEquals(RideState.NoActiveRide, resolved)
    }

    @Test
    fun `keeps no-active state when already no-active and server has no active trip`() {
        val resolved =
            resolveSyncedRideState(
                current = RideState.NoActiveRide,
                activeTrip = null,
                lastLocalUnlockAtMillis = 0L,
                nowMillis = 2_000L,
            )

        assertEquals(RideState.NoActiveRide, resolved)
    }
}
