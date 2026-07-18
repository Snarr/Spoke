package com.jacobsnarr.spoke.data.station.model

data class Station(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val totalDocks: Int,
    val docksAvailable: Int,
    val bikesAvailable: Int,
    val classicBikesAvailable: Int,
    val electricBikesAvailable: Int,
    val kioskStatus: String?,
    val kioskType: Int?,
    val addressStreet: String?,
    val addressCity: String?,
    val addressState: String?,
    val addressZipCode: String?,
    val bikes: List<Bike>,
    val distanceMeters: Double? = null,
) {
    /** Energy-saving stations require the rider to press the button on the bike dock to activate. */
    val isEnergySaving: Boolean
        get() = kioskType == ENERGY_SAVING_KIOSK_TYPE

    private companion object {
        const val ENERGY_SAVING_KIOSK_TYPE = 10
    }
}

data class Bike(val dockNumber: Int, val isElectric: Boolean, val isAvailable: Boolean, val battery: Int?)
