package com.jacobsnarr.spoke.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * A single bike trip from the `/user/trips` endpoint. Unknown keys are ignored by the JSON parser.
 *
 * An **active** (in-progress) trip is identified by a null [checkInLocation] together with the
 * sentinel [checkInDate] of `0001-01-01T00:00:00Z` — the bike has been checked out but not yet
 * returned.
 *
 * The fields [miles], [cost], [moneySaved], [duration], and [isDurationAdjusted] are only used for
 * display (My Rides screen) and default to zero so the active-trip detection logic in
 * [com.jacobsnarr.spoke.data.ride.TripRepository] remains unaffected.
 */
@Serializable
data class TripDto(
    val tripId: Long,
    val programName: String? = null,
    val checkOutDate: String? = null,
    val checkOutLocation: String? = null,
    val checkOutLat: Double = 0.0,
    val checkOutLon: Double = 0.0,
    val checkInDate: String? = null,
    val checkInLocation: String? = null,
    val checkInLat: Double = 0.0,
    val checkInLon: Double = 0.0,
    val miles: Double = 0.0,
    val cost: Double = 0.0,
    val moneySaved: Double = 0.0,
    val duration: Int = 0,
    val isDurationAdjusted: Boolean = false,
)
