package com.janerli.delishhub.feature.recipes.ui

data class RecipesFiltersUi(
    val minTime: Int? = null,
    val maxTime: Int? = null,
    val minDifficulty: Int? = null,
    val maxDifficulty: Int? = null,
    val onlyFavorites: Boolean = false
)

enum class RecipesSortUi {
    TITLE_ASC,
    TIME_ASC,
    DIFFICULTY_ASC
}
