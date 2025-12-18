package com.janerli.delishhub.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal_plan_entries",
    indices = [
        Index(value = ["userId", "dateEpochDay"]),
        Index(value = ["userId", "dateEpochDay", "mealType"], unique = true)
    ]
)
data class MealPlanEntryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val dateEpochDay: Long,
    val mealType: String,          // "BREAKFAST"/"LUNCH"/...
    val recipeId: String,
    val servings: Int,
    val createdAt: Long,
    val updatedAt: Long,

    /**
     * 0=SYNCED, 1=CREATED, 2=UPDATED, 3=DELETED
     */
    val syncStatus: Int = 0
)
