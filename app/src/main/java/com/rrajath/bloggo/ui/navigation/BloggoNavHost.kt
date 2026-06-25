package com.rrajath.bloggo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.rrajath.bloggo.ui.editor.EditorScreen
import com.rrajath.bloggo.ui.home.HomeScreen
import com.rrajath.bloggo.ui.settings.SettingsScreen

@Composable
fun BloggoNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onNewPost = { navController.navigate(Routes.editor()) },
                onOpenPost = { postId -> navController.navigate(Routes.editor(postId)) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(
            route = Routes.EDITOR_WITH_POST,
            arguments = listOf(
                navArgument("postId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val postId = entry.arguments?.getString("postId")
            EditorScreen(
                postId = postId,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
