package com.jacobsnarr.spoke.data.prefs

import org.junit.Assert.assertEquals
import org.junit.Test

class FavoriteStationIdsCodecTest {
    @Test
    fun `parseFavoriteStationIds ignores invalid and empty entries`() {
        val parsed = parseFavoriteStationIds("1, 2,foo,, 3,2")

        assertEquals(setOf(1, 2, 3), parsed)
    }

    @Test
    fun `encodeFavoriteStationIds produces stable sorted string`() {
        val encoded = encodeFavoriteStationIds(setOf(42, 3, 7))

        assertEquals("3,7,42", encoded)
    }

    @Test
    fun `toggledFavoriteStationIds adds and removes`() {
        val added = toggledFavoriteStationIds(setOf(1, 2), 3)
        val removed = toggledFavoriteStationIds(added, 2)

        assertEquals(setOf(1, 2, 3), added)
        assertEquals(setOf(1, 3), removed)
    }
}
