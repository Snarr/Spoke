package com.jacobsnarr.spoke.ui.ride

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

/**
 * Ticks once per second and returns the whole seconds elapsed since [startedAtMillis]. Shared by
 * the ride screen and the persistent ride banner so both stay in sync and visibly increment.
 */
@Composable
fun rememberElapsedSeconds(startedAtMillis: Long): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startedAtMillis) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1.seconds)
        }
    }
    return ((now - startedAtMillis) / 1000).coerceAtLeast(0)
}

/**
 * Formats elapsed seconds as `MM:SS`, expanding to `H:MM:SS` once the ride passes an hour.
 */
fun formatElapsed(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
