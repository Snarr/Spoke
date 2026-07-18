package com.jacobsnarr.spoke.ui.offline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jacobsnarr.spoke.ui.components.ListItemDivider
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.eInkTypography

@Composable
fun OfflineModeBanner(applyNavBarPadding: Boolean) {
    androidx.compose.foundation.layout.Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
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
                imageVector = Icons.Filled.CloudOff,
                contentDescription = null,
            )
            androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))
            TextMMD(
                text = "Offline mode: showing cached data",
                style = eInkTypography.bodyMedium,
            )
        }
    }
}
