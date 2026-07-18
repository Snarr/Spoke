package com.jacobsnarr.spoke.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.jacobsnarr.spoke.di.AppContainer
import com.jacobsnarr.spoke.ui.components.BottomBarItem
import com.jacobsnarr.spoke.ui.ride.RideTripPoller

@Composable
internal fun SpokeAppScaffold(
    container: AppContainer,
    navController: NavHostController,
    startDestination: String,
    bottomTabs: List<BottomBarItem>,
    isDebuggable: Boolean,
) {
    val resumeScope = rememberCoroutineScope()
    val rideState by container.rideRepository.rideState.collectAsStateWithLifecycle()
    val isOffline by container.connectivityRepository.isOffline.collectAsStateWithLifecycle()
    val debugMenuVisible by container.debugSettingsRepository.debugMenuVisible.collectAsStateWithLifecycle()
    val debugCompassEnabled by container.debugSettingsRepository.debugCompassEnabled.collectAsStateWithLifecycle()

    RideTripPoller()
    HandleRideStateEffects(
        container = container,
        rideState = rideState,
        resumeScope = resumeScope,
    )
    HandleResumeSyncEffect(
        container = container,
        resumeScope = resumeScope,
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            SpokeBottomBarContent(
                container = container,
                navController = navController,
                bottomTabs = bottomTabs,
                rideState = rideState,
                isOffline = isOffline,
                resumeScope = resumeScope,
            )
        },
    ) { innerPadding ->
        SpokeNavGraph(
            container = container,
            navController = navController,
            startDestination = startDestination,
            isDebuggable = isDebuggable,
            debugMenuVisible = debugMenuVisible,
            debugCompassEnabled = debugCompassEnabled,
            rideState = rideState,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
