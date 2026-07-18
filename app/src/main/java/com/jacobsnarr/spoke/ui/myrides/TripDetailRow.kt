package com.jacobsnarr.spoke.ui.myrides

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Start
import androidx.compose.ui.graphics.vector.ImageVector
import com.jacobsnarr.spoke.data.remote.dto.TripDto
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal data class TripDetailRow(val label: String, val value: String, val icon: ImageVector)

internal fun buildTripDetailRows(trip: TripDto): List<TripDetailRow> = listOf(
    TripDetailRow("Date", formatDate(trip.checkOutDate), Icons.Filled.History),
    TripDetailRow("From", trip.checkOutLocation ?: "Unknown", Icons.Outlined.Start),
    TripDetailRow("To", trip.checkInLocation ?: "Unknown", Icons.Outlined.Flag),
    TripDetailRow("Duration", formatTripDuration(trip), Icons.Filled.History),
    TripDetailRow("Cost", formatCurrency(trip.cost), Icons.Filled.Payment),
    TripDetailRow("Money Saved", formatCurrency(trip.moneySaved), Icons.Filled.BarChart),
)

internal fun formatDate(value: String?): String {
    if (value.isNullOrBlank()) return "Unknown"
    return runCatching {
        Instant.parse(value)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("MM/dd/yy", Locale.getDefault()))
    }.getOrDefault("Unknown")
}

private fun formatCurrency(value: Double): String {
    if (value <= 0.0) return "$0.00"
    return NumberFormat.getCurrencyInstance().format(value)
}

private fun formatTripDuration(trip: TripDto): String {
    val minutes = resolveDurationMinutes(trip)
    return if (minutes > 0) "$minutes min" else "Unknown"
}

private fun resolveDurationMinutes(trip: TripDto): Int {
    if (trip.duration > 0) return trip.duration
    val outDate = trip.checkOutDate ?: return 0
    val inDate = trip.checkInDate ?: return 0
    return runCatching {
        val outMillis = Instant.parse(outDate).toEpochMilli()
        val inMillis = Instant.parse(inDate).toEpochMilli()
        ((inMillis - outMillis) / MILLIS_PER_MINUTE).toInt().coerceAtLeast(0)
    }.getOrDefault(0)
}

private const val MILLIS_PER_MINUTE = 60_000L
