package com.bothybag.app.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bothybag.app.ui.detail.BothyDetailScreen
import com.bothybag.app.ui.map.MapScreen
import com.bothybag.app.ui.news.NewsScreen

object Screen {
    const val MAP = "map"
    const val DETAIL = "detail/{bothyId}"
    const val NEWS = "news"

    fun detailRoute(bothyId: String) = "detail/$bothyId"
}

/**
 * True while this screen is the one in front. Used to ignore a second tap that
 * arrives while the screen is already animating away, which would otherwise
 * navigate or go back twice.
 */
private fun NavBackStackEntry.isInFront(): Boolean =
    lifecycle.currentState == Lifecycle.State.RESUMED

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.MAP
    ) {
        composable(route = Screen.MAP) { backStackEntry ->
            MapScreen(
                onBothyClick = { bothyId ->
                    if (backStackEntry.isInFront()) {
                        navController.navigate(Screen.detailRoute(bothyId))
                    }
                },
                onNewsClick = {
                    if (backStackEntry.isInFront()) {
                        navController.navigate(Screen.NEWS)
                    }
                }
            )
        }

        composable(
            route = Screen.DETAIL,
            arguments = listOf(
                navArgument("bothyId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val bothyId = backStackEntry.arguments?.getString("bothyId") ?: return@composable
            BothyDetailScreen(
                bothyId = bothyId,
                // Go back to wherever the bothy was opened from: the map (which
                // reopens where it was left) or the news list
                onBack = {
                    if (backStackEntry.isInFront()) navController.popBackStack()
                }
            )
        }

        composable(route = Screen.NEWS) { backStackEntry ->
            NewsScreen(
                onBack = {
                    if (backStackEntry.isInFront()) navController.popBackStack()
                },
                onBothyClick = { bothyId ->
                    if (backStackEntry.isInFront()) {
                        navController.navigate(Screen.detailRoute(bothyId))
                    }
                }
            )
        }
    }
}
