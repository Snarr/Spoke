package com.jacobsnarr.spoke.data.ride

import com.jacobsnarr.spoke.data.auth.AuthRepository
import com.jacobsnarr.spoke.data.remote.SummaryApi
import com.jacobsnarr.spoke.data.remote.TripsApi
import com.jacobsnarr.spoke.data.remote.UnpaidFeesApi
import com.jacobsnarr.spoke.data.remote.dto.TripDto
import com.jacobsnarr.spoke.data.remote.dto.UnpaidFeesDto
import com.jacobsnarr.spoke.data.remote.dto.UserSummaryDto
import com.jacobsnarr.spoke.data.system.SystemRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate

/**
 * Fetches user-specific data for the My Rides screen: lifetime stats, past trips, and unpaid fees.
 *
 * This repository is separate from [TripRepository], which handles active-ride detection and ride
 * lifecycle. Each function returns [Result.failure] if the current system doesn't support the
 * endpoint or the user is not signed in, so the UI can display graceful "not supported" states.
 */
class MyRidesRepository(
    private val summaryApi: SummaryApi,
    private val tripsApi: TripsApi,
    private val unpaidFeesApi: UnpaidFeesApi,
    private val authRepository: AuthRepository,
    private val systemRepository: SystemRepository,
) {
    private val cacheLock = Any()
    private val summaryCache = mutableMapOf<String, CachedValue<UserSummaryDto>>()
    private val tripsCache = mutableMapOf<TripCacheKey, CachedValue<List<TripDto>>>()
    private val unpaidFeesCache = mutableMapOf<String, CachedValue<UnpaidFeesDto>>()

    /**
     * Returns the signed-in user's lifetime ride summary (miles, money saved, calories, carbon).
     */
    suspend fun getSummary(forceRefresh: Boolean = false): Result<UserSummaryDto> {
        if (!forceRefresh) {
            cachedSummary()?.let { return Result.success(it) }
        }
        val token = authRepository.ensureValidToken()
            ?: return Result.failure(
                IllegalStateException("Couldn't refresh your session right now. Check your connection and try again."),
            )
        val summaryUrl = systemRepository.current.summary?.summaryUrl
            ?: return Result.failure(IllegalStateException("User summary is not supported for ${systemRepository.current.displayName}."))
        return runCatching {
            summaryApi.getSummary(url = summaryUrl, authorization = "Bearer $token")
                .also {
                    synchronized(cacheLock) {
                        summaryCache[systemRepository.current.id] = CachedValue(it)
                    }
                }
        }
    }

    /**
     * Returns completed trips for the given [month] and [year], with the active-trip sentinel
     * entry filtered out. An active trip has a null [TripDto.checkInLocation].
     */
    suspend fun getPastTrips(month: Int, year: Int, forceRefresh: Boolean = false): Result<List<TripDto>> {
        if (!forceRefresh) {
            cachedPastTrips(month, year)?.let { return Result.success(it) }
        }
        val token = authRepository.ensureValidToken()
            ?: return Result.failure(
                IllegalStateException("Couldn't refresh your session right now. Check your connection and try again."),
            )
        val tripsUrl = systemRepository.current.trips?.tripsUrl
            ?: return Result.failure(IllegalStateException("Trip history is not supported for ${systemRepository.current.displayName}."))
        return runCatching {
            tripsApi
                .getTrips(url = tripsUrl, authorization = "Bearer $token", month = month, year = year)
                .filter { it.checkInLocation != null }
                .also {
                    synchronized(cacheLock) {
                        tripsCache[TripCacheKey(systemRepository.current.id, month, year)] = CachedValue(it)
                    }
                }
        }
    }

    /**
     * Returns the signed-in user's unpaid fees.
     */
    suspend fun getUnpaidFees(forceRefresh: Boolean = false): Result<UnpaidFeesDto> {
        if (!forceRefresh) {
            cachedUnpaidFees()?.let { return Result.success(it) }
        }
        val token = authRepository.ensureValidToken()
            ?: return Result.failure(
                IllegalStateException("Couldn't refresh your session right now. Check your connection and try again."),
            )
        val unpaidFeesUrl = systemRepository.current.summary?.unpaidFeesUrl
            ?: return Result.failure(IllegalStateException("Unpaid fees are not supported for ${systemRepository.current.displayName}."))
        return runCatching {
            unpaidFeesApi.getUnpaidFees(url = unpaidFeesUrl, authorization = "Bearer $token")
                .also {
                    synchronized(cacheLock) {
                        unpaidFeesCache[systemRepository.current.id] = CachedValue(it)
                    }
                }
        }
    }

    /**
     * Best-effort warm-up for the My Rides pages. Queries lifetime stats, the current month's trips,
     * and unpaid fees so their pages can open from cache instead of waiting on a fresh fetch.
     */
    suspend fun prefetchCurrentMonthData() = coroutineScope {
        val today = LocalDate.now()
        val jobs = buildList {
            if (cachedSummary() == null || isSummaryStale()) add(async { getSummary(forceRefresh = true) })
            if (cachedPastTrips(today.monthValue, today.year) == null || isPastTripsStale(today.monthValue, today.year)) {
                add(
                    async {
                        getPastTrips(today.monthValue, today.year, forceRefresh = true)
                    },
                )
            }
            if (cachedUnpaidFees() == null || isUnpaidFeesStale()) add(async { getUnpaidFees(forceRefresh = true) })
        }
        jobs.forEach { it.await() }
    }

    fun invalidateCurrentSystemCache() = synchronized(cacheLock) {
        val systemId = systemRepository.current.id
        summaryCache.remove(systemId)
        unpaidFeesCache.remove(systemId)
        tripsCache.keys.removeAll { it.systemId == systemId }
    }

    fun cachedSummary(): UserSummaryDto? = synchronized(cacheLock) {
        summaryCache[systemRepository.current.id]?.value
    }

    fun cachedPastTrips(month: Int, year: Int): List<TripDto>? = synchronized(cacheLock) {
        tripsCache[TripCacheKey(systemRepository.current.id, month, year)]?.value
    }
    fun cachedTripById(tripId: Long): TripDto? = synchronized(cacheLock) {
        val systemId = systemRepository.current.id
        tripsCache.entries
            .asSequence()
            .filter { it.key.systemId == systemId }
            .flatMap { it.value.value.asSequence() }
            .firstOrNull { it.tripId == tripId }
    }

    fun cachedUnpaidFees(): UnpaidFeesDto? = synchronized(cacheLock) {
        unpaidFeesCache[systemRepository.current.id]?.value
    }

    fun isSummaryStale(): Boolean = !isFresh(summaryCache[systemRepository.current.id], SUMMARY_MAX_AGE_MILLIS)

    fun isPastTripsStale(month: Int, year: Int): Boolean =
        !isFresh(tripsCache[TripCacheKey(systemRepository.current.id, month, year)], TRIPS_MAX_AGE_MILLIS)
    fun isUnpaidFeesStale(): Boolean = !isFresh(unpaidFeesCache[systemRepository.current.id], UNPAID_FEES_MAX_AGE_MILLIS)

    private data class TripCacheKey(val systemId: String, val month: Int, val year: Int)

    data class CachedValue<T>(val value: T, val fetchedAtMillis: Long = System.currentTimeMillis())

    private companion object {
        const val SUMMARY_MAX_AGE_MILLIS = 10 * 60_000L
        const val TRIPS_MAX_AGE_MILLIS = 5 * 60_000L
        const val UNPAID_FEES_MAX_AGE_MILLIS = 60_000L
    }
}

private fun <T> isFresh(cacheEntry: MyRidesRepository.CachedValue<T>?, maxAgeMillis: Long): Boolean {
    if (cacheEntry == null) return false
    return System.currentTimeMillis() - cacheEntry.fetchedAtMillis <= maxAgeMillis
}
