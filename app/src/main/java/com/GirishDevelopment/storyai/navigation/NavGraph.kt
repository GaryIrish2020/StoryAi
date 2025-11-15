package com.GirishDevelopment.storyai.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.GirishDevelopment.storyai.ui.ChatScreen
import com.GirishDevelopment.storyai.ui.HomeScreen
import com.GirishDevelopment.storyai.ui.LoginScreen
import com.GirishDevelopment.storyai.ui.SplashScreen
import com.GirishDevelopment.storyai.ui.VideoScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(navController = navController)
        }
        composable("login") {
            LoginScreen(navController = navController)
        }
        composable("home") {
            HomeScreen(navController = navController)
        }
        composable(
            route = "video/{storyId}",
            arguments = listOf(navArgument("storyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val storyId = backStackEntry.arguments?.getString("storyId")
            if (storyId != null) {
                VideoScreen(navController = navController, storyId = storyId)
            }
        }
        // --- FIX: isNewStory is now a required argument ---
        composable(
            route = "chat/{storyId}/{isNewStory}",
            arguments = listOf(
                navArgument("storyId") { type = NavType.StringType },
                navArgument("isNewStory") { type = NavType.BoolType }
            )
        ) {
            ChatScreen(navController = navController)
        }
    }
}
