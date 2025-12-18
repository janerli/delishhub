package com.janerli.delishhub.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.janerli.delishhub.data.local.model.RecipeFullLocal
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeFullDao {

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :recipeId")
    fun observeRecipeFull(recipeId: String): Flow<RecipeFullLocal?>

    @Transaction
    @Query("SELECT * FROM recipes")
    fun observeAllRecipesFull(): Flow<List<RecipeFullLocal>>
}
