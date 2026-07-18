package com.jacobsnarr.spoke.ui.myrides

import com.jacobsnarr.spoke.data.remote.dto.TripDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripHistoryPaginationTest {
    @Test
    fun `merge de-dupes by tripId and keeps most recent first`() {
        val existing =
            listOf(
                trip(id = 3, checkOutDate = "2026-07-01T12:00:00Z"),
                trip(id = 2, checkOutDate = "2026-06-15T12:00:00Z"),
            )
        val incoming =
            listOf(
                trip(id = 2, checkOutDate = "2026-06-15T12:00:00Z"),
                trip(id = 1, checkOutDate = "2026-05-10T12:00:00Z"),
            )

        val merged = mergeTripsAndSort(existing, incoming)

        assertEquals(listOf(3L, 2L, 1L), merged.map { it.tripId })
    }

    @Test
    fun `continue loading when within lookback and empty streak limits`() {
        assertTrue(shouldContinueLoading(loadedMonths = 4, emptyMonthStreak = 1))
    }

    @Test
    fun `stop loading when lookback reached`() {
        assertFalse(shouldContinueLoading(loadedMonths = 12, emptyMonthStreak = 0))
    }

    @Test
    fun `stop loading when empty streak reached`() {
        assertFalse(shouldContinueLoading(loadedMonths = 2, emptyMonthStreak = 2))
    }

    private fun trip(id: Long, checkOutDate: String): TripDto = TripDto(
        tripId = id,
        checkOutDate = checkOutDate,
        checkOutLocation = "A",
        checkInLocation = "B",
    )
}
