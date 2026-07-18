package com.jacobsnarr.spoke.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.jacobsnarr.spoke.data.ride.RideState
import com.jacobsnarr.spoke.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun HandleRideStateEffects(container: AppContainer, rideState: RideState, resumeScope: CoroutineScope) {
    val previousRideState = remember { mutableStateOf<RideState?>(null) }

    LaunchedEffect(rideState) {
        val previous = previousRideState.value
        if (previous is RideState.Active && rideState is RideState.NoActiveRide) {
            container.myRidesRepository.invalidateCurrentSystemCache()
            if (container.authRepository.isCurrentlyLoggedIn()) {
                resumeScope.launch {
                    container.myRidesRepository.prefetchCurrentMonthData()
                }
            }
        }
        previousRideState.value = rideState
    }
}

@Composable
internal fun HandleResumeSyncEffect(container: AppContainer, resumeScope: CoroutineScope) {
    LifecycleResumeEffect(Unit) {
        val job =
            resumeScope.launch {
                if (container.authRepository.isCurrentlyLoggedIn()) {
                    val token = container.authRepository.ensureValidToken()
                    container.rideRepository.syncFromTrips(token)
                }
            }
        onPauseOrDispose { job.cancel() }
    }
}
