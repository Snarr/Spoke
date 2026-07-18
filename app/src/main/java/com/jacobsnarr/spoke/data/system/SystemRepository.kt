package com.jacobsnarr.spoke.data.system

import com.jacobsnarr.spoke.data.prefs.PreferencesStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the currently selected bikeshare system and persists the choice so it survives app
 * restarts (needed for token refresh and station fetches after relaunch).
 */
class SystemRepository(private val preferencesStore: PreferencesStore) {
    private val _currentSystem =
        MutableStateFlow(
            BikeSystems.fromId(preferencesStore.selectedSystemId),
        )
    val currentSystem: StateFlow<BikeSystem> = _currentSystem.asStateFlow()

    val current: BikeSystem
        get() = _currentSystem.value

    /** True once the user has explicitly chosen a system (i.e. completed first-launch onboarding). */
    val hasSelectedSystem: Boolean
        get() = preferencesStore.selectedSystemId != null

    fun select(system: BikeSystem) {
        preferencesStore.selectedSystemId = system.id
        _currentSystem.value = system
    }
}
