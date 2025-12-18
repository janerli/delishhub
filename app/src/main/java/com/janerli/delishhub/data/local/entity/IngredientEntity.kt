package com.janerli.delishhub.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ingredients",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["recipeId"]),
        Index(value = ["name"])
    ]
)
data class IngredientEntity(
    @PrimaryKey val id: String, // UUID
    val recipeId: String,
    val name: String,
    val amount: Double? = null,
    val unit: String? = null, // "г", "мл", "шт"
    val position: Int = 0
)
