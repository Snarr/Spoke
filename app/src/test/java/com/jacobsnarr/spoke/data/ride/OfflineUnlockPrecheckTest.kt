package com.jacobsnarr.spoke.data.ride

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfflineUnlockPrecheckTest {
    @Test
    fun `returns offline error before token checks`() {
        val error = resolveUnlockPrecheck(isOffline = true, bearerToken = null)

        assertEquals("Offline mode: reconnect to unlock a bike.", error?.message)
    }

    @Test
    fun `returns login error when online and token missing`() {
        val error = resolveUnlockPrecheck(isOffline = false, bearerToken = null)

        assertEquals("You need to be logged in to unlock a bike.", error?.message)
    }

    @Test
    fun `passes precheck when online and token present`() {
        val error = resolveUnlockPrecheck(isOffline = false, bearerToken = "token")

        assertNull(error)
    }
}
