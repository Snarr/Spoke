package com.jacobsnarr.spoke.data.remote

import com.jacobsnarr.spoke.data.remote.dto.UserSummaryDto
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

interface SummaryApi {
    /**
     * Returns the signed-in user's lifetime ride summary. The host differs per bikeshare system,
     * so the full [url] is supplied at call time.
     */
    @GET
    suspend fun getSummary(@Url url: String, @Header("Authorization") authorization: String): UserSummaryDto
}
