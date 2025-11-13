package com.example.storyai.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.storyai.ui.ChatScreen
import com.example.storyai.ui.HomeScreen
import com.example.storyai.ui.LoginScreen

@Composable
fun NavGraph(startDestination: String) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(navController = navController)
        }
        composable("home") {
            HomeScreen(navController = navController)
        }
        composable(
            route = "chat/{storyId}",
            arguments = listOf(navArgument("storyId") { type = NavType.StringType })
        ) {
            val storyId = it.arguments?.getString("storyId")
            requireNotNull(storyId) { "Story ID not found" }
            ChatScreen(storyId = storyId)
        }
    }
}
