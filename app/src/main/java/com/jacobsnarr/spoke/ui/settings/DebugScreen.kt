package com.jacobsnarr.spoke.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jacobsnarr.spoke.ui.components.SettingsMenuRow
import com.jacobsnarr.spoke.ui.components.SpokeTopBar
import com.jacobsnarr.spoke.ui.rememberAppContainer
import com.mudita.mmd.components.lazy.LazyColumnMMD

/**
 * Developer-only tools. Reached from Settings and only registered in debuggable builds, so it is
 * absent from production. Currently hosts the simulated-ride toggle used to exercise the ride UI
 * and lifecycle without an actual checkout.
 */
@Composable
fun DebugScreen(onBack: () -> Unit, onHideDebugMenuUntilRestart: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel: DebugViewModel = viewModel(factory = DebugViewModel.provideFactory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        SpokeTopBar(
            title = "Debug",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(28.dp))
                }
            },
        )
        LazyColumnMMD(modifier = Modifier.fillMaxSize()) {
            item {
                SettingsMenuRow(
                    icon = null,
                    title = if (state.rideActive) "End ride" else "Start ride",
                    onClick = viewModel::toggleRide,
                )
            }
            item {
                SettingsMenuRow(
                    icon = null,
                    title = if (state.usingTestLocation) "Use real location" else "Use test location",
                    onClick = viewModel::toggleTestLocation,
                )
            }
            item {
                SettingsMenuRow(
                    icon = null,
                    title = if (state.compassEnabled) "Hide compass" else "Show compass",
                    onClick = viewModel::toggleCompass,
                )
            }
            item {
                SettingsMenuRow(
                    icon = null,
                    title = if (state.dummyTripsEnabled) "Use real trip history" else "Use dummy trip history",
                    onClick = viewModel::toggleDummyTrips,
                )
            }
            state.statusMessage?.let { stationName ->
                item {
                    SettingsMenuRow(
                        icon = null,
                        title = "Ride from $stationName",
                        onClick = {},
                    )
                }
            }
            item {
                SettingsMenuRow(
                    icon = null,
                    title = "Hide debug menu until restart",
                    onClick = {
                        viewModel.hideDebugMenuUntilRestart()
                        onHideDebugMenuUntilRestart()
                    },
                )
            }
        }
    }
}
