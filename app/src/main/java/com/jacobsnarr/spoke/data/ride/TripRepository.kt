package com.jacobsnarr.spoke.data.ride

import android.util.Log
import com.jacobsnarr.spoke.data.remote.TripsApi
import com.jacobsnarr.spoke.data.remote.dto.TripDto
import com.jacobsnarr.spoke.data.system.SystemRepository
import java.time.Instant
import java.time.LocalDate

/**
 * A currently in-progress bike trip resolved from the `/user/trips` endpoint.
 */
data class ActiveTrip(val tripId: Long, val stationName: String?, val startedAtMillis: Long)

/**
 * Reads the signed-in user's trips to determine whether a ride is currently in progress.
 *
 * Correctness note: a *failure* result means "couldn't determine" (network/parse error) and must
 * never be treated as "no active trip" — otherwise a transient error would end a live ride. Only a
 * successful `null` means the user genuinely has no active trip.
 */
class TripRepository(private val tripsApi: TripsApi, private val systemRepository: SystemRepository) {
    @Volatile
    private var simulatedTrip: ActiveTrip? = null

    /**
     * Debug-only: forces [activeTrip] to report [trip] (or, when null, to fall back to the real
     * feed). Used by the simulated-ride testing tool so an active ride can be exercised without an
     * actual checkout, sign-in, or trips-capable system.
     */
    fun setSimulatedTrip(trip: ActiveTrip?) {
        Log.d("TripRepository", "setSimulatedTrip: $trip")
        simulatedTrip = trip
    }

    /**
     * Returns the active trip, or `null` when the user has none. Queries the current calendar month
     * and, if nothing active is found there, falls back to the previous month so a ride that began
     * just before a month boundary is not missed.
     */
    suspend fun activeTrip(bearerToken: String?): Result<ActiveTrip?> {
        Log.d("TripRepository", "activeTrip: simulatedTrip=$simulatedTrip")
        simulatedTrip?.let {
            Log.d("TripRepository", "activeTrip: Returning simulated trip")
            return Result.success(it)
        }

        val trips =
            systemRepository.current.trips
                ?: return Result.failure(IllegalStateException("Trips are not supported for this system."))
        if (bearerToken.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Not signed in."))
        }

        val today = LocalDate.now()
        val current = fetchActive(trips.tripsUrl, bearerToken, today.monthValue, today.year)
        val currentActive = current.getOrElse { return Result.failure(it) }
        if (currentActive != null) return Result.success(currentActive)

        val previous = today.minusMonths(1)
        return fetchActive(trips.tripsUrl, bearerToken, previous.monthValue, previous.year)
    }

    private suspend fun fetchActive(url: String, bearerToken: String, month: Int, year: Int): Result<ActiveTrip?> = runCatching {
        val trips =
            tripsApi.getTrips(
                url = url,
                authorization = "Bearer $bearerToken",
                month = month,
                year = year,
            )
        trips.firstOrNull { it.isActive }?.toActiveTrip()
    }

    private val TripDto.isActive: Boolean
        get() = checkInLocation == null

    private fun TripDto.toActiveTrip() = ActiveTrip(
        tripId = tripId,
        stationName = checkOutLocation,
        startedAtMillis = checkOutDate?.let { parseInstantMillis(it) } ?: System.currentTimeMillis(),
    )

    private fun parseInstantMillis(iso: String): Long? = runCatching { Instant.parse(iso).toEpochMilli() }.getOrNull()
}
