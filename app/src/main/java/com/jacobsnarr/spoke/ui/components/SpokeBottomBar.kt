package com.jacobsnarr.spoke.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.eInkTypography

/**
 * Compact bottom navigation bar tuned for the 800x480 e-ink screen. Unlike MMD's
 * [com.mudita.mmd.components.nav_bar.NavigationBarMMD] (whose items are a fixed 80dp tall),
 * this keeps a short 56dp content row while correctly reserving the system-nav inset via
 * [navigationBarsPadding], so it never overlaps the device back/home/multitask buttons.
 */
@Composable
fun SpokeBottomBar(items: List<BottomBarItem>, currentRoute: String?, onSelect: (BottomBarItem) -> Unit) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        TopBarDivider()
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(72.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onSelect(item) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            painter = item.icon,
                            contentDescription = item.label,
                        )
                        Spacer(Modifier.height(2.dp))
                        TextMMD(
                            text = item.label,
                            style = eInkTypography.labelSmall,
                            fontWeight = if (selected) FontWeight.Black else FontWeight.Normal,
                        )
                    }
                    Box(
                        modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(ACTIVE_INDICATOR_HEIGHT)
                            .background(
                                if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            ),
                    )
                }
                if (item !== items.last()) Spacer(Modifier.width(4.dp))
            }
        }
    }
}

private val ACTIVE_INDICATOR_HEIGHT = 6.dp

data class BottomBarItem(val route: String, val label: String, val icon: Painter)
