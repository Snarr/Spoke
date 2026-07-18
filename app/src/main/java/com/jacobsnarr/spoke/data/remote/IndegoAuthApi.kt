package com.jacobsnarr.spoke.data.remote

import com.jacobsnarr.spoke.data.remote.dto.TokenDto
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface IndegoAuthApi {
    @FormUrlEncoded
    @POST("connect/token")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("acr_values") acrValues: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("scope") scope: String,
        @Field("grant_type") grantType: String = "password",
    ): TokenDto

    @FormUrlEncoded
    @POST("connect/token")
    suspend fun refresh(
        @Field("refresh_token") refreshToken: String,
        @Field("acr_values") acrValues: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("grant_type") grantType: String = "refresh_token",
    ): TokenDto
}
