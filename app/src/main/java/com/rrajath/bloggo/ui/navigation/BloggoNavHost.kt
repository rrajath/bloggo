package com.rrajath.bloggo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.rrajath.bloggo.ui.editor.EditorScreen
import com.rrajath.bloggo.ui.home.HomeScreen
import com.rrajath.bloggo.ui.settings.SettingsScreen
import android.content.Intent
import android.net.Uri

@Composable
fun BloggoNavHost(navController: NavHostController) {
    val context = LocalContext.current
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onNewPost = { navController.navigate(Routes.editor()) },
                onOpenPost = { postId -> navController.navigate(Routes.editor(postId)) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onViewLive = { slug ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(slug))
                    context.startActivity(intent)
                },
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
                onPublish = { post ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("published_post", post)
                    navController.popBackStack()
                },
                onInsertImage = { /* M10 */ },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
