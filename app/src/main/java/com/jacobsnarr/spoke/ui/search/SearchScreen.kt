package com.jacobsnarr.spoke.ui.search

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jacobsnarr.spoke.ui.components.CenteredMessage
import com.jacobsnarr.spoke.ui.components.LoadingState
import com.jacobsnarr.spoke.ui.components.SpokeTopBar
import com.jacobsnarr.spoke.ui.components.StationRow
import com.jacobsnarr.spoke.ui.rememberAppContainer
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.progress_indicator.CircularProgressIndicatorMMD
import com.mudita.mmd.components.search_bar.SearchBarDefaultsMMD
import com.mudita.mmd.components.text.TextMMD

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(onStationClick: (Int) -> Unit, onBack: () -> Unit, reserveBottomInset: Boolean = true) {
    val container = rememberAppContainer()
    val viewModel: SearchViewModel = viewModel(factory = SearchViewModel.provideFactory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val unitSystem by container.preferencesStore.unitSystem.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .then(if (reserveBottomInset) Modifier.navigationBarsPadding() else Modifier),
    ) {
        SpokeTopBar(
            title = {
                SearchPill(
                    query = state.query,
                    onQueryChange = viewModel::onQueryChange,
                    focusRequester = focusRequester,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(28.dp))
                }
            },
            actions = {
                IconButton(
                    onClick = { viewModel.refresh() },
                    enabled = !state.isLoading,
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicatorMMD(size = 20.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh search data",
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            },
        )

        when {
            state.isLoading -> LoadingState()
            state.error != null -> CenteredMessage(text = state.error!!)
            state.query.isBlank() -> CenteredMessage(text = "Type to search for a station.")
            state.results.isEmpty() -> CenteredMessage(text = "No stations match \"${state.query}\".")
            else ->
                LazyColumnMMD(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    items(
                        count = state.results.size,
                        key = { index -> state.results[index].id },
                    ) { index ->
                        val station = state.results[index]
                        StationRow(
                            station = station,
                            unitSystem = unitSystem,
                            onClick = { onStationClick(station.id) },
                            highlightQuery = state.query,
                        )
                    }
                }
        }
    }
}

/**
 * A compact rounded search field for the top app bar. Uses MMD's own pill shape and border
 * tokens around a [BasicTextField] (which is what MMD's search InputField wraps), so we get the
 * Mudita look without the M3 InputField's fixed 56dp height.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchPill(query: String, onQueryChange: (String) -> Unit, focusRequester: FocusRequester) {
    Box(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(SearchBarDefaultsMMD.inputFieldBorder, SearchBarDefaultsMMD.inputFieldShape)
            .clip(SearchBarDefaultsMMD.inputFieldShape),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier =
            Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            singleLine = true,
            textStyle = TextStyle(color = Color.Black, fontSize = 18.sp),
            cursorBrush = SolidColor(Color.Black),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (query.isEmpty()) {
                            TextMMD(
                                "Search",
                                fontSize = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    }
                }
            },
        )
    }
}
