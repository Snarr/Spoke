package com.jacobsnarr.spoke.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD

/**
 * App bar wrapper used across the logged-in screens. Matches Mudita's top app bar 1:1 (MMD's
 * default expanded height of 64dp) and lets [TopAppBarMMD] own the top status-bar inset (the
 * Scaffold no longer double-applies it).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpokeTopBar(title: String, navigationIcon: @Composable () -> Unit = {}, actions: @Composable RowScope.() -> Unit = {}) {
    SpokeTopBar(
        title = { TextMMD(title, maxLines = 1, fontWeight = FontWeight.Black) },
        navigationIcon = navigationIcon,
        actions = actions,
    )
}

/**
 * Overload accepting a composable title slot, so screens like Search can place a custom control
 * (e.g. an inline search field) directly in the top app bar's title area. [expandedHeight] defaults
 * to MMD's 64dp; raise it only if a taller control needs the room.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpokeTopBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    expandedHeight: Dp = 64.dp,
) {
    Column {
        TopAppBarMMD(
            title = title,
            navigationIcon = navigationIcon,
            actions = actions,
            expandedHeight = expandedHeight,
        )
        TopBarDivider()
    }
}
