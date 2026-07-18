package com.jacobsnarr.spoke.ui.myrides

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jacobsnarr.spoke.ui.components.CenteredMessage
import com.jacobsnarr.spoke.ui.components.SettingsMenuRow
import com.jacobsnarr.spoke.ui.components.SpokeTopBar
import com.jacobsnarr.spoke.ui.rememberAppContainer
import com.mudita.mmd.components.lazy.LazyColumnMMD

@Composable
fun MyRidesScreen(onOpenLifetimeStats: () -> Unit, onOpenTripHistory: () -> Unit, onOpenUnpaidFees: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel: MyRidesViewModel = viewModel(factory = MyRidesViewModel.provideFactory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        SpokeTopBar(title = "My Rides")

        when {
            !state.isLoggedIn -> CenteredMessage("Log in to see your rides.")
            !state.isSupported -> CenteredMessage("${container.systemRepository.current.displayName} doesn't support ride history yet.")
            else ->
                LazyColumnMMD(modifier = Modifier.fillMaxSize()) {
                    item {
                        SettingsMenuRow(
                            icon = Icons.Filled.BarChart,
                            title = "Lifetime Stats",
                            onClick = onOpenLifetimeStats,
                        )
                    }
                    item {
                        SettingsMenuRow(
                            icon = Icons.Filled.History,
                            title = "Trip History",
                            onClick = onOpenTripHistory,
                        )
                    }
                    item {
                        SettingsMenuRow(
                            icon = Icons.Filled.Payment,
                            title = "Unpaid Fees",
                            onClick = onOpenUnpaidFees,
                        )
                    }
                }
        }
    }
}
