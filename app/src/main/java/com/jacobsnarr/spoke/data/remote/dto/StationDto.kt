package com.jacobsnarr.spoke.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StationCollectionDto(
    @SerialName("last_updated") val lastUpdated: String? = null,
    val features: List<StationFeatureDto> = emptyList(),
    val type: String? = null,
)

@Serializable
data class StationFeatureDto(val geometry: GeometryDto? = null, val properties: StationPropertiesDto, val type: String? = null)

@Serializable
data class GeometryDto(val coordinates: List<Double> = emptyList(), val type: String? = null)

@Serializable
data class StationPropertiesDto(
    val id: Int,
    val name: String,
    val coordinates: List<Double> = emptyList(),
    val totalDocks: Int = 0,
    val docksAvailable: Int = 0,
    val bikesAvailable: Int = 0,
    val classicBikesAvailable: Int = 0,
    val smartBikesAvailable: Int = 0,
    val electricBikesAvailable: Int = 0,
    val rewardBikesAvailable: Int = 0,
    val rewardDocksAvailable: Int = 0,
    val kioskStatus: String? = null,
    val kioskPublicStatus: String? = null,
    val kioskType: Int? = null,
    val addressStreet: String? = null,
    val addressCity: String? = null,
    val addressState: String? = null,
    val addressZipCode: String? = null,
    val bikes: List<BikeDto> = emptyList(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
)

@Serializable
data class BikeDto(val dockNumber: Int, val isElectric: Boolean = false, val isAvailable: Boolean = false, val battery: Int? = null)
