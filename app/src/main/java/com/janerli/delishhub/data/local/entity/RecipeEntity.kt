package com.janerli.delishhub.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recipes",
    indices = [
        Index(value = ["ownerId"]),
        Index(value = ["title"]),
        Index(value = ["categoryId"]),
        Index(value = ["updatedAt"]),
        Index(value = ["cookTimeMin"]),
        Index(value = ["difficulty"]),
        Index(value = ["isPublic"]),
    ]
)
data class RecipeEntity(
    @PrimaryKey val id: String, // UUID
    val ownerId: String,        // uid или "guest"
    val title: String,
    val description: String = "",
    val categoryId: String? = null,
    val difficulty: Int = 1, // 1..5
    val cookTimeMin: Int = 0,

    // nutrition
    val calories: Int? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val carbs: Double? = null,

    val isVegetarian: Boolean = false,
    val isPublic: Boolean = false,

    // optional ranking for сортировки
    val ratingAvg: Double? = null,
    val ratingsCount: Int? = null,

    val mainImageUrl: String? = null,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    /**
     * 0=SYNCED, 1=CREATED, 2=UPDATED, 3=DELETED
     * (для будущей синхронизации с Firestore)
     */
    val syncStatus: Int = 0
)
