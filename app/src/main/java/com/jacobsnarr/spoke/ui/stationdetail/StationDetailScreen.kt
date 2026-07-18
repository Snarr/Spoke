package com.jacobsnarr.spoke.ui.stationdetail

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jacobsnarr.spoke.data.prefs.UnitSystem
import com.jacobsnarr.spoke.data.station.model.Bike
import com.jacobsnarr.spoke.data.station.model.Station
import com.jacobsnarr.spoke.ui.components.CenteredMessage
import com.jacobsnarr.spoke.ui.components.ListItemDivider
import com.jacobsnarr.spoke.ui.components.LoadingState
import com.jacobsnarr.spoke.ui.components.SpokeTopBar
import com.jacobsnarr.spoke.ui.components.formatDistance
import com.jacobsnarr.spoke.ui.rememberAppContainer
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.progress_indicator.CircularProgressIndicatorMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.eInkTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@Composable
fun StationDetailScreen(
    stationId: Int,
    onBack: () -> Unit,
    onCompass: (() -> Unit)?,
    onUnlocked: () -> Unit,
    reserveBottomInset: Boolean = true,
) {
    val container = rememberAppContainer()
    val viewModel: StationDetailViewModel =
        viewModel(
            factory = StationDetailViewModel.provideFactory(container, stationId),
        )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .then(if (reserveBottomInset) Modifier.navigationBarsPadding() else Modifier),
    ) {
        SpokeTopBar(
            title = state.station?.name ?: "Station",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(28.dp))
                }
            },
            actions = {
                IconButton(onClick = viewModel::toggleFavorite) {
                    Icon(
                        imageVector = if (state.isFavorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (state.isFavorited) "Remove station from favorites" else "Add station to favorites",
                        modifier = Modifier.size(28.dp),
                    )
                }
            },
        )

        when {
            state.isLoading -> LoadingState()
            state.station == null -> CenteredMessage(text = state.error ?: "Station not found.")
            else ->
                StationDetailContent(
                    station = state.station!!,
                    checkoutSupported = state.checkoutSupported,
                    onBikeClick = viewModel::onBikeSelected,
                    onCompass = onCompass,
                )
        }
    }

    val pendingDock = state.pendingUnlockDock
    if (pendingDock != null) {
        val unitSystem by viewModel.unitSystem.collectAsStateWithLifecycle()
        CheckoutConfirmationDialog(
            viewModel = viewModel,
            state = state,
            pendingDock = pendingDock,
            unitSystem = unitSystem,
            onUnlocked = onUnlocked,
        )
    }
}

// Fixed-height label/value row used inside the checkout info card. A fixed height keeps the card
// from reflowing as GPS values fill in — important on E-ink.
private val DETAIL_ROW_HEIGHT = 44.dp

@Composable
private fun DetailRow(label: String, value: String, loading: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DETAIL_ROW_HEIGHT),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextMMD(text = label, style = eInkTypography.bodyMedium)
        if (loading) {
            CircularProgressIndicatorMMD(size = 16.dp)
        } else {
            TextMMD(text = value, style = eInkTypography.bodyMedium)
        }
    }
}

@Composable
private fun CheckoutConfirmationDialog(
    viewModel: StationDetailViewModel,
    state: StationDetailUiState,
    pendingDock: Int,
    unitSystem: UnitSystem,
    onUnlocked: () -> Unit,
) {
    val isEnergySaving = state.station?.isEnergySaving == true
    val station = state.station
    val userLocation = state.userLocation
    val distance = if (station != null && userLocation != null) {
        viewModel.distanceToStation(userLocation, station)
    } else {
        null
    }

    Dialog(
        onDismissRequest = viewModel::dismissUnlock,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        (LocalView.current.parent as? DialogWindowProvider)?.window?.apply {
            setDimAmount(0f)
            setWindowAnimations(0)
        }
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(24.dp),
            ) {
                TextMMD(text = "Unlock bike", style = eInkTypography.titleLarge)
                Spacer(Modifier.height(16.dp))

                val stationName = state.station?.name ?: "Unknown station"
                val distanceLoading =
                    distance == null && state.locationAccuracy == LocationAccuracyStatus.UNKNOWN
                val distanceValue = if (distance != null) formatDistance(distance, unitSystem) else "—"
                val accuracyValue =
                    if (userLocation?.accuracyMeters != null) {
                        "±${formatDistance(userLocation.accuracyMeters.toDouble(), unitSystem)}"
                    } else {
                        "—"
                    }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onBackground,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    TextMMD(
                        text = stationName,
                        style = eInkTypography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(8.dp))
                    ListItemDivider()
                    DetailRow(label = "Distance", value = distanceValue, loading = distanceLoading)
                    ListItemDivider()
                    DetailRow(label = "GPS accuracy", value = accuracyValue)
                }

                val warning =
                    when (state.locationAccuracy) {
                        LocationAccuracyStatus.POOR ->
                            "⚠️ Location accuracy is limited in this area. Please verify you're at the correct station."
                        LocationAccuracyStatus.UNAVAILABLE ->
                            "⚠️ Location unavailable. Try moving to an open area for better GPS signal."
                        else -> null
                    }
                if (warning != null) {
                    Spacer(Modifier.height(12.dp))
                    TextMMD(text = warning, style = eInkTypography.bodyMedium)
                }

                Spacer(Modifier.weight(1f))

                if (isEnergySaving) {
                    TextMMD(
                        text = "This is an energy-saving station — press the button on the bike dock " +
                            "to activate it before unlocking.",
                        style = eInkTypography.bodyMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                TextMMD(
                    text = "Request to unlock the bike at dock #$pendingDock?",
                    style = eInkTypography.bodyLarge,
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButtonMMD(onClick = viewModel::dismissUnlock) {
                        TextMMD("Cancel")
                    }
                    Spacer(Modifier.width(12.dp))
                    ButtonMMD(onClick = { viewModel.confirmUnlock(onUnlocked) }) {
                        TextMMD("Unlock")
                    }
                }
            }
        }
    }
}

