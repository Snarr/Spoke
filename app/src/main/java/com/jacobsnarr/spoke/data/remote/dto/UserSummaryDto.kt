package com.jacobsnarr.spoke.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Response from the `/user/summary` endpoint. Surfaces lifetime ride stats for the signed-in user.
 * All numeric fields default to zero so a partially-populated response doesn't crash the parser.
 */
@Serializable
data class UserSummaryDto(
    val milesRidden: Double = 0.0,
    val moneySaved: Double = 0.0,
    val caloriesBurned: Double = 0.0,
    val carbonOffset: Double = 0.0,
    val membershipType: String? = null,
    val memberSince: String? = null,
    val membershipExpiration: String? = null,
)
