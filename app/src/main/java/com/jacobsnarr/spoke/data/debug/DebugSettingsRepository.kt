package com.jacobsnarr.spoke.data.debug

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.jacobsnarr.spoke.BuildConfig
import com.jacobsnarr.spoke.location.UserLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val DEFAULT_DEBUG_ACCURACY_METERS = 8f

class DebugSettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    private val _debugCompassEnabled = MutableStateFlow(readDebugCompassEnabled())
    private val _debugDummyTripsEnabled = MutableStateFlow(readDebugDummyTripsEnabled())
    private val _debugLocationOverride = MutableStateFlow(readDebugLocationOverride())
    private val _debugMenuVisible = MutableStateFlow(BuildConfig.DEBUG)

    val debugCompassEnabled: StateFlow<Boolean> = _debugCompassEnabled.asStateFlow()
    val debugDummyTripsEnabled: StateFlow<Boolean> = _debugDummyTripsEnabled.asStateFlow()
    val debugLocationOverride: StateFlow<UserLocation?> = _debugLocationOverride.asStateFlow()
    val debugMenuVisible: StateFlow<Boolean> = _debugMenuVisible.asStateFlow()

    fun setDebugCompassEnabled(enabled: Boolean) {
        if (!BuildConfig.DEBUG) return
        prefs.edit { putBoolean(KEY_DEBUG_COMPASS_ENABLED, enabled) }
        _debugCompassEnabled.value = enabled
    }

    fun setDebugDummyTripsEnabled(enabled: Boolean) {
        if (!BuildConfig.DEBUG) return
        prefs.edit { putBoolean(KEY_DEBUG_DUMMY_TRIPS_ENABLED, enabled) }
        _debugDummyTripsEnabled.value = enabled
    }

    fun setDebugLocationOverride(location: UserLocation?) {
        if (!BuildConfig.DEBUG) return
        if (location == null) {
            prefs.edit {
                remove(KEY_DEBUG_LOCATION_LAT)
                remove(KEY_DEBUG_LOCATION_LNG)
                remove(KEY_DEBUG_LOCATION_ACCURACY_METERS)
            }
            _debugLocationOverride.value = null
            return
        }

        val accuracy = location.accuracyMeters ?: DEFAULT_DEBUG_ACCURACY_METERS
        prefs.edit {
            putString(KEY_DEBUG_LOCATION_LAT, location.latitude.toString())
            putString(KEY_DEBUG_LOCATION_LNG, location.longitude.toString())
            putString(KEY_DEBUG_LOCATION_ACCURACY_METERS, accuracy.toString())
        }
        _debugLocationOverride.value = location.copy(accuracyMeters = accuracy)
    }

    fun hideDebugMenuUntilRestart() {
        if (!BuildConfig.DEBUG) return
        _debugMenuVisible.value = false
    }

    private fun readDebugCompassEnabled(): Boolean = if (BuildConfig.DEBUG) {
        prefs.getBoolean(KEY_DEBUG_COMPASS_ENABLED, false)
    } else {
        false
    }

    private fun readDebugDummyTripsEnabled(): Boolean = if (BuildConfig.DEBUG) {
        prefs.getBoolean(KEY_DEBUG_DUMMY_TRIPS_ENABLED, false)
    } else {
        false
    }

    private fun readDebugLocationOverride(): UserLocation? {
        if (!BuildConfig.DEBUG) return null
        return toDebugLocationOverride(
            latitudeRaw = prefs.getString(KEY_DEBUG_LOCATION_LAT, null),
            longitudeRaw = prefs.getString(KEY_DEBUG_LOCATION_LNG, null),
            accuracyRaw = prefs.getString(KEY_DEBUG_LOCATION_ACCURACY_METERS, null),
        )
    }

    private companion object {
        const val PREFS_FILE = "spoke_debug_prefs"
        const val KEY_DEBUG_LOCATION_LAT = "debug_location_lat"
        const val KEY_DEBUG_LOCATION_LNG = "debug_location_lng"
        const val KEY_DEBUG_LOCATION_ACCURACY_METERS = "debug_location_accuracy_meters"
        const val KEY_DEBUG_COMPASS_ENABLED = "debug_compass_enabled"
        const val KEY_DEBUG_DUMMY_TRIPS_ENABLED = "debug_dummy_trips_enabled"
    }
}

internal fun toDebugLocationOverride(latitudeRaw: String?, longitudeRaw: String?, accuracyRaw: String?): UserLocation? {
    val latitude = latitudeRaw?.toDoubleOrNull() ?: return null
    val longitude = longitudeRaw?.toDoubleOrNull() ?: return null
    val accuracy = accuracyRaw?.toFloatOrNull() ?: DEFAULT_DEBUG_ACCURACY_METERS
    return UserLocation(latitude = latitude, longitude = longitude, accuracyMeters = accuracy)
}
