package com.jacobsnarr.spoke.ui.settings

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
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jacobsnarr.spoke.ui.components.ListItemDivider
import com.jacobsnarr.spoke.ui.components.ListRowHeight
import com.jacobsnarr.spoke.ui.components.SettingsDetailRow
import com.jacobsnarr.spoke.ui.components.SettingsMenuRow
import com.jacobsnarr.spoke.ui.components.SpokeTopBar
import com.jacobsnarr.spoke.ui.rememberAppContainer
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.eInkTypography

@Composable
fun SettingsScreen(
    onOpenAccount: () -> Unit,
    onOpenUnits: () -> Unit,
    onSwitchSystem: () -> Unit,
    onOpenDebug: () -> Unit,
    showDebug: Boolean,
) {
    val container = rememberAppContainer()
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.provideFactory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        SpokeTopBar(title = "Settings")

        // All rows share the ListRowHeight grid so their dividers line up with the Stations list —
        // switching tabs then moves as little content as possible (E Ink "Follow the Lines").
        LazyColumnMMD(modifier = Modifier.fillMaxSize()) {
            item {
                ConnectedSystemRow(
                    systemName = state.system.displayName,
                    onSwitch = onSwitchSystem,
                )
            }
            item {
                SettingsMenuRow(
                    icon = Icons.Filled.Person,
                    title = "Account",
                    onClick = onOpenAccount,
                )
            }
            item {
                SettingsMenuRow(
                    icon = Icons.Filled.Straighten,
                    title = "Units",
                    onClick = onOpenUnits,
                )
            }
            item {
                SettingsDetailRow(
                    icon = Icons.Outlined.Info,
                    title = "Version",
                    detail = state.appVersion,
                )
            }
            if (showDebug) {
                item {
                    SettingsMenuRow(
                        icon = Icons.Filled.BugReport,
                        title = "Debug",
                        onClick = onOpenDebug,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectedSystemRow(systemName: String, onSwitch: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .height(ListRowHeight)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                TextMMD(text = "Riding with", style = eInkTypography.bodySmall)
                TextMMD(
                    text = systemName,
                    style = eInkTypography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            ButtonMMD(onClick = onSwitch) {
                TextMMD("Switch")
            }
        }
        ListItemDivider()
    }
}
