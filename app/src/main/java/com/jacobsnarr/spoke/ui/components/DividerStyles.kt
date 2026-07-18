package com.jacobsnarr.spoke.ui.components

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ListItemDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
fun TopBarDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 2.dp,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
