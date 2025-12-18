package com.janerli.delishhub.data.local.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.janerli.delishhub.data.local.entity.RecipeEntity
import com.janerli.delishhub.data.local.entity.RecipeTagCrossRef
import com.janerli.delishhub.data.local.entity.TagEntity

data class RecipeWithTags(
    @Embedded val recipe: RecipeEntity,
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
