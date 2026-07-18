package com.jacobsnarr.spoke.ui.components

import com.jacobsnarr.spoke.data.prefs.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceFormatterTest {
    @Test
    fun `imperial formats short distances in feet`() {
        val formatted = formatDistance(meters = 100.0, unitSystem = UnitSystem.IMPERIAL)

        assertEquals("328 ft", formatted)
    }

    @Test
    fun `imperial formats long distances in miles with one decimal`() {
        val formatted = formatDistance(meters = 1609.344, unitSystem = UnitSystem.IMPERIAL)

        assertEquals("1.0 mi", formatted)
    }

    @Test
    fun `metric formats short distances in meters`() {
        val formatted = formatDistance(meters = 999.0, unitSystem = UnitSystem.METRIC)

        assertEquals("999 m", formatted)
    }

    @Test
    fun `metric formats long distances in kilometers with one decimal`() {
        val formatted = formatDistance(meters = 1250.0, unitSystem = UnitSystem.METRIC)

        assertEquals("1.3 km", formatted)
    }
}
