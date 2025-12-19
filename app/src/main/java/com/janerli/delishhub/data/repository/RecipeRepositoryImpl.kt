package com.janerli.delishhub.data.repository

import com.janerli.delishhub.data.local.dao.FavoriteDao
import com.janerli.delishhub.data.local.dao.MealPlanDao
import com.janerli.delishhub.data.local.dao.RecipeDao
import com.janerli.delishhub.data.local.dao.ShoppingDao
import com.janerli.delishhub.data.local.dao.TagDao
import com.janerli.delishhub.data.local.entity.FavoriteEntity
import com.janerli.delishhub.data.local.entity.MealPlanEntryEntity
import com.janerli.delishhub.data.local.entity.RecipeEntity
import com.janerli.delishhub.data.local.entity.ShoppingItemEntity
import com.janerli.delishhub.data.local.entity.TagEntity
import com.janerli.delishhub.data.local.model.RecipeFull
import com.janerli.delishhub.data.sync.SyncStatus
import com.janerli.delishhub.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import kotlin.math.abs

class RecipeRepositoryImpl(
    private val recipeDao: RecipeDao,
    private val favoriteDao: FavoriteDao,
    private val mealPlanDao: MealPlanDao,
    private val shoppingDao: ShoppingDao,
    private val tagDao: TagDao
) : RecipeRepository {

    override fun observeCatalog(
        ownerId: String,
        onlyMine: Boolean,
        filters: RecipeRepository.RecipeFilters,
        sort: RecipeRepository.RecipeSort
    ): Flow<List<RecipeEntity>> {
        return recipeDao.observeCatalog(
            query = filters.query,
            categoryId = null,
            vegetarianOnly = false,
            hasPhoto = false,
            minDifficulty = filters.minDifficulty,
            maxDifficulty = filters.maxDifficulty,
            minCookTime = filters.minTime,
            maxCookTime = filters.maxTime,
            onlyMine = onlyMine,
            ownerId = ownerId,
            publicOnly = false,
            sort = sort.name
        )
    }

    override fun observeFavorites(
        userId: String,
        filters: RecipeRepository.RecipeFilters,
        sort: RecipeRepository.RecipeSort
    ): Flow<List<RecipeEntity>> {
        return recipeDao.observeFavorites(
            userId = userId,
            query = filters.query,
            categoryId = null,
            vegetarianOnly = false,
            hasPhoto = false,
            minDifficulty = filters.minDifficulty,
            maxDifficulty = filters.maxDifficulty,
            minCookTime = filters.minTime,
            maxCookTime = filters.maxTime,
            sort = sort.name
        )
    }

    override fun observeIsFavorite(userId: String, recipeId: String): Flow<Boolean> {
        return favoriteDao.observeIsFavorite(userId, recipeId)
    }

    override suspend fun toggleFavorite(userId: String, recipeId: String) {
        val now = System.currentTimeMillis()
        val isFav = favoriteDao.isFavorite(userId, recipeId)

        if (isFav) {
            // ✅ soft delete
            favoriteDao.removeFavorite(userId, recipeId, now)
        } else {
            val existing = favoriteDao.getNow(userId, recipeId)
            val createdAt = existing?.createdAt ?: now

            val entity = FavoriteEntity(
                userId = userId,
                recipeId = recipeId,
                createdAt = createdAt,
                updatedAt = now,
                syncStatus = SyncStatus.CREATED
            )
            favoriteDao.addFavorite(entity)
        }
    }

    override fun observeRecipeFull(recipeId: String): Flow<RecipeFull?> =
        recipeDao.observeRecipeFull(recipeId)

    override suspend fun getRecipeFull(recipeId: String): RecipeFull? =
        recipeDao.getRecipeFull(recipeId)

    override suspend fun upsertRecipeFull(
        recipe: RecipeEntity,
        ingredients: List<com.janerli.delishhub.data.local.entity.IngredientEntity>,
        steps: List<com.janerli.delishhub.data.local.entity.StepEntity>,
        tagIds: List<String>
    ) {
        val now = System.currentTimeMillis()
        val existing = recipeDao.getRecipeBaseNow(recipe.id)

        val status = when {
            existing == null -> SyncStatus.CREATED
            existing.syncStatus == SyncStatus.CREATED -> SyncStatus.CREATED
            existing.syncStatus == SyncStatus.DELETED -> SyncStatus.UPDATED
            else -> SyncStatus.UPDATED
        }

        val fixed = recipe.copy(
            createdAt = existing?.createdAt ?: recipe.createdAt,
            updatedAt = now,
            syncStatus = status
        )

        recipeDao.upsertRecipeFull(fixed, ingredients, steps, tagIds)
    }

    override suspend fun deleteRecipe(recipeId: String, hardDelete: Boolean) {
        if (hardDelete) recipeDao.hardDeleteRecipeDeep(recipeId)
        else recipeDao.softDeleteRecipe(recipeId, System.currentTimeMillis())
    }

    // ---------------- TAGS ----------------

    override fun observeAllTags(): Flow<List<TagEntity>> =
        tagDao.observeAll()

    override suspend fun upsertTag(name: String) {
        val clean = name.trim()
        if (clean.isBlank()) return

        // не создаём дубль по имени
        val existing = tagDao.getByNameNow(clean)
        if (existing != null) return

        tagDao.insertIgnore(
            TagEntity(
                id = UUID.randomUUID().toString(),
                name = clean
            )
        )
    }

    override suspend fun deleteTag(tagId: String) {
        tagDao.deleteById(tagId)
    }

    override fun observeRecipeIdsByTagIds(tagIds: List<String>): Flow<List<String>> =
        recipeDao.observeRecipeIdsByTagIds(tagIds)

    // -------- Planner --------

    override fun observeMealPlanDay(userId: String, dateEpochDay: Long): Flow<List<MealPlanEntryEntity>> =
        mealPlanDao.observeDay(userId, dateEpochDay)

    override suspend fun setMeal(
        userId: String,
        dateEpochDay: Long,
        mealType: String,
        recipeId: String,
        servings: Int,
        timeMinutes: Int?
    ) {
        val now = System.currentTimeMillis()
        val existing = mealPlanDao.getSlot(userId, dateEpochDay, mealType)

        val status = when {
            existing == null -> SyncStatus.CREATED
            existing.syncStatus == SyncStatus.CREATED -> SyncStatus.CREATED
            existing.syncStatus == SyncStatus.DELETED -> SyncStatus.UPDATED
            else -> SyncStatus.UPDATED
        }

        // ✅ если время не передали — НЕ затираем существующее
        val resolvedTime = timeMinutes ?: existing?.timeMinutes

        val entry = MealPlanEntryEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            userId = userId,
            dateEpochDay = dateEpochDay,
            mealType = mealType,
            timeMinutes = resolvedTime,
            recipeId = recipeId,
            servings = servings,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            syncStatus = status
        )
        mealPlanDao.upsert(entry)
    }

    override suspend fun updateMealTime(
        userId: String,
        dateEpochDay: Long,
        mealType: String,
        timeMinutes: Int?
    ) {
        val now = System.currentTimeMillis()
        val existing = mealPlanDao.getSlot(userId, dateEpochDay, mealType) ?: return

        // не обновляем “удалённые” (пусть сначала назначат рецепт заново)
        if (existing.syncStatus == SyncStatus.DELETED) return

        val nextStatus = when (existing.syncStatus) {
            SyncStatus.CREATED -> SyncStatus.CREATED
            SyncStatus.DELETED -> SyncStatus.UPDATED
            else -> SyncStatus.UPDATED
        }

        mealPlanDao.upsert(
            existing.copy(
                timeMinutes = timeMinutes,
                updatedAt = now,
                syncStatus = nextStatus
            )
        )
    }

    override suspend fun removeMeal(userId: String, dateEpochDay: Long, mealType: String) {
        mealPlanDao.softDeleteSlot(userId, dateEpochDay, mealType, System.currentTimeMillis())
    }

    // -------- Shopping --------

    override fun observeShopping(userId: String): Flow<List<ShoppingItemEntity>> =
        shoppingDao.observeAll(userId)

    override suspend fun addShoppingManual(userId: String, name: String, qtyText: String?) {
        val now = System.currentTimeMillis()
        val cleanName = name.trim()
        if (cleanName.isBlank()) return

        val item = ShoppingItemEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            name = cleanName,
            amount = null,
            unit = null,
            qtyText = qtyText?.trim()?.takeIf { it.isNotBlank() },
            isChecked = false,
            createdAt = now,
            updatedAt = now,
            syncStatus = SyncStatus.CREATED
        )
        shoppingDao.upsert(item)
    }

    override suspend fun toggleShoppingChecked(userId: String, id: String, checked: Boolean) {
        shoppingDao.setChecked(id, checked, System.currentTimeMillis())
    }

    override suspend fun deleteShoppingItem(userId: String, id: String) {
        shoppingDao.softDeleteById(id, System.currentTimeMillis())
    }

    override suspend fun clearCheckedShopping(userId: String) {
        shoppingDao.softDeleteChecked(userId, System.currentTimeMillis())
    }

    override suspend fun addToShoppingFromRecipe(userId: String, recipeId: String) {
        val full = recipeDao.getRecipeFull(recipeId) ?: return
        addIngredientsToShopping(userId, full.ingredients.map { Triple(it.name, it.amount, it.unit) })
    }

    override suspend fun addToShoppingFromPlanDay(userId: String, dateEpochDay: Long) {
        val entries = mealPlanDao.getDayNow(userId, dateEpochDay)
        if (entries.isEmpty()) return

        val allTriples = mutableListOf<Triple<String, Double?, String?>>()
        for (e in entries) {
            val full = recipeDao.getRecipeFull(e.recipeId) ?: continue
            full.ingredients.forEach { ing -> allTriples.add(Triple(ing.name, ing.amount, ing.unit)) }
        }
        addIngredientsToShopping(userId, allTriples)
    }

    private suspend fun addIngredientsToShopping(
        userId: String,
        ingredients: List<Triple<String, Double?, String?>>
    ) {
        val now = System.currentTimeMillis()
        val existing = shoppingDao.getAllNow(userId)

        fun key(name: String, unit: String?): String =
            name.trim().lowercase() + "||" + (unit?.trim()?.lowercase() ?: "")

        val map = existing.associateBy { key(it.name, it.unit) }.toMutableMap()

        for ((rawName, amount, unitRaw) in ingredients) {
            val name = rawName.trim()
            if (name.isBlank()) continue
            val unit = unitRaw?.trim()?.takeIf { it.isNotBlank() }

            val k = key(name, unit)
            val prev = map[k]

            if (prev == null) {
                val item = ShoppingItemEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    name = name,
                    amount = amount,
                    unit = unit,
                    qtyText = if (amount == null) "по рецепту" else null,
                    isChecked = false,
                    createdAt = now,
                    updatedAt = now,
                    syncStatus = SyncStatus.CREATED
                )
                map[k] = item
            } else {
                val newAmount = if (prev.amount != null && amount != null) prev.amount + amount else prev.amount
                val newQtyText = when {
                    prev.amount != null && amount != null -> null
                    prev.qtyText.isNullOrBlank() -> "по рецепту"
                    else -> prev.qtyText
                }

                val nextStatus = when (prev.syncStatus) {
                    SyncStatus.CREATED -> SyncStatus.CREATED
                    SyncStatus.DELETED -> SyncStatus.UPDATED
                    else -> SyncStatus.UPDATED
                }

                map[k] = prev.copy(
                    amount = newAmount,
                    qtyText = newQtyText,
                    updatedAt = now,
                    syncStatus = nextStatus
                )
            }
        }

        val normalized = map.values.map { it ->
            val a = it.amount
            if (a != null) {
                val intA = a.toInt()
                if (abs(a - intA.toDouble()) < 1e-9) it.copy(amount = intA.toDouble()) else it
            } else it
        }

        shoppingDao.upsertAll(normalized)
    }
}
