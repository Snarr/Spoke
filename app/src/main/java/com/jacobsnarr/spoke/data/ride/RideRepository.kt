package com.jacobsnarr.spoke.data.ride

import android.util.Log
import com.jacobsnarr.spoke.data.network.ConnectivityRepository
import com.jacobsnarr.spoke.data.remote.CheckoutApi
import com.jacobsnarr.spoke.data.remote.dto.CheckoutRequestDto
import com.jacobsnarr.spoke.data.system.SystemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the current ride state and performs bike checkout (unlock) requests against the current
 * bikeshare system's checkout endpoint. A local [RideState.Active] is only started once a checkout
 * succeeds. Systems without a documented checkout endpoint report the feature as unavailable.
 *
 * The ride lifecycle is authoritatively driven by the server: [syncFromTrips] reconciles local
 * state against the `/user/trips` endpoint, adopting rides started elsewhere and ending the local
 * ride once the server reports no active trip. There is intentionally no manual "end ride" control.
 */
class RideRepository(
    private val checkoutApi: CheckoutApi,
    private val systemRepository: SystemRepository,
    private val tripRepository: TripRepository,
    private val connectivityRepository: ConnectivityRepository,
) {
    @Volatile
    private var lastLocalUnlockAtMillis: Long = 0L

    private val _rideState = MutableStateFlow<RideState>(RideState.NoActiveRide)
    val rideState: StateFlow<RideState> = _rideState.asStateFlow()

    /**
     * Requests a checkout (unlock) for [bikeDockNumber] at [stationId] using the caller's
     * [bearerToken]. On success the local ride is marked active. The success response format is
     * undocumented and energy-saving stations can respond with `["CheckoutDeclined"]`, so the raw
     * body is inspected defensively.
     */
    suspend fun requestUnlock(stationId: Int, stationName: String, bikeDockNumber: Int, bearerToken: String?): CheckoutResult {
        resolveUnlockPrecheck(
            isOffline = connectivityRepository.isOffline.value,
            bearerToken = bearerToken,
        )?.let { return it }

        val system = systemRepository.current
        val checkout =
            system.checkout
                ?: return CheckoutResult.Error(
                    "Bike checkout isn't available for ${system.displayName} yet.",
                )

        val result =
            runCatching {
                checkoutApi.checkout(
                    url = checkout.checkoutUrl,
                    authorization = "Bearer $bearerToken",
                    body =
                    CheckoutRequestDto(
                        dockNumber = bikeDockNumber,
                        stationId = stationId,
                        systemId = checkout.systemId,
                    ),
                    programId = checkout.programId,
                )
            }.getOrElse {
                return CheckoutResult.Error("Couldn't reach the checkout service. Check your connection.")
            }

        val rawBody = (result.body() ?: result.errorBody())?.string().orEmpty()

        if (!result.isSuccessful || rawBody.contains(DECLINED_CODE, ignoreCase = true)) {
            return CheckoutResult.Declined
        }

        lastLocalUnlockAtMillis = System.currentTimeMillis()
        _rideState.value =
            RideState.Active(
                tripId = null,
                stationName = stationName,
                bikeDockNumber = bikeDockNumber,
                startedAtMillis = lastLocalUnlockAtMillis,
            )

        // Immediately confirm the ride against the trips feed so the session is backed by a real
        // trip and the timer runs from the server's check-out time. If the trip hasn't propagated
        // yet, the grace window keeps the local ride and a later poll adopts the server start.
        syncFromTrips(bearerToken)
        return CheckoutResult.Success
    }

    /**
     * Reconciles local ride state with the server's `/user/trips` view:
     *  - An active trip is **adopted** (or refreshed) into [RideState.Active] using the server's
     *    checkout time as the authoritative start, so timers stay accurate and rides started on
     *    another device are picked up.
     *  - When the server reports **no** active trip, the local ride is ended — except within a
     *    short grace window right after an in-app unlock, to absorb server propagation lag.
     *
     * A failed poll (network/parse error, or trips unsupported) never changes state; only a
     * successful "no active trip" response can end a ride.
     */
    suspend fun syncFromTrips(bearerToken: String?) {
        if (systemRepository.current.trips == null) {
            Log.d("RideRepository", "syncFromTrips: trips endpoint not supported")
            return
        }

        val activeTrip = tripRepository.activeTrip(bearerToken).getOrElse {
            Log.d("RideRepository", "syncFromTrips: activeTrip failed")
            return
        }
        val current = _rideState.value
        Log.d("RideRepository", "syncFromTrips: activeTrip=$activeTrip, current=$current, lastLocalUnlockAtMillis=$lastLocalUnlockAtMillis")
        when (
            val nextState =
                resolveSyncedRideState(
                    current = current,
                    activeTrip = activeTrip,
                    lastLocalUnlockAtMillis = lastLocalUnlockAtMillis,
                    nowMillis = System.currentTimeMillis(),
                )
        ) {
            is RideState.Active -> {
                Log.d("RideRepository", "syncFromTrips: Setting ride to Active")
                _rideState.value = nextState
            }

            is RideState.NoActiveRide -> {
                if (current is RideState.Active) {
                    Log.d("RideRepository", "syncFromTrips: Ending ride")
                    endRide()
                }
            }
        }
    }

    fun endRide() {
        Log.d("RideRepository", "endRide: Ending active ride")
        lastLocalUnlockAtMillis = 0L
        _rideState.value = RideState.NoActiveRide
    }

    /**
     * Debug-only: starts a simulated active ride so the ride UI and lifecycle can be exercised
     * without an actual checkout. A fake trip (starting now) is injected into [tripRepository] so
     * the periodic poller keeps the ride alive, and the ride state is set immediately so it also
     * appears on systems without a trips endpoint / while signed out.
     */
    fun startSimulatedRide(stationName: String) {
        Log.d("RideRepository", "startSimulatedRide: Starting")
        val startedAt = System.currentTimeMillis()
        val fakeTrip = createSimulatedTrip(stationName, startedAt)
        tripRepository.setSimulatedTrip(fakeTrip)
        lastLocalUnlockAtMillis = startedAt
        _rideState.value = createSimulatedRideState(fakeTrip)
    }

    /**
     * Debug-only: ends a simulated ride started via [startSimulatedRide], clearing the injected
     * trip so subsequent polls report no active trip.
     */
    fun stopSimulatedRide() {
        tripRepository.setSimulatedTrip(null)
        endRide()
    }

    private companion object {
        const val DECLINED_CODE = "CheckoutDeclined"

        /** Trust an in-app unlock over the server for this long, to absorb propagation lag. */
        const val UNLOCK_GRACE_MILLIS = 90_000L
    }
}

