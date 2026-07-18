package com.jacobsnarr.spoke.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class HighlightedTextTest {
    @Test
    fun `returns original text when query not found`() {
        val text = buildHighlightedString("Market Street", "bike")

        assertEquals("Market Street", text.text)
        assertEquals(0, text.spanStyles.size)
    }

    @Test
    fun `highlights case-insensitive single match`() {
        val text = buildHighlightedString("Market Street", "market")

        assertEquals("Market Street", text.text)
        assertEquals(1, text.spanStyles.size)
        assertEquals(0, text.spanStyles.first().start)
        assertEquals(6, text.spanStyles.first().end)
    }

    @Test
    fun `highlights all non-overlapping matches`() {
        val text = buildHighlightedString("Banana", "an")

        assertEquals("Banana", text.text)
        assertEquals(2, text.spanStyles.size)
        assertEquals(1, text.spanStyles[0].start)
        assertEquals(3, text.spanStyles[0].end)
        assertEquals(3, text.spanStyles[1].start)
        assertEquals(5, text.spanStyles[1].end)
    }
}
