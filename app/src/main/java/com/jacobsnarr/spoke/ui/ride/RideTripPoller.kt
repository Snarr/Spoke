package com.jacobsnarr.spoke.ui.ride

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.jacobsnarr.spoke.ui.rememberAppContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.seconds

private val POLL_INTERVAL = 30.seconds

/**
 * Drives ride-session tracking while the app is in the foreground. Whenever the user is signed in
 * and the screen is resumed, it polls the `/user/trips` endpoint on login/resume and then every
 * [POLL_INTERVAL], letting [com.jacobsnarr.spoke.data.ride.RideRepository.syncFromTrips]
 * adopt server-started rides and end the session once no active trip remains.
 *
 * Polling is scoped to the RESUMED lifecycle state, so it stops when the app is backgrounded — the
 * app never tracks rides in the background.
 */
@Composable
fun RideTripPoller() {
    val container = rememberAppContainer()
    val lifecycleOwner = LocalLifecycleOwner.current
    val isLoggedIn by container.authRepository.isLoggedIn.collectAsStateWithLifecycle()

    if (!isLoggedIn) {
        return
    }

    androidx.compose.runtime.LaunchedEffect(isLoggedIn) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                val token = container.authRepository.ensureValidToken()
                Log.d("RideTripPoller", "RideTripPoller: Polling syncFromTrips")
                container.rideRepository.syncFromTrips(token)
                delay(POLL_INTERVAL)
            }
        }
    }
}
