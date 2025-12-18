package com.janerli.delishhub.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.janerli.delishhub.feature.admin.AdminScreen
import com.janerli.delishhub.feature.auth.ForgotPasswordScreen
import com.janerli.delishhub.feature.auth.LoginScreen
import com.janerli.delishhub.feature.auth.OnboardingScreen
import com.janerli.delishhub.feature.auth.RegisterScreen
import com.janerli.delishhub.feature.auth.SplashScreen
import com.janerli.delishhub.feature.export.ExportScreen
import com.janerli.delishhub.feature.favorites.FavoritesScreen
import com.janerli.delishhub.feature.home.HomeScreen
import com.janerli.delishhub.feature.planner.PlannerScreen
import com.janerli.delishhub.feature.profile.ProfileScreen
import com.janerli.delishhub.feature.recipes.RecipeCreateScreen
import com.janerli.delishhub.feature.recipes.RecipeDetailsScreen
import com.janerli.delishhub.feature.recipes.RecipeEditScreen
import com.janerli.delishhub.feature.recipes.RecipesScreen
import com.janerli.delishhub.feature.shopping.ShoppingScreen
import com.janerli.delishhub.feature.profile.AccountScreen
import com.janerli.delishhub.feature.profile.SettingsScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onContinue = { route ->
                    navController.navigate(route) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onLogin = { navController.navigate(Routes.LOGIN) },
                onRegister = { navController.navigate(Routes.REGISTER) },
                onGuest = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
                onForgot = { navController.navigate(Routes.FORGOT_PASSWORD) }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegistered = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }

        // Main flow
        composable(Routes.HOME) { HomeScreen(navController) }
        composable(Routes.RECIPES) { RecipesScreen(navController) }
        composable(Routes.PLANNER) { PlannerScreen(navController) }
        composable(Routes.SHOPPING) { ShoppingScreen(navController) }
        composable(Routes.PROFILE) { ProfileScreen(navController) }
        composable(Routes.FAVORITES) { FavoritesScreen(navController) }

        composable(
            route = Routes.RECIPE_DETAILS,
            arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("recipeId").orEmpty()
            RecipeDetailsScreen(navController = navController, recipeId = id)
        }

        composable(Routes.RECIPE_CREATE) {
            RecipeCreateScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.RECIPE_EDIT,
            arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("recipeId").orEmpty()
            RecipeEditScreen(navController = navController, recipeId = id)
        }

        composable(Routes.MY_RECIPES) {
            com.janerli.delishhub.feature.recipes.RecipesScreen(
                navController = navController,
                isMyMode = true
            )
        }

        // Secondary
        composable(Routes.ADMIN) { AdminScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.EXPORT) { ExportScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.SETTINGS) { SettingsScreen(navController) }
        composable(Routes.ACCOUNT) { AccountScreen(navController) }
    }
}
