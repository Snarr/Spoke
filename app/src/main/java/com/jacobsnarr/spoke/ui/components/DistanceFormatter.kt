package com.jacobsnarr.spoke.ui.components

import com.jacobsnarr.spoke.data.prefs.UnitSystem
import kotlin.math.roundToInt

fun formatDistance(meters: Double, unitSystem: UnitSystem): String = when (unitSystem) {
    UnitSystem.IMPERIAL -> {
        val feet = meters * 3.28084
        if (feet < 528) {
            "${feet.roundToInt()} ft"
        } else {
            val miles = meters / 1609.344
            "${(miles * 10).roundToInt() / 10.0} mi"
        }
    }
    UnitSystem.METRIC -> {
        if (meters < 1000) {
            "${meters.roundToInt()} m"
        } else {
            val km = meters / 1000.0
            "${(km * 10).roundToInt() / 10.0} km"
        }
    }
}
