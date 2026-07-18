package com.jacobsnarr.spoke.data.remote

import com.jacobsnarr.spoke.data.remote.dto.CheckoutRequestDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface CheckoutApi {
    /**
     * Requests a bike checkout (unlock). The checkout host differs per bikeshare system, so the
     * full [url] is supplied at call time. The success response format is not yet documented, and
     * energy-saving stations can respond with `["CheckoutDeclined"]`, so we return the raw
     * [Response] and let the repository inspect the body defensively.
     */
    @POST
    suspend fun checkout(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body body: CheckoutRequestDto,
        @Query("programId") programId: Int,
    ): Response<ResponseBody>
}
