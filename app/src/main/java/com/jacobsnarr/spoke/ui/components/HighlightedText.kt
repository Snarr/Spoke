package com.jacobsnarr.spoke.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

internal fun buildHighlightedString(text: String, query: String): AnnotatedString = buildAnnotatedString {
    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()
    var start = 0
    while (start < text.length) {
        val matchIndex = lowerText.indexOf(lowerQuery, start)
        if (matchIndex == -1) {
            append(text.substring(start))
            break
        }
        append(text.substring(start, matchIndex))
        withStyle(SpanStyle(fontWeight = FontWeight.Black)) {
            append(text.substring(matchIndex, matchIndex + query.length))
        }
        start = matchIndex + query.length
    }
}
