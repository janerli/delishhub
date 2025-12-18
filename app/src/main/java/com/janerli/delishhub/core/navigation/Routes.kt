package com.janerli.delishhub.core.navigation

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"

    const val HOME = "home"
    const val RECIPES = "recipes"
    const val RECIPE_DETAILS = "recipe_details/{recipeId}"
    fun recipeDetails(recipeId: String) = "recipe_details/$recipeId"

    const val RECIPE_EDIT = "recipe_edit/{recipeId}"
    fun recipeEdit(recipeId: String) = "recipe_edit/$recipeId"
    const val RECIPE_CREATE = "recipe_create"

    const val FAVORITES = "favorites"
    const val PLANNER = "planner"
    const val SHOPPING = "shopping"
    const val PROFILE = "profile"
    const val ADMIN = "admin"
    const val EXPORT = "export"
    const val SETTINGS = "settings"
    const val ACCOUNT = "account"
    const val MY_RECIPES = "my_recipes"
}
