package dev.ahmad.wird.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.ahmad.wird.ui.components.WirdBottomBar
import dev.ahmad.wird.ui.feature.today.TodayScreen

/**
 * The NavController lives here and nowhere else. Screens receive lambdas rather than the
 * controller itself, so they stay testable and previewable.
 *
 * The destinations are intentionally empty: this establishes the shell, and each screen
 * fills its own route in.
 */
@Composable
fun WirdNavGraph(
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = backStackEntry?.destination

    val selected = TopLevelDestination.entries.firstOrNull { destination ->
        current.matches(destination)
    } ?: TopLevelDestination.TODAY

    Scaffold(
        bottomBar = {
            WirdBottomBar(
                destinations = TopLevelDestination.entries,
                selected = selected,
                onSelect = { destination ->
                    navController.navigate(destination.route) {
                        popUpTo(TodayRoute) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TodayRoute,
            modifier = Modifier.padding(padding),
        ) {
            composable<TodayRoute> { TodayScreen() }
            composable<HistoryRoute> { EmptyDestination() }
            composable<SettingsRoute> { EmptyDestination() }
        }
    }
}

private fun NavDestination?.matches(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { node ->
        when (destination) {
            TopLevelDestination.TODAY -> node.hasRoute(TodayRoute::class)
            TopLevelDestination.HISTORY -> node.hasRoute(HistoryRoute::class)
            TopLevelDestination.SETTINGS -> node.hasRoute(SettingsRoute::class)
        }
    } == true

@Composable
private fun EmptyDestination() {
    Box(Modifier.fillMaxSize())
}
