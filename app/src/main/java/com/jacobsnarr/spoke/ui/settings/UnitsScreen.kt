package com.jacobsnarr.spoke.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacobsnarr.spoke.data.prefs.UnitSystem
import com.jacobsnarr.spoke.ui.components.SpokeTopBar
import com.jacobsnarr.spoke.ui.rememberAppContainer
import com.mudita.mmd.components.radio_button.RadioButtonMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.eInkTypography

@Composable
fun UnitsScreen(onBack: () -> Unit) {
    val container = rememberAppContainer()
    val unitSystem by container.preferencesStore.unitSystem.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        SpokeTopBar(
            title = "Units",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(28.dp))
                }
            },
        )
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            TextMMD(
                text = "Distances are shown in these units.",
                style = eInkTypography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            UnitOption(
                label = "Imperial (miles, feet)",
                selected = unitSystem == UnitSystem.IMPERIAL,
                onSelect = { container.preferencesStore.setUnitSystem(UnitSystem.IMPERIAL) },
            )
            UnitOption(
                label = "Metric (kilometers, meters)",
                selected = unitSystem == UnitSystem.METRIC,
                onSelect = { container.preferencesStore.setUnitSystem(UnitSystem.METRIC) },
            )
        }
    }
}

@Composable
private fun UnitOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButtonMMD(selected = selected, onClick = onSelect)
        Spacer(Modifier.width(12.dp))
        TextMMD(text = label, style = eInkTypography.bodyLarge)
    }
}
