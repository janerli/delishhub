package com.janerli.delishhub.domain.repository

import com.janerli.delishhub.data.local.entity.MealPlanEntryEntity
import com.janerli.delishhub.data.local.entity.RecipeEntity
import com.janerli.delishhub.data.local.entity.ShoppingItemEntity
import com.janerli.delishhub.data.local.model.RecipeFull
import kotlinx.coroutines.flow.Flow

interface RecipeRepository {

    data class RecipeFilters(
        val query: String = "",
        val minTime: Int? = null,
        val maxTime: Int? = null,
        val minDifficulty: Int? = null,
        val maxDifficulty: Int? = null
    )

    enum class RecipeSort {
        UPDATED_DESC,
        TITLE_ASC,
        TIME_ASC,
        DIFFICULTY_ASC
    }

    fun observeCatalog(
        ownerId: String,
        onlyMine: Boolean,
        filters: RecipeFilters,
        sort: RecipeSort
    ): Flow<List<RecipeEntity>>

    fun observeFavorites(
        userId: String,
        filters: RecipeFilters,
        sort: RecipeSort
    ): Flow<List<RecipeEntity>>

    fun observeIsFavorite(userId: String, recipeId: String): Flow<Boolean>
    suspend fun toggleFavorite(userId: String, recipeId: String)

    fun observeRecipeFull(recipeId: String): Flow<RecipeFull?>
    suspend fun getRecipeFull(recipeId: String): RecipeFull?
    suspend fun upsertRecipeFull(
        recipe: RecipeEntity,
        ingredients: List<com.janerli.delishhub.data.local.entity.IngredientEntity>,
        steps: List<com.janerli.delishhub.data.local.entity.StepEntity>,
        tagIds: List<String>
    )

    suspend fun deleteRecipe(recipeId: String, hardDelete: Boolean)

    // -------- Planner (Шаг 5.4) --------
    fun observeMealPlanDay(userId: String, dateEpochDay: Long): Flow<List<MealPlanEntryEntity>>
    suspend fun setMeal(userId: String, dateEpochDay: Long, mealType: String, recipeId: String, servings: Int = 1)
    suspend fun removeMeal(userId: String, dateEpochDay: Long, mealType: String)

    // -------- Shopping (Шаг 5.5) --------
    fun observeShopping(userId: String): Flow<List<ShoppingItemEntity>>

    suspend fun addShoppingManual(
        userId: String,
        name: String,
        qtyText: String?
    )

    suspend fun toggleShoppingChecked(userId: String, id: String, checked: Boolean)
    suspend fun deleteShoppingItem(userId: String, id: String)
    suspend fun clearCheckedShopping(userId: String)

    /**
     * Добавить ингредиенты рецепта в покупки (суммируем по name+unit).
     */
    suspend fun addToShoppingFromRecipe(userId: String, recipeId: String)

    /**
     * Добавить ингредиенты всех рецептов из плана на выбранный день.
     */
    suspend fun addToShoppingFromPlanDay(userId: String, dateEpochDay: Long)
}
