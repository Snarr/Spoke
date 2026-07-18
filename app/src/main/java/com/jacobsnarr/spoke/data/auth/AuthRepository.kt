package com.jacobsnarr.spoke.data.auth

import com.jacobsnarr.spoke.data.remote.IndegoAuthApi
import com.jacobsnarr.spoke.data.ride.RideRepository
import com.jacobsnarr.spoke.data.station.StationRepository
import com.jacobsnarr.spoke.data.system.BikeSystem
import com.jacobsnarr.spoke.data.system.SystemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AuthRepository(
    private val authApi: IndegoAuthApi,
    private val tokenStore: TokenStore,
    private val systemRepository: SystemRepository,
    private val stationRepository: StationRepository,
    private val rideRepository: RideRepository,
) {
    private val _isLoggedIn = MutableStateFlow(tokenStore.isLoggedIn(systemRepository.current.id))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val refreshMutex = Mutex()
    private val refreshBackoffLock = Any()
    private var transientRefreshFailureCount = 0
    private var nextRefreshAttemptAtMillis = 0L

    fun isCurrentlyLoggedIn(): Boolean = tokenStore.isLoggedIn(systemRepository.current.id)

    suspend fun login(system: BikeSystem, username: String, password: String): Result<Unit> {
        val auth =
            system.auth
                ?: return Result.failure(
                    IllegalStateException("Sign-in isn't available for ${system.displayName} yet."),
                )
        return runCatching {
            // Switch systems before authenticating so cached (other-system) stations are dropped.
            systemRepository.select(system)
            stationRepository.clearCache()
            val token =
                authApi.login(
                    username = username.trim(),
                    password = password,
                    acrValues = auth.acrValues,
                    clientId = auth.clientId,
                    clientSecret = auth.clientSecret,
                    scope = auth.scope,
                )
            tokenStore.saveSession(
                systemId = system.id,
                accessToken = token.accessToken,
                refreshToken = token.refreshToken,
                expiresInSeconds = token.expiresIn,
            )
            clearRefreshBackoff()
            _isLoggedIn.value = true
        }
    }

    /**
     * Exchanges the stored refresh token for a fresh access token.
     *
     * Only explicit auth-invalid failures clear the local session. Transient failures (DNS, timeout,
     * 5xx, 429) keep the user signed in and schedule a retry window.
     */
    suspend fun refreshAccessToken(): Result<Unit> {
        val system = systemRepository.current
        val refreshToken = tokenStore.refreshToken(system.id)
        if (refreshToken.isNullOrBlank()) {
            clearRefreshBackoff()
            logout()
            return Result.failure(IllegalStateException("No refresh token available"))
        }
        val auth = system.auth
        if (auth == null) {
            clearRefreshBackoff()
            logout()
            return Result.failure(IllegalStateException("No auth configuration for current system"))
        }
        return runCatching {
            val token =
                authApi.refresh(
                    refreshToken = refreshToken,
                    acrValues = auth.acrValues,
                    clientId = auth.clientId,
                    clientSecret = auth.clientSecret,
                )
            tokenStore.saveSession(
                systemId = system.id,
                accessToken = token.accessToken,
                // Some servers omit a rotated refresh token; keep the existing one if so.
                refreshToken = token.refreshToken ?: refreshToken,
                expiresInSeconds = token.expiresIn,
            )
            clearRefreshBackoff()
            _isLoggedIn.value = true
        }.onFailure {
            when (classifyRefreshFailure(it)) {
                RefreshFailureType.FatalAuth -> {
                    clearRefreshBackoff()
                    logout()
                }
                RefreshFailureType.Transient -> scheduleRefreshBackoff()
            }
        }
    }

    /**
     * Returns a currently-valid access token, refreshing proactively when it is within
     * [EXPIRY_BUFFER_MILLIS] of expiring. Returns null when no valid token can be obtained.
     * Concurrent callers are coalesced via [refreshMutex] so we refresh at most once.
     */
    suspend fun ensureValidToken(): String? {
        val systemId = systemRepository.current.id
        if (!tokenStore.isAccessTokenExpired(systemId, EXPIRY_BUFFER_MILLIS)) {
            return tokenStore.accessToken(systemId)
        }
        refreshMutex.withLock {
            // Re-check inside the lock: another caller may have refreshed while we waited.
            if (!tokenStore.isAccessTokenExpired(systemId, EXPIRY_BUFFER_MILLIS)) {
                return tokenStore.accessToken(systemId)
            }
            if (isRefreshBackoffActive()) {
                return null
            }
            val result = refreshAccessToken()
            return if (result.isSuccess) tokenStore.accessToken(systemId) else null
        }
    }

    fun logout() {
        clearRefreshBackoff()
        tokenStore.clear(systemRepository.current.id)
        stationRepository.clearCache()
        rideRepository.endRide()
        _isLoggedIn.value = false
    }

    /**
     * Switches to [system]. Sessions are retained per system, so any previously stored token for
     * [system] is restored: the user doesn't have to re-login when travelling between cities. The
     * active ride and cached (other-system) stations are dropped for the switch.
     */
    fun switchSystem(system: BikeSystem) {
        rideRepository.endRide()
        stationRepository.clearCache()
        systemRepository.select(system)
        clearRefreshBackoff()
        _isLoggedIn.value = tokenStore.isLoggedIn(system.id)
    }

    private fun isRefreshBackoffActive(nowMillis: Long = System.currentTimeMillis()): Boolean = synchronized(refreshBackoffLock) {
        isRefreshBackoffActive(nowMillis, nextRefreshAttemptAtMillis)
    }

    private fun clearRefreshBackoff() = synchronized(refreshBackoffLock) {
        transientRefreshFailureCount = 0
        nextRefreshAttemptAtMillis = 0L
    }

    private fun scheduleRefreshBackoff(nowMillis: Long = System.currentTimeMillis()) = synchronized(refreshBackoffLock) {
        val next = computeNextRefreshBackoff(
            failureCount = transientRefreshFailureCount,
            nowMillis = nowMillis,
            baseBackoffMillis = BASE_REFRESH_BACKOFF_MILLIS,
            maxBackoffMillis = MAX_REFRESH_BACKOFF_MILLIS,
            maxBackoffPower = MAX_BACKOFF_POWER,
        )
        transientRefreshFailureCount = next.failureCount
        nextRefreshAttemptAtMillis = next.nextAttemptAtMillis
    }

    private companion object {
        const val EXPIRY_BUFFER_MILLIS = 60_000L
        const val BASE_REFRESH_BACKOFF_MILLIS = 5_000L
        const val MAX_REFRESH_BACKOFF_MILLIS = 5 * 60_000L
        const val MAX_BACKOFF_POWER = 6
    }
}

internal data class RefreshBackoffState(val failureCount: Int, val nextAttemptAtMillis: Long)

internal fun isRefreshBackoffActive(nowMillis: Long, nextAttemptAtMillis: Long): Boolean = nowMillis < nextAttemptAtMillis

internal fun computeNextRefreshBackoff(
    failureCount: Int,
    nowMillis: Long,
    baseBackoffMillis: Long,
    maxBackoffMillis: Long,
    maxBackoffPower: Int,
): RefreshBackoffState {
    val nextFailureCount = (failureCount + 1).coerceAtMost(maxBackoffPower)
    val factor = 1L shl (nextFailureCount - 1)
    val backoffMillis = (baseBackoffMillis * factor).coerceAtMost(maxBackoffMillis)
    return RefreshBackoffState(
        failureCount = nextFailureCount,
        nextAttemptAtMillis = nowMillis + backoffMillis,
    )
}
