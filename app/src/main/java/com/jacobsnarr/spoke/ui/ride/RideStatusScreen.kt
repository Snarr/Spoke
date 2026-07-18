package com.jacobsnarr.spoke.ui.ride

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacobsnarr.spoke.data.ride.RideState
import com.jacobsnarr.spoke.ui.components.SpokeTopBar
import com.jacobsnarr.spoke.ui.rememberAppContainer
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.eInkTypography

@Composable
fun RideStatusScreen(onBack: () -> Unit) {
    val container = rememberAppContainer()
    val rideState by container.rideRepository.rideState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
        SpokeTopBar(
            title = "Your ride",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(28.dp))
                }
            },
        )

        when (val state = rideState) {
            RideState.NoActiveRide ->
                Column(
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TextMMD(text = "No active ride", style = eInkTypography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    TextMMD(
                        text = "Unlock a bike from a station to start a ride.",
                        textAlign = TextAlign.Center,
                    )
                }

            is RideState.Active -> ActiveRideContent(state = state)
        }
    }
}

@Composable
private fun ActiveRideContent(state: RideState.Active) {
    val elapsedSeconds = rememberElapsedSeconds(state.startedAtMillis)

    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextMMD(text = "Ride in progress", style = eInkTypography.titleLarge)
        Spacer(Modifier.height(16.dp))
        TextMMD(text = formatElapsed(elapsedSeconds), style = eInkTypography.headlineMedium)
        state.stationName?.let { stationName ->
            Spacer(Modifier.height(24.dp))
            TextMMD(text = "From $stationName", textAlign = TextAlign.Center)
        }
        state.bikeDockNumber?.let { dock ->
            Spacer(Modifier.height(4.dp))
            TextMMD(text = "Bike at dock #$dock")
        }
        Spacer(Modifier.height(32.dp))
        TextMMD(
            text = "Your ride ends automatically when you return the bike to a dock.",
            textAlign = TextAlign.Center,
        )
    }
}
