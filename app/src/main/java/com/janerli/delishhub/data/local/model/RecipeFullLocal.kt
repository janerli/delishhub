package com.janerli.delishhub.data.local.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.janerli.delishhub.data.local.entity.CategoryEntity
import com.janerli.delishhub.data.local.entity.IngredientEntity
import com.janerli.delishhub.data.local.entity.RecipeEntity
import com.janerli.delishhub.data.local.entity.RecipeTagCrossRef
import com.janerli.delishhub.data.local.entity.StepEntity
import com.janerli.delishhub.data.local.entity.TagEntity

data class RecipeFullLocal(
    @Embedded val recipe: RecipeEntity,

    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity?,

    @Relation(
        parentColumn = "id",
        entityColumn = "recipeId"
    )
    val ingredients: List<IngredientEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "recipeId"
    )
    val steps: List<StepEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = RecipeTagCrossRef::class,
            parentColumn = "recipeId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>
)
