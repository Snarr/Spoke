package com.jacobsnarr.spoke.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class TripDetailsRouteTest {
    @Test
    fun `trip details route builder formats path`() {
        assertEquals("my_rides/trips/123", Routes.tripDetails(123L))
    }
}
