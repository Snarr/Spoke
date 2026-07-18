package com.jacobsnarr.spoke.data.remote

import com.jacobsnarr.spoke.data.remote.dto.StationCollectionDto
import retrofit2.http.GET
import retrofit2.http.Path

interface StationStatusApi {
    @GET("{system}")
    suspend fun getStations(@Path("system") system: String): StationCollectionDto
}
