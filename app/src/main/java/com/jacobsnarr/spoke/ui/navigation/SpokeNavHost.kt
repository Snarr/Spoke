package com.jacobsnarr.spoke.ui.navigation

import android.content.pm.ApplicationInfo
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.navigation.compose.rememberNavController
import com.jacobsnarr.spoke.R
import com.jacobsnarr.spoke.ui.components.BottomBarItem
import com.jacobsnarr.spoke.ui.rememberAppContainer

@Composable
fun SpokeNavHost() {
    val container = rememberAppContainer()
    val navController = rememberNavController()

    val context = LocalContext.current
    val isDebuggable =
        remember(context) {
            (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        }

    val startDestination =
        remember {
            if (container.systemRepository.hasSelectedSystem) Routes.STATIONS else Routes.SYSTEM_SELECT
        }

    val bottomTabs = rememberBottomTabs()

    SpokeAppScaffold(
        container = container,
        navController = navController,
        startDestination = startDestination,
        bottomTabs = bottomTabs,
        isDebuggable = isDebuggable,
    )
}

@Composable
internal fun rememberBottomTabs(): List<BottomBarItem> = listOf(
    BottomBarItem(Routes.STATIONS, "Stations", painterResource(R.drawable.ic_bike_station_2)),
    BottomBarItem(Routes.MY_RIDES, label = "My Rides", rememberVectorPainter(Icons.AutoMirrored.Filled.DirectionsBike)),
    BottomBarItem(Routes.SETTINGS, "Settings", rememberVectorPainter(Icons.Filled.Settings)),
)
