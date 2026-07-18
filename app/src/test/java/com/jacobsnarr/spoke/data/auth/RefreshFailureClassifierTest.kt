package com.jacobsnarr.spoke.data.auth

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class RefreshFailureClassifierTest {
    @Test
    fun `classifies unknown host as transient`() {
        assertEquals(RefreshFailureType.Transient, classifyRefreshFailure(UnknownHostException("dns")))
    }

    @Test
    fun `classifies timeout as transient`() {
        assertEquals(RefreshFailureType.Transient, classifyRefreshFailure(SocketTimeoutException("timeout")))
    }

    @Test
    fun `classifies 401 as fatal auth`() {
        assertEquals(RefreshFailureType.FatalAuth, classifyRefreshFailure(httpException(401)))
    }

    @Test
    fun `classifies invalid grant body as fatal auth`() {
        val errorBody = "{\"error\":\"invalid_grant\"}"
        assertEquals(RefreshFailureType.FatalAuth, classifyRefreshFailure(httpException(400, errorBody)))
    }

    @Test
    fun `classifies invalid token body as fatal auth`() {
        val errorBody = "{\"error\":\"invalid_token\"}"
        assertEquals(RefreshFailureType.FatalAuth, classifyRefreshFailure(httpException(400, errorBody)))
    }

    @Test
    fun `classifies 500 as transient`() {
        assertEquals(RefreshFailureType.Transient, classifyRefreshFailure(httpException(500)))
    }

    private fun httpException(code: Int, body: String = ""): HttpException {
        val responseBody = body.toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Any>(code, responseBody))
    }
}
