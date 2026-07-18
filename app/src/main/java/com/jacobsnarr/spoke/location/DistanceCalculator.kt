package com.jacobsnarr.spoke.location

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Calculates distances between geographic coordinates using the haversine formula.
 * This is the single source of truth for all distance calculations in the app,
 * ensuring consistency between stations list, search results, and checkout screens.
 */
object DistanceCalculator {
    /**
     * Calculates the distance in meters between two geographic points using the haversine formula.
     * Returns Double for full precision (avoids Float truncation errors).
     *
     * @param lat1 User/origin latitude in degrees
     * @param lng1 User/origin longitude in degrees
     * @param lat2 Station/destination latitude in degrees
     * @param lng2 Station/destination longitude in degrees
     * @return Distance in meters
     */
    fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a =
            sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2) * sin(dLng / 2)
        return EARTH_RADIUS_METERS * (2 * atan2(sqrt(a), sqrt(1 - a)))
    }

    private const val EARTH_RADIUS_METERS = 6_371_000.0
}