@Composable
private fun StationDetailContent(station: Station, checkoutSupported: Boolean, onBikeClick: (Bike) -> Unit, onCompass: (() -> Unit)?) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var addressCopied by remember { mutableStateOf(false) }
    LaunchedEffect(addressCopied) {
        if (addressCopied) {
            delay(2.seconds)
            addressCopied = false
        }
    }
    LazyColumnMMD(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val cityState =
                        listOfNotNull(station.addressCity, station.addressState)
                            .joinToString(", ")
                            .takeIf { it.isNotBlank() }
                    if (station.addressStreet == null && cityState == null) {
                        TextMMD(text = station.name, style = eInkTypography.bodyMedium)
                    } else {
                        station.addressStreet?.let {
                            TextMMD(text = it, style = eInkTypography.bodyMedium)
                        }
                        cityState?.let {
                            TextMMD(text = it, style = eInkTypography.bodySmall)
                        }
                    }
                }
                if (onCompass != null) {
                    IconButton(onClick = onCompass) {
                        Icon(
                            imageVector = Icons.Filled.Explore,
                            contentDescription = "Compass to station",
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
                IconButton(onClick = {
                    val address = formatStationAddress(station)
                    scope.launch {
                        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Station address", address)))
                    }
                    addressCopied = true
                }) {
                    Icon(
                        imageVector = if (addressCopied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                        contentDescription = if (addressCopied) "Address copied" else "Copy address",
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            ListItemDivider()
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                TextMMD(text = "Bikes", style = eInkTypography.titleMedium)
            }
        }

        if (station.bikes.isEmpty()) {
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    TextMMD("No individual bike data available.")
                }
            }
        } else {
            items(
                count = station.bikes.size,
                key = { index -> station.bikes[index].dockNumber },
            ) { index ->
                BikeRow(
                    bike = station.bikes[index],
                    checkoutSupported = checkoutSupported,
                    onClick = onBikeClick,
                )
            }
        }
    }
}

private fun formatStationAddress(station: Station): String {
    val cityStateZip =
        listOfNotNull(
            station.addressCity,
            station.addressState,
            station.addressZipCode,
        ).joinToString(" ").takeIf { it.isNotBlank() }
    val address =
        listOfNotNull(station.addressStreet, cityStateZip)
            .joinToString(", ")
            .takeIf { it.isNotBlank() }
    return address ?: station.name
}

// Geo intents are temporarily disabled; kept for easy re-enable once they work again.
@Suppress("unused")
private fun openInMaps(context: Context, station: Station) {
    val label = Uri.encode(station.name)
    val geoUri = "geo:${station.latitude},${station.longitude}?q=${station.latitude},${station.longitude}($label)".toUri()
    val intent = Intent(Intent.ACTION_VIEW, geoUri)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No maps app installed to handle the geo: intent; nothing else to do.
    }
}

@Composable
private fun BikeRow(bike: Bike, checkoutSupported: Boolean, onClick: (Bike) -> Unit) {
    // Rule 2 (Avoid grey): keep pure black/white and convey unavailability with a
    // strikethrough label instead of dimming the row to grey.
    val contentColor = MaterialTheme.colorScheme.onSurface
    val textDecoration = if (bike.isAvailable) TextDecoration.None else TextDecoration.LineThrough
    val clickable = bike.isAvailable && checkoutSupported
    Column {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .then(if (clickable) Modifier.clickable { onClick(bike) } else Modifier)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                Modifier
                    .size(40.dp)
                    .border(width = 1.5.dp, color = contentColor, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                TextMMD(
                    text = bike.dockNumber.toString(),
                    style = eInkTypography.titleSmall,
                    color = contentColor,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                val type = if (bike.isElectric) "Electric" else "Classic"
                val battery = bike.battery?.let { " · $it%" }.orEmpty()
                val status = if (bike.isAvailable) "" else " · Unavailable"
                TextMMD(
                    text = "$type$battery$status",
                    style = eInkTypography.bodyMedium,
                    color = contentColor,
                    textDecoration = textDecoration,
                )
            }
            if (checkoutSupported) {
                Icon(
                    imageVector = if (bike.isAvailable) Icons.Filled.LockOpen else Icons.Filled.Lock,
                    contentDescription = if (bike.isAvailable) "Unlock" else "Unavailable",
                    tint = contentColor,
                )
            }
        }
        ListItemDivider()
    }
}
