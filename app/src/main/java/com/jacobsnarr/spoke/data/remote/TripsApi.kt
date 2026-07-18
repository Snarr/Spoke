package com.jacobsnarr.spoke.data.remote

import com.jacobsnarr.spoke.data.remote.dto.TripDto
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.http.Url

interface TripsApi {
    /**
     * Returns the signed-in user's trips for a given [month]/[year]. The trips host differs per
     * bikeshare system, so the full [url] is supplied at call time.
     */
    @GET
    suspend fun getTrips(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Query("month") month: Int,
        @Query("year") year: Int,
    ): List<TripDto>
}
