package com.bothybag.app.navigation

import androidx.compose.runtime.Composable
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

    fun mapRoute(centerOnBothyId: String? = null): String {
        return if (centerOnBothyId != null) "map?centerOnBothyId=$centerOnBothyId" else "map"
    }

    fun detailRoute(bothyId: String) = "detail/$bothyId"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "map"
    ) {
        composable(
            route = "map?centerOnBothyId={centerOnBothyId}",
            arguments = listOf(
                navArgument("centerOnBothyId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val centerOnBothyId = backStackEntry.arguments?.getString("centerOnBothyId")
            MapScreen(
                onBothyClick = { bothyId ->
                    navController.navigate(Screen.detailRoute(bothyId))
                },
                onNewsClick = {
                    navController.navigate(Screen.NEWS)
                },
                centerOnBothyId = centerOnBothyId
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
                onBack = {
                    navController.navigate(Screen.mapRoute(centerOnBothyId = bothyId)) {
                        popUpTo("map") { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.NEWS) {
            NewsScreen(
                onBack = { navController.popBackStack() },
                onBothyClick = { bothyId ->
                    navController.navigate(Screen.detailRoute(bothyId))
                }
            )
        }
    }
}
