package com.janerli.delishhub.feature.recipes.ui

data class RecipeCardUi(
    val id: String,
    val title: String,
    val cookTimeMin: Int,
    val difficulty: Int,
    val isFavorite: Boolean,
    val imageUrl: String? = null,

    val ownerId: String? = null,
    val isPublic: Boolean = false,

    // ✅ новое: чтобы рисовать бейдж "МОЙ" без зависимостей от SessionManager в UI
    val isMine: Boolean = false
)
