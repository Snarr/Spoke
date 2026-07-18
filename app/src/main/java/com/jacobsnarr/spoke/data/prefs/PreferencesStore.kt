package com.jacobsnarr.spoke.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lightweight store for non-sensitive UI preferences (plain [SharedPreferences], no encryption
 * needed). Tracks whether the user opted in to sorting stations by their location, whether the
 * first-launch location prompt has been shown, the preferred [UnitSystem] for distances, and
 * per-system favorite station ids.
 */
class PreferencesStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    var locationSortEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCATION_SORT_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_LOCATION_SORT_ENABLED, value) }

    /** Whether the one-time first-launch location prompt has already been shown. */
    var locationPromptShown: Boolean
        get() = prefs.getBoolean(KEY_LOCATION_PROMPT_SHOWN, false)
        set(value) = prefs.edit { putBoolean(KEY_LOCATION_PROMPT_SHOWN, value) }

    /** Id of the last-selected bikeshare system (see [com.jacobsnarr.spoke.data.system.BikeSystems]). */
    var selectedSystemId: String?
        get() = prefs.getString(KEY_SELECTED_SYSTEM_ID, null)
        set(value) = prefs.edit { putString(KEY_SELECTED_SYSTEM_ID, value) }

    private val _unitSystem =
        MutableStateFlow(
            UnitSystem.fromName(prefs.getString(KEY_UNIT_SYSTEM, null)),
        )

    /** Reactive stream of the preferred [UnitSystem] so screens update when it changes. */
    val unitSystem: StateFlow<UnitSystem> = _unitSystem.asStateFlow()

    fun setUnitSystem(value: UnitSystem) {
        prefs.edit { putString(KEY_UNIT_SYSTEM, value.name) }
        _unitSystem.value = value
    }

    fun getFavoriteStationIds(systemId: String): Set<Int> = parseFavoriteStationIds(prefs.getString(favoritesKey(systemId), null).orEmpty())

    fun isFavoritedStation(systemId: String, stationId: Int): Boolean = getFavoriteStationIds(systemId).contains(stationId)

    /** Returns the new favorited state after toggling. */
    fun toggleFavoriteStation(systemId: String, stationId: Int): Boolean {
        val updatedIds = toggledFavoriteStationIds(getFavoriteStationIds(systemId), stationId)
        prefs.edit { putString(favoritesKey(systemId), encodeFavoriteStationIds(updatedIds)) }
        return updatedIds.contains(stationId)
    }

    private fun favoritesKey(systemId: String): String = "$KEY_FAVORITE_STATIONS_PREFIX$systemId"

    private companion object {
        const val PREFS_FILE = "indego_prefs"
        const val KEY_LOCATION_SORT_ENABLED = "location_sort_enabled"
        const val KEY_LOCATION_PROMPT_SHOWN = "location_prompt_shown"
        const val KEY_SELECTED_SYSTEM_ID = "selected_system_id"
        const val KEY_UNIT_SYSTEM = "unit_system"
        const val KEY_FAVORITE_STATIONS_PREFIX = "favorite_stations_"
    }
}

internal fun parseFavoriteStationIds(raw: String): Set<Int> = raw
    .split(',')
    .asSequence()
    .map { it.trim() }
    .filter { it.isNotBlank() }
    .mapNotNull { it.toIntOrNull() }
    .toSet()

internal fun encodeFavoriteStationIds(ids: Set<Int>): String = ids.sorted().joinToString(",")

internal fun toggledFavoriteStationIds(current: Set<Int>, stationId: Int): Set<Int> = if (current.contains(stationId)) {
    current - stationId
} else {
    current + stationId
}
