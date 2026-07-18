package com.jacobsnarr.spoke.ui.myrides

import com.jacobsnarr.spoke.data.remote.dto.TripDto
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripDummyDateSafetyTest {
    @Test
    fun `dummyization does not crash on malformed checkout date`() {
        val source = TripDto(tripId = 5L, checkOutDate = "bad-date", checkInDate = null)

        val redacted = redactTrips(listOf(source), stationNames = emptyList()).first()

        assertNotNull(redacted.checkOutDate)
        assertNotNull(redacted.checkInDate)
        assertTrue(redacted.duration > 0)
    }
}
