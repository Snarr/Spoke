package com.jacobsnarr.spoke.ui.navigation

object Routes {
    const val SYSTEM_SELECT = "system_select"
    const val SWITCH_SYSTEM = "switch_system"
    const val STATIONS = "stations"
    const val SETTINGS = "settings"
    const val SETTINGS_ACCOUNT = "settings/account"
    const val SETTINGS_UNITS = "settings/units"
    const val SETTINGS_DEBUG = "settings/debug"
    const val RIDE = "ride"
    const val SEARCH = "search"

    const val MY_RIDES = "my_rides"
    const val LIFETIME_STATS = "my_rides/lifetime_stats"
    const val TRIP_HISTORY = "my_rides/trips"
    const val TRIP_DETAILS = "my_rides/trips/{tripId}"
    const val UNPAID_FEES = "my_rides/unpaid_fees"
    const val ARG_TRIP_ID = "tripId"

    const val STATION_DETAIL = "station/{stationId}"

    fun stationDetail(stationId: Int): String = "station/$stationId"

    const val ARG_STATION_ID = "stationId"

    const val STATION_COMPASS = "station/{stationId}/compass"

    fun stationCompass(stationId: Int): String = "station/$stationId/compass"

    fun tripDetails(tripId: Long): String = "my_rides/trips/$tripId"
}
