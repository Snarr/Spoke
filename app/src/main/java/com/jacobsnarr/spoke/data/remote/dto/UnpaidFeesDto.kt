package com.jacobsnarr.spoke.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Response from the `/user/unpaid-fees` endpoint. The full response shape is not yet confirmed;
 * this is a placeholder that will be enriched once a real response is available.
 */
@Serializable
data class UnpaidFeesDto(val totalOwed: Double = 0.0)
