package com.jacobsnarr.spoke.data.remote

import com.jacobsnarr.spoke.data.remote.dto.UnpaidFeesDto
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

interface UnpaidFeesApi {
    /**
     * Returns the signed-in user's unpaid fees. The host differs per bikeshare system, so the full
     * [url] is supplied at call time. The response shape is a placeholder — update [UnpaidFeesDto]
     * once a real response is confirmed.
     */
    @GET
    suspend fun getUnpaidFees(@Url url: String, @Header("Authorization") authorization: String): UnpaidFeesDto
}
