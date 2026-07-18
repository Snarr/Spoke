package com.jacobsnarr.spoke.data.debug

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DebugLocationOverrideCodecTest {
    @Test
    fun `parses valid latitude and longitude`() {
        val location =
            toDebugLocationOverride(
                latitudeRaw = "39.96460",
                longitudeRaw = "-75.17957",
                accuracyRaw = "5.5",
            )

        requireNotNull(location)
        assertEquals(39.96460, location.latitude, 0.0)
        assertEquals(-75.17957, location.longitude, 0.0)
        assertEquals(5.5f, location.accuracyMeters ?: 0f, 0f)
    }

    @Test
    fun `uses default accuracy when not provided`() {
        val location =
            toDebugLocationOverride(
                latitudeRaw = "39.96460",
                longitudeRaw = "-75.17957",
                accuracyRaw = null,
            )

        requireNotNull(location)
        assertEquals(8f, location.accuracyMeters ?: 0f, 0f)
    }

    @Test
    fun `returns null when latitude missing or invalid`() {
        assertNull(toDebugLocationOverride(latitudeRaw = null, longitudeRaw = "-75.17957", accuracyRaw = "8"))
        assertNull(toDebugLocationOverride(latitudeRaw = "x", longitudeRaw = "-75.17957", accuracyRaw = "8"))
    }

    @Test
    fun `returns null when longitude missing or invalid`() {
        assertNull(toDebugLocationOverride(latitudeRaw = "39.96460", longitudeRaw = null, accuracyRaw = "8"))
        assertNull(toDebugLocationOverride(latitudeRaw = "39.96460", longitudeRaw = "x", accuracyRaw = "8"))
    }
}
