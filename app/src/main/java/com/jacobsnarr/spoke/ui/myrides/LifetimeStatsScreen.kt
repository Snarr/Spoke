package com.jacobsnarr.spoke.ui.myrides

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Co2
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jacobsnarr.spoke.data.prefs.UnitSystem
import com.jacobsnarr.spoke.data.remote.dto.UserSummaryDto
import com.jacobsnarr.spoke.ui.components.CenteredMessage
import com.jacobsnarr.spoke.ui.components.ListItemDivider
import com.jacobsnarr.spoke.ui.components.ListRowHeight
import com.jacobsnarr.spoke.ui.components.LoadingState
import com.jacobsnarr.spoke.ui.components.SpokeTopBar
import com.jacobsnarr.spoke.ui.components.formatDistance
import com.jacobsnarr.spoke.ui.rememberAppContainer
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.progress_indicator.CircularProgressIndicatorMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.eInkTypography
import kotlin.math.roundToInt

@Composable
fun LifetimeStatsScreen(onBack: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel: MyRidesViewModel = viewModel(factory = MyRidesViewModel.provideFactory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val unitSystem by viewModel.unitSystem.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        SpokeTopBar(
            title = "Lifetime Stats",
            navigationIcon = {
                androidx.compose.material3.IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(28.dp),
                    )
                }
            },
        )

        when {
            !state.isLoggedIn -> CenteredMessage("Log in to see your rides.")
            !state.isSupported -> CenteredMessage("${container.systemRepository.current.displayName} doesn't support ride history yet.")
            state.isLoading -> LoadingState()
            state.error != null -> CenteredMessage(state.error!!)
            else ->
                MyRidesStatsList(
                    summary = state.summary,
                    unitSystem = unitSystem,
                )
        }
    }
}

@Composable
private fun MyRidesStatsList(summary: UserSummaryDto?, unitSystem: UnitSystem) {
    LazyColumnMMD(modifier = Modifier.fillMaxSize()) {
        item {
            StatRow(
                icon = Icons.AutoMirrored.Filled.DirectionsBike,
                label = "Miles Ridden",
                value = summary?.let { formatMilesRidden(it.milesRidden, unitSystem) },
            )
        }
        item {
            StatRow(
                icon = Icons.Filled.Savings,
                label = "Money Saved",
                value = summary?.let { "$${it.moneySaved.roundToInt()}" },
            )
        }
        item {
            StatRow(
                icon = Icons.Filled.FitnessCenter,
                label = "Calories Burned",
                value = summary?.let { "${it.caloriesBurned.roundToInt()} kcal" },
            )
        }
        item {
            StatRow(
                icon = Icons.Filled.Co2,
                label = "Carbon Offset",
                value = summary?.let { "${it.carbonOffset.roundToInt()} lbs" },
            )
        }
    }
}

@Composable
private fun StatRow(icon: ImageVector, label: String, value: String?) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ListRowHeight)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(16.dp))
                TextMMD(text = label, style = eInkTypography.bodyLarge)
            }
            if (value != null) {
                TextMMD(text = value, style = eInkTypography.bodyLarge)
            } else {
                CircularProgressIndicatorMMD(size = 16.dp)
            }
        }
        ListItemDivider()
    }
}

private fun formatMilesRidden(miles: Double, unitSystem: UnitSystem): String {
    val meters = miles * 1609.344
    return formatDistance(meters, unitSystem)
}
