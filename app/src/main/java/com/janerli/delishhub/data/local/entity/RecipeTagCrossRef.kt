package com.janerli.delishhub.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "recipe_tag_cross_ref",
    primaryKeys = ["recipeId", "tagId"],
    indices = [
        Index(value = ["recipeId"]),
        Index(value = ["tagId"])
    ]
)
data class RecipeTagCrossRef(
    val recipeId: String,
    val tagId: String
)
