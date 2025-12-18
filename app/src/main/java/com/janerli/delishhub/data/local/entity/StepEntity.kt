package com.janerli.delishhub.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "steps",
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
        Index(value = ["recipeId"])
    ]
)
data class StepEntity(
    @PrimaryKey val id: String, // UUID
    val recipeId: String,
    val text: String,
    val photoUrl: String? = null,
    val position: Int = 0
)
