package com.janerli.delishhub.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.janerli.delishhub.data.local.entity.IngredientEntity
import com.janerli.delishhub.data.local.entity.RecipeEntity
import com.janerli.delishhub.data.local.entity.RecipeTagCrossRef
import com.janerli.delishhub.data.local.entity.StepEntity
import com.janerli.delishhub.data.local.model.RecipeFull
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    @Query("SELECT * FROM recipes WHERE syncStatus != 3")
    fun observeAllBase(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE id = :id AND syncStatus != 3 LIMIT 1")
    fun observeRecipeBase(id: String): Flow<RecipeEntity?>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id AND syncStatus != 3 LIMIT 1")
    fun observeRecipeFull(id: String): Flow<RecipeFull?>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id AND syncStatus != 3 LIMIT 1")
    suspend fun getRecipeFull(id: String): RecipeFull?

    @Query("SELECT * FROM recipes WHERE id = :id LIMIT 1")
    suspend fun getRecipeBaseNow(id: String): RecipeEntity?

    @Query("SELECT * FROM recipes WHERE syncStatus != 0")
    suspend fun getPendingBaseNow(): List<RecipeEntity>

    @Query("UPDATE recipes SET syncStatus = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query(
        """
        SELECT DISTINCT r.* FROM recipes r
        LEFT JOIN ingredients i ON i.recipeId = r.id
        WHERE r.syncStatus != 3
          AND (:onlyMine = 0 OR r.ownerId = :ownerId)
          AND (:publicOnly = 0 OR r.isPublic = 1)
          AND (:categoryId IS NULL OR r.categoryId = :categoryId)
          AND (:vegetarianOnly = 0 OR r.isVegetarian = 1)
          AND (:hasPhoto = 0 OR (r.mainImageUrl IS NOT NULL AND r.mainImageUrl != ''))
          AND (:minDifficulty IS NULL OR r.difficulty >= :minDifficulty)
          AND (:maxDifficulty IS NULL OR r.difficulty <= :maxDifficulty)
          AND (:minCookTime IS NULL OR r.cookTimeMin >= :minCookTime)
          AND (:maxCookTime IS NULL OR r.cookTimeMin <= :maxCookTime)
          AND (
              :query IS NULL OR :query = '' OR
              r.title LIKE '%' || :query || '%' OR
              i.name LIKE '%' || :query || '%'
          )
        ORDER BY
          CASE WHEN :sort = 'UPDATED_DESC' THEN r.updatedAt END DESC,
          CASE WHEN :sort = 'UPDATED_ASC'  THEN r.updatedAt END ASC,
          CASE WHEN :sort = 'TITLE_ASC'    THEN r.title END COLLATE NOCASE ASC,
          CASE WHEN :sort = 'TITLE_DESC'   THEN r.title END COLLATE NOCASE DESC,
          CASE WHEN :sort = 'TIME_ASC'     THEN r.cookTimeMin END ASC,
          CASE WHEN :sort = 'TIME_DESC'    THEN r.cookTimeMin END DESC,
          CASE WHEN :sort = 'RATING_DESC'  THEN r.ratingAvg END DESC,
          r.updatedAt DESC
        """
    )
    fun observeCatalog(
        query: String?,
        categoryId: String?,
        vegetarianOnly: Boolean,
        hasPhoto: Boolean,
        minDifficulty: Int?,
        maxDifficulty: Int?,
        minCookTime: Int?,
        maxCookTime: Int?,
        onlyMine: Boolean,
        ownerId: String,
        publicOnly: Boolean,
        sort: String
    ): Flow<List<RecipeEntity>>

    // ✅ ВАЖНО: f.syncStatus != 3
    @Query(
        """
        SELECT DISTINCT r.* FROM recipes r
        INNER JOIN favorites f ON f.recipeId = r.id AND f.userId = :userId AND f.syncStatus != 3
        LEFT JOIN ingredients i ON i.recipeId = r.id
        WHERE r.syncStatus != 3
          AND (:categoryId IS NULL OR r.categoryId = :categoryId)
          AND (:vegetarianOnly = 0 OR r.isVegetarian = 1)
          AND (:hasPhoto = 0 OR (r.mainImageUrl IS NOT NULL AND r.mainImageUrl != ''))
          AND (:minDifficulty IS NULL OR r.difficulty >= :minDifficulty)
          AND (:maxDifficulty IS NULL OR r.difficulty <= :maxDifficulty)
          AND (:minCookTime IS NULL OR r.cookTimeMin >= :minCookTime)
          AND (:maxCookTime IS NULL OR r.cookTimeMin <= :maxCookTime)
          AND (
              :query IS NULL OR :query = '' OR
              r.title LIKE '%' || :query || '%' OR
              i.name LIKE '%' || :query || '%'
          )
        ORDER BY
          CASE WHEN :sort = 'UPDATED_DESC' THEN r.updatedAt END DESC,
          CASE WHEN :sort = 'UPDATED_ASC'  THEN r.updatedAt END ASC,
          CASE WHEN :sort = 'TITLE_ASC'    THEN r.title END COLLATE NOCASE ASC,
          CASE WHEN :sort = 'TITLE_DESC'   THEN r.title END COLLATE NOCASE DESC,
          CASE WHEN :sort = 'TIME_ASC'     THEN r.cookTimeMin END ASC,
          CASE WHEN :sort = 'TIME_DESC'    THEN r.cookTimeMin END DESC,
          CASE WHEN :sort = 'RATING_DESC'  THEN r.ratingAvg END DESC,
          r.updatedAt DESC
        """
    )
    fun observeFavorites(
        userId: String,
        query: String?,
        categoryId: String?,
        vegetarianOnly: Boolean,
        hasPhoto: Boolean,
        minDifficulty: Int?,
        maxDifficulty: Int?,
        minCookTime: Int?,
        maxCookTime: Int?,
        sort: String
    ): Flow<List<RecipeEntity>>

    // ---------- ADMIN (обновили: filter + search + sort) ----------

    /**
     * filter: ALL | PUBLIC | PRIVATE | DELETED
     * sort: UPDATED_DESC | UPDATED_ASC | TITLE_ASC | TITLE_DESC
     */
    @Query(
        """
        SELECT * FROM recipes
        WHERE
            (
              (:filter = 'ALL')
              OR (:filter = 'PUBLIC'  AND isPublic = 1 AND syncStatus != 3)
              OR (:filter = 'PRIVATE' AND isPublic = 0 AND syncStatus != 3)
              OR (:filter = 'DELETED' AND syncStatus = 3)
            )
          AND (
              :query IS NULL OR :query = '' OR
              title LIKE '%' || :query || '%' OR
              ownerId LIKE '%' || :query || '%'
          )
        ORDER BY
          CASE WHEN :sort = 'UPDATED_DESC' THEN updatedAt END DESC,
          CASE WHEN :sort = 'UPDATED_ASC'  THEN updatedAt END ASC,
          CASE WHEN :sort = 'TITLE_ASC'    THEN title END COLLATE NOCASE ASC,
          CASE WHEN :sort = 'TITLE_DESC'   THEN title END COLLATE NOCASE DESC,
          updatedAt DESC
        """
    )
    fun observeAllForAdmin(
        filter: String,
        query: String?,
        sort: String
    ): Flow<List<RecipeEntity>>

    @Query("UPDATE recipes SET isPublic = :isPublic, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setPublic(id: String, isPublic: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE recipes SET syncStatus = 2, updatedAt = :updatedAt WHERE id = :id")
    suspend fun restoreRecipe(id: String, updatedAt: Long = System.currentTimeMillis())

    // ---------- UPSERT ----------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecipe(recipe: RecipeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIngredients(items: List<IngredientEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSteps(items: List<StepEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTagRefs(items: List<RecipeTagCrossRef>)

    @Query("DELETE FROM ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredientsByRecipe(recipeId: String)

    @Query("DELETE FROM steps WHERE recipeId = :recipeId")
    suspend fun deleteStepsByRecipe(recipeId: String)

    @Query("DELETE FROM recipe_tag_cross_ref WHERE recipeId = :recipeId")
    suspend fun deleteTagRefsByRecipe(recipeId: String)

    @Query("UPDATE recipes SET syncStatus = 3, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteRecipe(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun hardDeleteRecipe(id: String)

    @Transaction
    suspend fun hardDeleteRecipeDeep(id: String) {
        deleteIngredientsByRecipe(id)
        deleteStepsByRecipe(id)
        deleteTagRefsByRecipe(id)
        hardDeleteRecipe(id)
    }

    @Transaction
    suspend fun upsertRecipeFull(
        recipe: RecipeEntity,
        ingredients: List<IngredientEntity>,
        steps: List<StepEntity>,
        tagIds: List<String>
    ) {
        upsertRecipe(recipe)

        deleteIngredientsByRecipe(recipe.id)
        deleteStepsByRecipe(recipe.id)
        deleteTagRefsByRecipe(recipe.id)

        if (ingredients.isNotEmpty()) upsertIngredients(ingredients)
        if (steps.isNotEmpty()) upsertSteps(steps)
        if (tagIds.isNotEmpty()) {
            upsertTagRefs(tagIds.map { RecipeTagCrossRef(recipeId = recipe.id, tagId = it) })
        }
    }

    @Query(
        """
        SELECT DISTINCT recipeId
        FROM recipe_tag_cross_ref
        WHERE tagId IN (:tagIds)
        """
    )
    fun observeRecipeIdsByTagIds(tagIds: List<String>): Flow<List<String>>
}
