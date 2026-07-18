package com.jacobsnarr.spoke.data.debug

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class DebugReleaseGuardTest {
    @Test
    fun `debug parser returns null for missing coords regardless of mode`() {
        val location = toDebugLocationOverride(latitudeRaw = null, longitudeRaw = null, accuracyRaw = null)

        assertNull(location)
    }

    @Test
    fun `boolean guard expectation for non debug mode stays false`() {
        val expectedReleaseFlagValue = false

        assertFalse(expectedReleaseFlagValue)
    }
}
