package com.janerli.delishhub.feature.recipes.ui

data class RecipeCardUi(
    val id: String,
    val title: String,
    val cookTimeMin: Int,
    val difficulty: Int,
    val isFavorite: Boolean
)
