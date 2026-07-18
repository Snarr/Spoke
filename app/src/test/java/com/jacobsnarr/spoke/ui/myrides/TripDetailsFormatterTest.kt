package com.jacobsnarr.spoke.ui.myrides

import com.jacobsnarr.spoke.data.remote.dto.TripDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripDetailsFormatterTest {
    @Test
    fun `build rows uses selected order`() {
        val trip = sampleTrip()

        val rows = buildTripDetailRows(trip)

        assertEquals(
            listOf("Date", "From", "To", "Duration", "Cost", "Money Saved"),
            rows.map { it.label },
        )
    }

    @Test
    fun `build rows includes unknown fallbacks`() {
        val trip = TripDto(tripId = 1L)

        val rows = buildTripDetailRows(trip)

        assertEquals("Unknown", rows.first { it.label == "Date" }.value)
        assertEquals("Unknown", rows.first { it.label == "From" }.value)
        assertEquals("Unknown", rows.first { it.label == "To" }.value)
        assertEquals("Unknown", rows.first { it.label == "Duration" }.value)
    }

    @Test
    fun `build rows formats currency`() {
        val trip = sampleTrip(cost = 2.5, moneySaved = 3.0)

        val rows = buildTripDetailRows(trip)

        assertTrue(rows.first { it.label == "Cost" }.value.contains("2"))
        assertTrue(rows.first { it.label == "Money Saved" }.value.contains("3"))
    }

    private fun sampleTrip(cost: Double = 1.0, moneySaved: Double = 2.0): TripDto = TripDto(
        tripId = 42L,
        checkOutDate = "2026-07-17T12:00:00Z",
        checkOutLocation = "A",
        checkInLocation = "B",
        miles = 1.5,
        cost = cost,
        moneySaved = moneySaved,
    )
}
