package com.jacobsnarr.spoke.ui.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineBannerVisibilityTest {
    @Test
    fun `shows banner on normal screen when offline`() {
        assertTrue(shouldShowOfflineBanner(currentRoute = Routes.STATIONS, isOffline = true))
    }

    @Test
    fun `hides banner on system select route`() {
        assertFalse(shouldShowOfflineBanner(currentRoute = Routes.SYSTEM_SELECT, isOffline = true))
    }

    @Test
    fun `hides banner on account route`() {
        assertFalse(shouldShowOfflineBanner(currentRoute = Routes.SETTINGS_ACCOUNT, isOffline = true))
    }

    @Test
    fun `hides banner when online`() {
        assertFalse(shouldShowOfflineBanner(currentRoute = Routes.STATIONS, isOffline = false))
    }
}
