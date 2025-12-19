package com.janerli.delishhub.feature.recipes.ui

data class RecipesFiltersUi(
    val minTime: Int? = null,
    val maxTime: Int? = null,
    val minDifficulty: Int? = null,
    val maxDifficulty: Int? = null,
    val onlyFavorites: Boolean = false,

    // ✅ новое: мультивыбор тегов
    val tagIds: Set<String> = emptySet()
)

enum class RecipesSortUi {
    TITLE_ASC,
    TIME_ASC,
    DIFFICULTY_ASC
}
