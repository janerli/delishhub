package com.janerli.delishhub.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.janerli.delishhub.data.local.entity.RecipeEntity
import com.janerli.delishhub.data.local.entity.StepEntity

data class RecipeWithSteps(
    @Embedded val recipe: RecipeEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "recipeId"
    )
    val steps: List<StepEntity>
)
