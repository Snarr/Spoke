package com.jacobsnarr.spoke.ui.systemselect

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jacobsnarr.spoke.data.system.BikeSystem
import com.jacobsnarr.spoke.data.system.BikeSystems
import com.jacobsnarr.spoke.ui.rememberAppContainer
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.radio_button.RadioButtonMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.eInkTypography

/**
 * System picker used both for first-launch onboarding and for the "Switch system" action in
 * Settings. Choosing a system selects it (signing out of any existing session for [onBack] == null
 * onboarding, or the previous system when switching) and invokes [onConfirmed].
 *
 * @param onBack when non-null, shows a back button (switch-from-settings mode).
 * @param onConfirmed invoked after the selection is committed.
 */
@Composable
fun SystemSelectScreen(onConfirmed: () -> Unit, onBack: (() -> Unit)? = null) {
    val container = rememberAppContainer()
    val current = container.systemRepository.current
    var selectedId by remember { mutableStateOf(current.id) }

    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        if (onBack != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                TextMMD(text = "Switch system", style = eInkTypography.titleMedium)
            }
        }

        LazyColumnMMD(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
        ) {
            item {
                TextMMD(
                    text = "Choose your bikeshare system",
                    style = eInkTypography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(24.dp))
            }

            items(BikeSystems.all.size) { index ->
                val system = BikeSystems.all[index]
                SystemOption(
                    system = system,
                    selected = system.id == selectedId,
                    onSelect = { selectedId = system.id },
                )
            }

            item {
                Spacer(Modifier.height(32.dp))

                ButtonMMD(
                    onClick = {
                        val chosen = BikeSystems.fromId(selectedId)
                        // switchSystem signs out of any prior session; safe even on first launch.
                        container.authRepository.switchSystem(chosen)
                        onConfirmed()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextMMD("Continue")
                }
            }
        }
    }
}

@Composable
private fun SystemOption(system: BikeSystem, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        RadioButtonMMD(selected = selected, onClick = onSelect)
        Spacer(Modifier.width(12.dp))
        TextMMD(text = system.displayName, style = eInkTypography.bodyLarge)
    }
}
