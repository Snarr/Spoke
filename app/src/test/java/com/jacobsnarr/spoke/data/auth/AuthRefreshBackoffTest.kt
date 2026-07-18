package com.jacobsnarr.spoke.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRefreshBackoffTest {
    @Test
    fun `compute backoff doubles per transient failure`() {
        val first =
            computeNextRefreshBackoff(
                failureCount = 0,
                nowMillis = 1000L,
                baseBackoffMillis = 5_000L,
                maxBackoffMillis = 300_000L,
                maxBackoffPower = 6,
            )
        val second =
            computeNextRefreshBackoff(
                failureCount = first.failureCount,
                nowMillis = 1000L,
                baseBackoffMillis = 5_000L,
                maxBackoffMillis = 300_000L,
                maxBackoffPower = 6,
            )

        assertEquals(1, first.failureCount)
        assertEquals(6_000L, first.nextAttemptAtMillis)
        assertEquals(2, second.failureCount)
        assertEquals(11_000L, second.nextAttemptAtMillis)
    }

    @Test
    fun `compute backoff caps by max power and max duration`() {
        val capped =
            computeNextRefreshBackoff(
                failureCount = 99,
                nowMillis = 1000L,
                baseBackoffMillis = 5_000L,
                maxBackoffMillis = 300_000L,
                maxBackoffPower = 6,
            )

        assertEquals(6, capped.failureCount)
        assertEquals(161_000L, capped.nextAttemptAtMillis)
    }

    @Test
    fun `backoff active compares now with next attempt`() {
        assertTrue(isRefreshBackoffActive(nowMillis = 1000L, nextAttemptAtMillis = 1500L))
        assertFalse(isRefreshBackoffActive(nowMillis = 1500L, nextAttemptAtMillis = 1500L))
    }
}
