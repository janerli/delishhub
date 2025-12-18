package com.janerli.delishhub.domain.model

data class Recipe(
    val id: String,
    val title: String,
    val description: String,
    val cookTimeMinutes: Int,
    val imageUrl: String? = null,
    val isFavorite: Boolean = false
)
