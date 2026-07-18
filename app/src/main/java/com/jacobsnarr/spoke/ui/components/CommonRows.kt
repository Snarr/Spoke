package com.jacobsnarr.spoke.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.PedalBike
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jacobsnarr.spoke.R
import com.jacobsnarr.spoke.data.prefs.UnitSystem
import com.jacobsnarr.spoke.data.station.model.Station
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.eInkTypography

@Composable
fun SettingsMenuRow(icon: ImageVector?, title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .height(ListRowHeight)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null)
                Spacer(Modifier.width(16.dp))
            }
            TextMMD(
                text = title,
                style = eInkTypography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
            )
        }
        ListItemDivider()
    }
}

@Composable
fun SettingsDetailRow(icon: ImageVector?, title: String, detail: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .height(ListRowHeight)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null)
                Spacer(Modifier.width(16.dp))
            }
            TextMMD(
                text = title,
                style = eInkTypography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            TextMMD(
                text = detail,
                style = eInkTypography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        ListItemDivider()
    }
}

@Composable
fun StationRow(station: Station, unitSystem: UnitSystem, onClick: () -> Unit, modifier: Modifier = Modifier, highlightQuery: String = "") {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .height(ListRowHeight)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (highlightQuery.isNotBlank()) {
                    TextMMD(
                        text = buildHighlightedString(station.name, highlightQuery),
                        style = eInkTypography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    TextMMD(
                        text = station.name,
                        style = eInkTypography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ElectricBolt,
                        contentDescription = "Electric bikes",
                        modifier = Modifier.heightIn(max = 16.dp),
                    )
                    Spacer(Modifier.width(2.dp))
                    TextMMD(
                        text = station.electricBikesAvailable.toString(),
                        style = eInkTypography.bodySmall,
                    )
                    Spacer(Modifier.width(10.dp))

                    Icon(
                        imageVector = Icons.Outlined.PedalBike,
                        contentDescription = "Classic bikes",
                        modifier = Modifier.heightIn(max = 16.dp),
                    )
                    Spacer(Modifier.width(2.dp))
                    TextMMD(
                        text = station.classicBikesAvailable.toString(),
                        style = eInkTypography.bodySmall,
                    )
                    Spacer(Modifier.width(10.dp))

                    Icon(
                        painter = painterResource(R.drawable.ic_bike_dock),
                        contentDescription = "Docks",
                        modifier = Modifier.heightIn(max = 16.dp),
                    )
                    Spacer(Modifier.width(2.dp))
                    TextMMD(
                        text = station.docksAvailable.toString(),
                        style = eInkTypography.bodySmall,
                    )
                    Spacer(Modifier.width(10.dp))

                    Spacer(Modifier.weight(1f))

                    station.distanceMeters?.let { meters ->
                        TextMMD(
                            text = formatDistance(meters, unitSystem),
                            style = eInkTypography.bodySmall,
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open",
            )
        }
        ListItemDivider()
    }
}
