package com.jacobsnarr.spoke.ui.ride

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jacobsnarr.spoke.data.ride.RideState
import com.jacobsnarr.spoke.ui.components.ListItemDivider
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.eInkTypography

/**
 * Slim, tappable ride indicator shown above the bottom nav bar while a ride is active, so the rider
 * can keep an eye on their ride while browsing stations for open docks. Tapping opens the full ride
 * screen. Kept static (no animation) for the e-ink display.
 *
 * @param applyNavBarPadding reserve the system-nav inset when there is no bottom bar beneath the
 * banner to reserve it (e.g. on the station detail / search screens).
 */
@Composable
fun RideBanner(state: RideState.Active, applyNavBarPadding: Boolean, onClick: () -> Unit) {
    val elapsedSeconds = rememberElapsedSeconds(state.startedAtMillis)

    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
    ) {
        ListItemDivider()
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .then(if (applyNavBarPadding) Modifier.navigationBarsPadding() else Modifier)
                .height(44.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                contentDescription = null,
            )
            Spacer(Modifier.width(12.dp))
            TextMMD(text = "Ride in progress")
            Spacer(Modifier.weight(1f))
            TextMMD(text = formatElapsed(elapsedSeconds), style = eInkTypography.titleMedium)
        }
    }
}
