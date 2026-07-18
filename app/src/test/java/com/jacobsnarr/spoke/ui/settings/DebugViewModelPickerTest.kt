package com.jacobsnarr.spoke.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugViewModelPickerTest {
    @Test
    fun `returns fallback station when list empty`() {
        val name = pickRandomStationName(emptyList())

        assertEquals("Phantom Cycle Terminal", name)
    }

    @Test
    fun `returns member from list when stations exist`() {
        val stations = listOf("A", "B", "C")

        repeat(10) {
            val name = pickRandomStationName(stations)
            assertTrue(stations.contains(name))
        }
    }
}
