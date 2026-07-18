package com.jacobsnarr.spoke.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CheckoutRequestDto(val dockNumber: Int, val stationId: Int, val systemId: String)
