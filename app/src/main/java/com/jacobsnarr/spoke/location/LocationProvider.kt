package com.jacobsnarr.spoke.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class UserLocation(val latitude: Double, val longitude: Double, val accuracyMeters: Float? = null)

/**
 * Location access via the platform [LocationManager] rather than Google Play Services'
 * FusedLocationProvider, because the target device (Mudita Kompakt) runs de-Googled AOSP.
 */
class LocationProvider(private val context: Context) {
    @Volatile
    private var debugLocationOverride: UserLocation? = null

    fun setDebugLocationOverride(location: UserLocation?) {
        debugLocationOverride = location
    }

    fun isUsingDebugLocationOverride(): Boolean = debugLocationOverride != null

    fun hasPermission(): Boolean {
        val fine =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        val coarse =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Returns the user's *current* location. Actively requests a fresh fix (GPS preferred, then
     * network) so the result follows the user as they move. A cached last-known fix is only used
     * as a fallback, and only when it is recent enough — otherwise a stale fix (e.g. the user's
     * home from hours ago) would be returned indefinitely.
     */
    suspend fun getCurrentLocation(): UserLocation? {
        debugLocationOverride?.let { return it }
        if (!hasPermission()) return null
        val manager =
            context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return null

        val fresh =
            try {
                withTimeout(FRESH_FIX_TIMEOUT) { requestFreshFix(manager) }
            } catch (_: TimeoutCancellationException) {
                null
            }
        if (fresh != null) return fresh.toUserLocation()

        return recentLastKnownLocation(manager)?.toUserLocation()
    }

    private suspend fun requestFreshFix(manager: LocationManager): Location? {
        val provider =
            when {
                isEnabled(manager, LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                isEnabled(manager, LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> return null
            }
        val executor = ContextCompat.getMainExecutor(context)
        return suspendCancellableCoroutine { cont ->
            val cancellationSignal = CancellationSignal()
            cont.invokeOnCancellation { cancellationSignal.cancel() }
            try {
                LocationManagerCompat.getCurrentLocation(
                    manager,
                    provider,
                    cancellationSignal,
                    executor,
                ) { location ->
                    if (cont.isActive) cont.resume(location)
                }
            } catch (_: SecurityException) {
                if (cont.isActive) cont.resume(null)
            }
        }
    }

    private fun recentLastKnownLocation(manager: LocationManager): Location? {
        val now = System.currentTimeMillis()
        return listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        ).filter { isEnabled(manager, it) }
            .mapNotNull { provider ->
                try {
                    manager.getLastKnownLocation(provider)
                } catch (_: SecurityException) {
                    null
                }
            }.filter { now - it.time <= MAX_LAST_KNOWN_AGE_MILLIS }
            .maxByOrNull { it.time }
    }

    private fun isEnabled(manager: LocationManager, provider: String): Boolean =
        runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)

    private fun Location.toUserLocation() = UserLocation(latitude, longitude, if (accuracy > 0) accuracy else null)

    private companion object {
        val FRESH_FIX_TIMEOUT: Duration = 15.seconds
        const val MAX_LAST_KNOWN_AGE_MILLIS = 2 * 60 * 1000L
    }
}
