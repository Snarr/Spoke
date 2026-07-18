package com.jacobsnarr.spoke.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.jacobsnarr.spoke.data.ride.RideState
import com.jacobsnarr.spoke.di.AppContainer
import com.jacobsnarr.spoke.ui.components.BottomBarItem
import com.jacobsnarr.spoke.ui.components.SpokeBottomBar
import com.jacobsnarr.spoke.ui.offline.OfflineModeBanner
import com.jacobsnarr.spoke.ui.ride.RideBanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun SpokeBottomBarContent(
    container: AppContainer,
    navController: NavHostController,
    bottomTabs: List<BottomBarItem>,
    rideState: RideState,
    isOffline: Boolean,
    resumeScope: CoroutineScope,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomTabs.map { it.route }
    val bannerHiddenRoutes = setOf(Routes.RIDE, Routes.SYSTEM_SELECT, Routes.SWITCH_SYSTEM)
    val showOfflineBanner = shouldShowOfflineBanner(currentRoute = currentRoute, isOffline = isOffline)
    val activeRide = rideState as? RideState.Active
    val bannerRide = activeRide?.takeIf { currentRoute !in bannerHiddenRoutes }

    if (!showOfflineBanner && bannerRide == null && !showBottomBar) return

    Column {
        if (showOfflineBanner) {
            OfflineModeBanner(applyNavBarPadding = !showBottomBar && bannerRide == null)
        }
        if (bannerRide != null) {
            RideBanner(
                state = bannerRide,
                applyNavBarPadding = !showBottomBar,
                onClick = {
                    if (currentRoute != Routes.RIDE) {
                        navController.navigate(Routes.RIDE) {
                            popUpTo(Routes.STATIONS)
                            launchSingleTop = true
                        }
                    }
                },
            )
        }
        if (showBottomBar) {
            SpokeBottomBar(
                items = bottomTabs,
                currentRoute = currentRoute,
                onSelect = { tab ->
                    if (currentRoute != tab.route) {
                        if (tab.route == Routes.MY_RIDES && container.authRepository.isCurrentlyLoggedIn()) {
                            resumeScope.launch {
                                container.myRidesRepository.prefetchCurrentMonthData()
                            }
                        }
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
            )
        }
    }
}

internal fun shouldShowOfflineBanner(currentRoute: String?, isOffline: Boolean): Boolean {
    if (!isOffline) return false
    val hiddenRoutes = setOf(Routes.SYSTEM_SELECT, Routes.SWITCH_SYSTEM, Routes.SETTINGS_ACCOUNT)
    return currentRoute !in hiddenRoutes
}
