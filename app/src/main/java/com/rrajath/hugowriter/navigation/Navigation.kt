package com.rrajath.hugowriter.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rrajath.hugowriter.ui.screens.PostEditorScreen
import com.rrajath.hugowriter.ui.screens.PostListScreen
import com.rrajath.hugowriter.ui.screens.SettingsScreen

sealed class Screen(val route: String, val title: String) {
    object Posts : Screen("posts", "Posts")
    object Settings : Screen("settings", "Settings")
    object PostEditor : Screen("post_editor/{postId}", "Post Editor") {
        fun createRoute(postId: String?) = if (postId != null) "post_editor/$postId" else "post_editor/new"
    }
}

@Composable
fun HugoWriterApp(
    darkTheme: Boolean
) {
    val navController = rememberNavController()

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Posts.route
        ) {
            composable(Screen.Posts.route) {
                PostListScreen(
                    onNavigateToEditor = { postId ->
                        navController.navigate(Screen.PostEditor.createRoute(postId))
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Screen.PostEditor.route,
                arguments = listOf(
                    navArgument("postId") {
                        type = NavType.StringType
                        nullable = true
                    }
                )
            ) { backStackEntry ->
                val postId = backStackEntry.arguments?.getString("postId")
                    ?.takeIf { it != "new" }

                PostEditorScreen(
                    postId = postId,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
