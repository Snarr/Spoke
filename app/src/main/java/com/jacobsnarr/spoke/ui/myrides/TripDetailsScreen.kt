package com.jacobsnarr.spoke.ui.myrides

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jacobsnarr.spoke.ui.components.CenteredMessage
import com.jacobsnarr.spoke.ui.components.ListItemDivider
import com.jacobsnarr.spoke.ui.components.ListRowHeight
import com.jacobsnarr.spoke.ui.components.LoadingState
import com.jacobsnarr.spoke.ui.components.SpokeTopBar
import com.jacobsnarr.spoke.ui.rememberAppContainer
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.eInkTypography

@Composable
fun TripDetailsScreen(tripId: Long, onBack: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel: TripDetailsViewModel = viewModel(factory = TripDetailsViewModel.provideFactory(container, tripId))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val trip = state.trip

    Column(modifier = Modifier.fillMaxSize()) {
        SpokeTopBar(
            title = "Trip Details",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(28.dp),
                    )
                }
            },
        )

        when {
            !state.isLoggedIn -> CenteredMessage("Log in to see trip details.")
            !state.isSupported -> CenteredMessage("Trip history isn't supported for this system.")
            state.isLoading -> LoadingState()
            state.error != null -> CenteredMessage(state.error!!)
            trip == null -> CenteredMessage("Trip not found.")
            else -> TripDetailsList(rows = buildTripDetailRows(trip))
        }
    }
}

@Composable
private fun TripDetailsList(rows: List<TripDetailRow>) {
    LazyColumnMMD(modifier = Modifier.fillMaxSize()) {
        items(rows.size) { index ->
            TripDetailRowItem(row = rows[index])
        }
    }
}

@Composable
private fun TripDetailRowItem(row: TripDetailRow) {
    val isStationRow = row.label == "From" || row.label == "To"
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isStationRow) Modifier.heightIn(min = ListRowHeight) else Modifier.height(ListRowHeight))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = row.icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            androidx.compose.foundation.layout.Spacer(Modifier.size(12.dp))
            TextMMD(
                text = row.label,
                style = eInkTypography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (isStationRow) Modifier.width(72.dp) else Modifier.weight(1f),
            )
            androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
            TextMMD(
                text = row.value,
                style = if (isStationRow) eInkTypography.bodySmall else eInkTypography.bodyLarge,
                maxLines = if (isStationRow) 2 else 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = if (isStationRow) Modifier.weight(1f) else Modifier,
            )
        }
        ListItemDivider()
    }
}
