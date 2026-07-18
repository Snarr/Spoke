package com.jacobsnarr.spoke.ui.myrides

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jacobsnarr.spoke.data.remote.dto.TripDto
import com.jacobsnarr.spoke.ui.components.CenteredMessage
import com.jacobsnarr.spoke.ui.components.ListItemDivider
import com.jacobsnarr.spoke.ui.components.ListRowHeight
import com.jacobsnarr.spoke.ui.components.LoadingState
import com.jacobsnarr.spoke.ui.components.SpokeTopBar
import com.jacobsnarr.spoke.ui.rememberAppContainer
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.progress_indicator.CircularProgressIndicatorMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.eInkTypography
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TripHistoryScreen(onBack: () -> Unit, onOpenTripDetails: (Long) -> Unit) {
    val container = rememberAppContainer()
    val viewModel: TripHistoryViewModel = viewModel(factory = TripHistoryViewModel.provideFactory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        SpokeTopBar(
            title = "Trip History",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(28.dp),
                    )
                }
            },
        )

        when {
            !state.isLoggedIn -> CenteredMessage("Log in to see your trips.")
            !state.isSupported -> CenteredMessage("Trip history isn't supported for this system.")
            state.isLoading -> LoadingState()
            state.error != null -> CenteredMessage(state.error!!)
            state.trips.isEmpty() -> CenteredMessage("No trips found for this month.")
            else ->
                TripList(
                    trips = state.trips,
                    isLoadingMore = state.isLoadingMore,
                    hasMoreMonths = state.hasMoreMonths,
                    onLoadMore = viewModel::loadMore,
                    onOpenTripDetails = onOpenTripDetails,
                )
        }
    }
}

@Composable
private fun TripList(
    trips: List<TripDto>,
    isLoadingMore: Boolean,
    hasMoreMonths: Boolean,
    onLoadMore: () -> Unit,
    onOpenTripDetails: (Long) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState, trips, isLoadingMore, hasMoreMonths) {
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            hasMoreMonths &&
                !isLoadingMore &&
                trips.isNotEmpty() &&
                lastVisible >= trips.lastIndex - LOAD_MORE_TRIGGER_THRESHOLD
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { onLoadMore() }
    }

    LazyColumnMMD(modifier = Modifier.fillMaxSize(), state = listState) {
        items(trips.size) { index ->
            TripRow(trip = trips[index], onClick = { onOpenTripDetails(trips[index].tripId) })
        }
        if (isLoadingMore) {
            item {
                LoadingMoreRow()
            }
        }
    }
}

@Composable
private fun TripRow(trip: TripDto, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ListRowHeight)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                TextMMD(
                    text = "From: ${trip.checkOutLocation ?: "Unknown"}",
                    style = eInkTypography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                TextMMD(
                    text = "To: ${trip.checkInLocation ?: "Unknown"}",
                    style = eInkTypography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.size(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextMMD(
                    text = formatTripDate(trip.checkOutDate),
                    style = eInkTypography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.size(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        ListItemDivider()
    }
}

private fun formatTripDate(checkOutDate: String?): String {
    if (checkOutDate == null) return "Unknown date"
    return runCatching {
        val instant = Instant.parse(checkOutDate)
        val local = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        local.format(DateTimeFormatter.ofPattern("MM/dd/yy", Locale.getDefault()))
    }.getOrDefault("Unknown date")
}

@Composable
private fun LoadingMoreRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ListRowHeight)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicatorMMD(size = 16.dp)
        Spacer(Modifier.size(12.dp))
        TextMMD(text = "Loading older trips…", style = eInkTypography.bodySmall)
    }
}

private const val LOAD_MORE_TRIGGER_THRESHOLD = 3
