package com.jacobsnarr.spoke.data.auth

import retrofit2.HttpException
import java.io.IOException

internal enum class RefreshFailureType {
    FatalAuth,
    Transient,
}

internal fun classifyRefreshFailure(error: Throwable): RefreshFailureType {
    if (error is HttpException) {
        return classifyHttpFailure(error)
    }
    if (error is IOException) {
        return RefreshFailureType.Transient
    }
    return RefreshFailureType.Transient
}

private fun classifyHttpFailure(error: HttpException): RefreshFailureType {
    val statusCode = error.code()
    return when {
        statusCode == HTTP_UNAUTHORIZED || statusCode == HTTP_FORBIDDEN -> RefreshFailureType.FatalAuth
        statusCode == HTTP_TOO_MANY_REQUESTS || statusCode >= HTTP_SERVER_ERROR_START -> RefreshFailureType.Transient
        statusCode == HTTP_BAD_REQUEST && errorBodyContainsInvalidAuth(error) -> RefreshFailureType.FatalAuth
        else -> RefreshFailureType.Transient
    }
}

private fun errorBodyContainsInvalidAuth(error: HttpException): Boolean {
    val body =
        runCatching {
            error.response()?.errorBody()?.string()
        }.getOrNull()
            ?.lowercase()
            ?: return false

    return body.contains("invalid_grant") || body.contains("invalid_token")
}

private const val HTTP_BAD_REQUEST = 400
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
private const val HTTP_TOO_MANY_REQUESTS = 429
private const val HTTP_SERVER_ERROR_START = 500