internal fun resolveSyncedRideState(
    current: RideState,
    activeTrip: ActiveTrip?,
    lastLocalUnlockAtMillis: Long,
    nowMillis: Long,
    graceMillis: Long = 90_000L,
): RideState {
    if (activeTrip != null) {
        return RideState.Active(
            tripId = activeTrip.tripId,
            stationName = activeTrip.stationName ?: (current as? RideState.Active)?.stationName,
            bikeDockNumber = (current as? RideState.Active)?.bikeDockNumber,
            startedAtMillis = activeTrip.startedAtMillis,
        )
    }

    if (current !is RideState.Active) return current

    val sinceUnlock = nowMillis - lastLocalUnlockAtMillis
    if (lastLocalUnlockAtMillis > 0L && sinceUnlock < graceMillis) {
        return current
    }
    return RideState.NoActiveRide
}

internal fun resolveUnlockPrecheck(isOffline: Boolean, bearerToken: String?): CheckoutResult.Error? {
    if (isOffline) {
        return CheckoutResult.Error("Offline mode: reconnect to unlock a bike.")
    }
    if (bearerToken.isNullOrBlank()) {
        return CheckoutResult.Error("You need to be logged in to unlock a bike.")
    }
    return null
}

internal fun createSimulatedTrip(stationName: String, startedAtMillis: Long): ActiveTrip = ActiveTrip(
    tripId = -1L,
    stationName = stationName,
    startedAtMillis = startedAtMillis,
)

internal fun createSimulatedRideState(trip: ActiveTrip): RideState.Active = RideState.Active(
    tripId = trip.tripId,
    stationName = trip.stationName,
    bikeDockNumber = null,
    startedAtMillis = trip.startedAtMillis,
)

sealed interface CheckoutResult {
    data object Success : CheckoutResult

    data object Declined : CheckoutResult

    data class Error(val message: String) : CheckoutResult
}

sealed interface RideState {
    data object NoActiveRide : RideState

    /**
     * An in-progress ride. [tripId] and [bikeDockNumber] may be unknown for rides adopted from the
     * server (or before the first trips sync completes). [startedAtMillis] is server-authoritative
     * once a trip has been synced.
     */
    data class Active(val tripId: Long?, val stationName: String?, val bikeDockNumber: Int?, val startedAtMillis: Long) : RideState
}
