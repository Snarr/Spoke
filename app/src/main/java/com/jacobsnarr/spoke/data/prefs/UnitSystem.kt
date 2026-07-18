package com.jacobsnarr.spoke.data.prefs

/**
 * Measurement system used for displaying distances. Defaults to [IMPERIAL] because all supported
 * bikeshare systems are in the United States.
 */
enum class UnitSystem {
    IMPERIAL,
    METRIC,
    ;

    companion object {
        fun fromName(name: String?): UnitSystem = entries.firstOrNull { it.name == name } ?: IMPERIAL
    }
}
