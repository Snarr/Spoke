package com.jacobsnarr.spoke.ui.stations

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jacobsnarr.spoke.ui.components.CenteredMessage
import com.jacobsnarr.spoke.ui.components.LoadingState
import com.jacobsnarr.spoke.ui.components.SpokeTopBar
import com.jacobsnarr.spoke.ui.components.StationRow
import com.jacobsnarr.spoke.ui.rememberAppContainer
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.progress_indicator.CircularProgressIndicatorMMD

@Composable
fun StationsScreen(onStationClick: (Int) -> Unit, onSearch: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel: StationsViewModel = viewModel(factory = StationsViewModel.provideFactory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val unitSystem by container.preferencesStore.unitSystem.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    // Scroll to top whenever the station list is re-sorted (e.g. location refresh).
    LaunchedEffect(state.stations) {
        listState.scrollToItem(0)
    }

    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose {}
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (granted) viewModel.onLocationRequested()
        }

    // Spec: on first launch, prompt the user to share their location.
    LaunchedEffect(Unit) {
        if (viewModel.shouldPromptForLocation()) {
            viewModel.markLocationPromptShown()
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SpokeTopBar(
            title = "Stations",
            actions = {
                IconButton(onClick = viewModel::toggleFavoritesFilter) {
                    Icon(
                        imageVector = if (state.showFavoritesOnly) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (state.showFavoritesOnly) "Show all stations" else "Show favorite stations",
                        modifier = Modifier.size(28.dp),
                    )
                }
                IconButton(onClick = onSearch) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search stations",
                        modifier = Modifier.size(28.dp),
                    )
                }
                IconButton(
                    onClick = { viewModel.refresh() },
                    enabled = !state.isLoading,
                ) {
                    if (state.isLoading || state.locationLoading) {
                        CircularProgressIndicatorMMD(size = 20.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh stations",
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            },
        )

        when {
            state.isLoading && state.stations.isEmpty() -> LoadingState()
            state.error != null && state.stations.isEmpty() ->
                CenteredMessage(text = state.error!!)
            state.stations.isEmpty() ->
                CenteredMessage(text = if (state.showFavoritesOnly) "No favorite stations found." else "No stations found.")
            else ->
                LazyColumnMMD(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    items(
                        count = state.stations.size,
                        key = { index -> state.stations[index].id },
                    ) { index ->
                        val station = state.stations[index]
                        StationRow(
                            station = station,
                            unitSystem = unitSystem,
                            onClick = { onStationClick(station.id) },
                        )
                    }
                }
        }
    }
}
