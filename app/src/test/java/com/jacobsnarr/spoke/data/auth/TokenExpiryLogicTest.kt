package com.jacobsnarr.spoke.data.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TokenExpiryLogicTest {
    @Test
    fun `missing access token is treated as expired`() {
        assertTrue(
            isAccessTokenExpired(
                accessToken = null,
                expiresAtMillis = 10_000L,
                nowMillis = 1_000L,
                bufferMillis = 0L,
            ),
        )
    }

    @Test
    fun `token expires when now reaches expiresAt minus buffer`() {
        assertTrue(
            isAccessTokenExpired(
                accessToken = "token",
                expiresAtMillis = 10_000L,
                nowMillis = 9_500L,
                bufferMillis = 500L,
            ),
        )
    }

    @Test
    fun `token not expired before buffer threshold`() {
        assertFalse(
            isAccessTokenExpired(
                accessToken = "token",
                expiresAtMillis = 10_000L,
                nowMillis = 9_499L,
                bufferMillis = 500L,
            ),
        )
    }
}
