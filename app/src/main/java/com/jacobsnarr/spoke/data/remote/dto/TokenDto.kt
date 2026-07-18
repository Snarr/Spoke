package com.jacobsnarr.spoke.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long = 3600,
    @SerialName("token_type") val tokenType: String? = null,
    @SerialName("scope") val scope: String? = null,
)
