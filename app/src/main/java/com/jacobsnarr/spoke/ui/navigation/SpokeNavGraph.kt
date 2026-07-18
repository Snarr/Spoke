package com.jacobsnarr.spoke.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.jacobsnarr.spoke.data.ride.RideState
import com.jacobsnarr.spoke.di.AppContainer
import com.jacobsnarr.spoke.ui.compass.CompassScreen
import com.jacobsnarr.spoke.ui.myrides.LifetimeStatsScreen
import com.jacobsnarr.spoke.ui.myrides.MyRidesScreen
import com.jacobsnarr.spoke.ui.myrides.TripDetailsScreen
import com.jacobsnarr.spoke.ui.myrides.TripHistoryScreen
import com.jacobsnarr.spoke.ui.myrides.UnpaidFeesScreen
import com.jacobsnarr.spoke.ui.ride.RideStatusScreen
import com.jacobsnarr.spoke.ui.search.SearchScreen
import com.jacobsnarr.spoke.ui.settings.AccountScreen
import com.jacobsnarr.spoke.ui.settings.DebugScreen
import com.jacobsnarr.spoke.ui.settings.SettingsScreen
import com.jacobsnarr.spoke.ui.settings.UnitsScreen
import com.jacobsnarr.spoke.ui.stationdetail.StationDetailScreen
import com.jacobsnarr.spoke.ui.stations.StationsScreen
import com.jacobsnarr.spoke.ui.systemselect.SystemSelectScreen

@Composable
internal fun SpokeNavGraph(
    container: AppContainer,
    navController: NavHostController,
    startDestination: String,
    isDebuggable: Boolean,
    debugMenuVisible: Boolean,
    debugCompassEnabled: Boolean,
    rideState: RideState,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(Routes.SYSTEM_SELECT) {
            SystemSelectScreen(
                onConfirmed = {
                    navController.navigate(Routes.STATIONS) {
                        popUpTo(Routes.SYSTEM_SELECT) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.SWITCH_SYSTEM) {
            SystemSelectScreen(
                onConfirmed = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.STATIONS) {
            StationsScreen(
                onStationClick = { stationId ->
                    navController.navigate(Routes.stationDetail(stationId))
                },
                onSearch = {
                    navController.navigate(Routes.SEARCH)
                },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onOpenAccount = {
                    navController.navigate(Routes.SETTINGS_ACCOUNT)
                },
                onOpenUnits = {
                    navController.navigate(Routes.SETTINGS_UNITS)
                },
                onOpenDebug = {
                    navController.navigate(Routes.SETTINGS_DEBUG)
                },
                showDebug = isDebuggable && debugMenuVisible,
                onSwitchSystem = {
                    navController.navigate(Routes.SWITCH_SYSTEM)
                },
            )
        }
        composable(Routes.SETTINGS_ACCOUNT) {
            AccountScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS_UNITS) {
            UnitsScreen(onBack = { navController.popBackStack() })
        }
        if (isDebuggable && debugMenuVisible) {
            composable(Routes.SETTINGS_DEBUG) {
                DebugScreen(
                    onBack = { navController.popBackStack() },
                    onHideDebugMenuUntilRestart = {
                        container.debugSettingsRepository.hideDebugMenuUntilRestart()
                        navController.popBackStack()
                    },
                )
            }
        }
        composable(
            route = Routes.STATION_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_STATION_ID) { type = NavType.IntType }),
        ) { entry ->
            val stationId = entry.arguments?.getInt(Routes.ARG_STATION_ID) ?: return@composable
            StationDetailScreen(
                stationId = stationId,
                onBack = { navController.popBackStack() },
                onCompass =
                if (isDebuggable && debugCompassEnabled) {
                    { navController.navigate(Routes.stationCompass(stationId)) }
                } else {
                    null
                },
                reserveBottomInset = rideState !is RideState.Active,
                onUnlocked = {
                    navController.navigate(Routes.RIDE) {
                        popUpTo(Routes.STATIONS)
                    }
                },
            )
        }
        if (isDebuggable && debugCompassEnabled) {
            composable(
                route = Routes.STATION_COMPASS,
                arguments = listOf(navArgument(Routes.ARG_STATION_ID) { type = NavType.IntType }),
            ) { entry ->
                val stationId =
                    entry.arguments?.getInt(Routes.ARG_STATION_ID)
                        ?: return@composable
                CompassScreen(
                    stationId = stationId,
                    onBack = { navController.popBackStack() },
                )
            }
        }
        composable(Routes.SEARCH) {
            SearchScreen(
                onStationClick = { stationId ->
                    navController.navigate(Routes.stationDetail(stationId))
                },
                onBack = { navController.popBackStack() },
                reserveBottomInset = rideState !is RideState.Active,
            )
        }
        composable(Routes.RIDE) {
            RideStatusScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.MY_RIDES) {
            MyRidesScreen(
                onOpenLifetimeStats = { navController.navigate(Routes.LIFETIME_STATS) },
                onOpenTripHistory = { navController.navigate(Routes.TRIP_HISTORY) },
                onOpenUnpaidFees = { navController.navigate(Routes.UNPAID_FEES) },
            )
        }
        composable(Routes.LIFETIME_STATS) {
            LifetimeStatsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.TRIP_HISTORY) {
            TripHistoryScreen(
                onBack = { navController.popBackStack() },
                onOpenTripDetails = { tripId ->
                    navController.navigate(Routes.tripDetails(tripId))
                },
            )
        }
        composable(
            route = Routes.TRIP_DETAILS,
            arguments = listOf(navArgument(Routes.ARG_TRIP_ID) { type = NavType.LongType }),
        ) { entry ->
            val tripId = entry.arguments?.getLong(Routes.ARG_TRIP_ID) ?: return@composable
            TripDetailsScreen(
                tripId = tripId,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.UNPAID_FEES) {
            UnpaidFeesScreen(onBack = { navController.popBackStack() })
        }
    }
}
