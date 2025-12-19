package com.janerli.delishhub.data.sync

import android.content.Context
import androidx.room.Room
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.janerli.delishhub.data.local.AppDatabase
import com.janerli.delishhub.data.local.DbConfig
import com.janerli.delishhub.data.local.dao.RecipeDao
import com.janerli.delishhub.data.local.entity.IngredientEntity
import com.janerli.delishhub.data.local.entity.RecipeEntity
import com.janerli.delishhub.data.local.entity.StepEntity
import kotlinx.coroutines.tasks.await

class RecipeSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val UNIQUE_NAME_ONE_TIME = "recipe_sync_one_time"
        const val UNIQUE_NAME_PERIODIC = "recipe_sync_periodic"

        private const val PREFS = "recipe_sync_prefs"
        private const val KEY_LAST_PULL_OWN_AT = "last_pull_own_at"
        private const val KEY_LAST_PULL_PUBLIC_AT = "last_pull_public_at"

        // ❗ тот же админ, что и в SessionManager
        private const val ADMIN_EMAIL = "admin@mail.ru"

        fun defaultConstraints(): Constraints =
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
    }

    override suspend fun doWork(): Result {
        val user = FirebaseAuth.getInstance().currentUser
        val uid: String? = user?.uid
        val email = user?.email
        val isAdmin = !email.isNullOrBlank() && email.equals(ADMIN_EMAIL, ignoreCase = true)

        val firestore = FirebaseFirestore.getInstance()

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            DbConfig.DB_NAME
        )
            .fallbackToDestructiveMigration()
            .build()

        val recipeDao = db.recipeDao()

        return try {
            val isInitialPull = recipeDao.countNotDeletedNow() == 0

            var uploaded = 0
            var pulledOwn = 0
            val pulledPublic: Int

            // upload pending
            if (uid != null) {
                uploaded = uploadPending(uid, isAdmin, firestore, recipeDao)
                pulledOwn = pullOwnUpdates(uid, firestore, recipeDao, isInitialPull)
            }

            // public pull
            pulledPublic = pullPublicUpdates(firestore, recipeDao, isInitialPull)

            Result.success(
                workDataOf(
                    "uploaded" to uploaded,
                    "pulled_own" to pulledOwn,
                    "pulled_public" to pulledPublic,
                    "initial_pull" to isInitialPull,
                    "mode" to when {
                        uid == null -> "guest"
                        isAdmin -> "admin"
                        else -> "user"
                    }
                )
            )
        } catch (_: Exception) {
            Result.retry()
        } finally {
            db.close()
        }
    }

    // ---------------------------------------------------------------------
    // Upload: local -> Firestore
    // User: только свои
    // Admin: любые (включая чужие DELETED / UPDATED)
    // ---------------------------------------------------------------------

    private suspend fun uploadPending(
        uid: String,
        isAdmin: Boolean,
        firestore: FirebaseFirestore,
        recipeDao: RecipeDao
    ): Int {
        val pending = recipeDao.getPendingBaseNow()
        if (pending.isEmpty()) return 0

        var uploaded = 0

        for (base in pending) {
            val full = recipeDao.getRecipeFull(base.id) ?: continue
            val recipe = full.recipe

            val canUpload =
                recipe.ownerId == uid || isAdmin

            if (!canUpload) {
                // обычный пользователь не может пушить чужое
                recipeDao.markSynced(recipe.id)
                continue
            }

            val isDeleted = recipe.syncStatus == SyncStatus.DELETED

            val payload = hashMapOf<String, Any?>(
                "id" to recipe.id,
                "ownerId" to recipe.ownerId,
                "title" to recipe.title,
                "description" to recipe.description,
                "categoryId" to recipe.categoryId,
                "difficulty" to recipe.difficulty,
                "cookTimeMin" to recipe.cookTimeMin,
                "calories" to recipe.calories,
                "protein" to recipe.protein,
                "fat" to recipe.fat,
                "carbs" to recipe.carbs,
                "isVegetarian" to recipe.isVegetarian,
                "isPublic" to recipe.isPublic,
                "ratingAvg" to recipe.ratingAvg,
                "ratingsCount" to recipe.ratingsCount,
                "mainImageUrl" to recipe.mainImageUrl,
                "createdAt" to recipe.createdAt,
                "updatedAt" to recipe.updatedAt,
                "isDeleted" to isDeleted,
                "ingredients" to full.ingredients.map {
                    mapOf(
                        "id" to it.id,
                        "name" to it.name,
                        "amount" to it.amount,
                        "unit" to it.unit,
                        "position" to it.position
                    )
                },
                "steps" to full.steps.map {
                    mapOf(
                        "id" to it.id,
                        "text" to it.text,
                        "photoUrl" to it.photoUrl,
                        "position" to it.position
                    )
                },
                "tagIds" to full.tags.map { it.id }
            )

            firestore.collection("recipes")
                .document(recipe.id)
                .set(payload, SetOptions.merge())
                .await()

            uploaded++

            if (isDeleted) {
                recipeDao.hardDeleteRecipeDeep(recipe.id)
            } else {
                recipeDao.markSynced(recipe.id)
            }
        }

        return uploaded
    }

    // ---------------------------------------------------------------------
    // Pull own
    // ---------------------------------------------------------------------

    private suspend fun pullOwnUpdates(
        uid: String,
        firestore: FirebaseFirestore,
        recipeDao: RecipeDao,
        isInitialPull: Boolean
    ): Int {
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastPullAt = prefs.getLong(KEY_LAST_PULL_OWN_AT, 0L)

        val query = firestore.collection("recipes")
            .whereEqualTo("ownerId", uid)
            .let { q ->
                if (isInitialPull) q else q.whereGreaterThan("updatedAt", lastPullAt)
            }

        val snap = query.get().await()

        var pulled = 0
        var maxUpdatedAt = if (isInitialPull) 0L else lastPullAt

        for (doc in snap.documents) {
            val id = doc.getString("id") ?: doc.id
            val remoteUpdatedAt = doc.getLong("updatedAt") ?: 0L
            val isDeleted = doc.getBoolean("isDeleted") ?: false

            if (remoteUpdatedAt > maxUpdatedAt) maxUpdatedAt = remoteUpdatedAt

            val local = recipeDao.getRecipeBaseNow(id)
            if (local != null && local.syncStatus != SyncStatus.SYNCED && local.updatedAt > remoteUpdatedAt) {
                continue
            }

            if (isDeleted) {
                recipeDao.hardDeleteRecipeDeep(id)
                pulled++
                continue
            }

            val recipe = mapDocToRecipeEntity(
                docId = id,
                ownerIdFallback = uid,
                docOwnerId = doc.getString("ownerId"),
                docTitle = doc.getString("title"),
                docDescription = doc.getString("description"),
                docCategoryId = doc.getString("categoryId"),
                docDifficulty = doc.getLong("difficulty"),
                docCookTimeMin = doc.getLong("cookTimeMin"),
                docCalories = doc.getLong("calories"),
                docProtein = doc.getDouble("protein"),
                docFat = doc.getDouble("fat"),
                docCarbs = doc.getDouble("carbs"),
                docIsVegetarian = doc.getBoolean("isVegetarian"),
                docIsPublic = doc.getBoolean("isPublic"),
                docRatingAvg = doc.getDouble("ratingAvg"),
                docRatingsCount = doc.getLong("ratingsCount"),
                docMainImageUrl = doc.getString("mainImageUrl"),
                docCreatedAt = doc.getLong("createdAt"),
                updatedAt = remoteUpdatedAt,
                syncStatus = SyncStatus.SYNCED
            )

            val ingredients = mapDocToIngredients(id, doc.get("ingredients"))
            val steps = mapDocToSteps(id, doc.get("steps"))
            val tagIds = (doc.get("tagIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

            recipeDao.upsertRecipeFull(recipe, ingredients, steps, tagIds)
            recipeDao.markSynced(id)

            pulled++
        }

        prefs.edit().putLong(KEY_LAST_PULL_OWN_AT, maxUpdatedAt).apply()
        return pulled
    }

    // ---------------------------------------------------------------------
    // Pull public
    // ---------------------------------------------------------------------

    private suspend fun pullPublicUpdates(
        firestore: FirebaseFirestore,
        recipeDao: RecipeDao,
        isInitialPull: Boolean
    ): Int {
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastPullAt = prefs.getLong(KEY_LAST_PULL_PUBLIC_AT, 0L)

        val query = firestore.collection("recipes")
            .whereEqualTo("isPublic", true)
            .let { q ->
                if (isInitialPull) q else q.whereGreaterThan("updatedAt", lastPullAt)
            }

        val snap = query.get().await()

        var pulled = 0
        var maxUpdatedAt = if (isInitialPull) 0L else lastPullAt

        for (doc in snap.documents) {
            val id = doc.getString("id") ?: doc.id
            val remoteUpdatedAt = doc.getLong("updatedAt") ?: 0L
            val isDeleted = doc.getBoolean("isDeleted") ?: false

            if (remoteUpdatedAt > maxUpdatedAt) maxUpdatedAt = remoteUpdatedAt

            val local = recipeDao.getRecipeBaseNow(id)
            if (local != null && local.syncStatus != SyncStatus.SYNCED && local.updatedAt > remoteUpdatedAt) {
                continue
            }

            if (isDeleted) {
                recipeDao.hardDeleteRecipeDeep(id)
                pulled++
                continue
            }

            val ownerId = doc.getString("ownerId") ?: "unknown"

            val recipe = mapDocToRecipeEntity(
                docId = id,
                ownerIdFallback = ownerId,
                docOwnerId = ownerId,
                docTitle = doc.getString("title"),
                docDescription = doc.getString("description"),
                docCategoryId = doc.getString("categoryId"),
                docDifficulty = doc.getLong("difficulty"),
                docCookTimeMin = doc.getLong("cookTimeMin"),
                docCalories = doc.getLong("calories"),
                docProtein = doc.getDouble("protein"),
                docFat = doc.getDouble("fat"),
                docCarbs = doc.getDouble("carbs"),
                docIsVegetarian = doc.getBoolean("isVegetarian"),
                docIsPublic = doc.getBoolean("isPublic"),
                docRatingAvg = doc.getDouble("ratingAvg"),
                docRatingsCount = doc.getLong("ratingsCount"),
                docMainImageUrl = doc.getString("mainImageUrl"),
                docCreatedAt = doc.getLong("createdAt"),
                updatedAt = remoteUpdatedAt,
                syncStatus = SyncStatus.SYNCED
            )

            val ingredients = mapDocToIngredients(id, doc.get("ingredients"))
            val steps = mapDocToSteps(id, doc.get("steps"))
            val tagIds = (doc.get("tagIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

            recipeDao.upsertRecipeFull(recipe, ingredients, steps, tagIds)
            recipeDao.markSynced(id)

            pulled++
        }

        prefs.edit().putLong(KEY_LAST_PULL_PUBLIC_AT, maxUpdatedAt).apply()
        return pulled
    }

    // ---------------------------------------------------------------------
    // Mapping helpers
    // ---------------------------------------------------------------------

    private fun mapDocToRecipeEntity(
        docId: String,
        ownerIdFallback: String,
        docOwnerId: String?,
        docTitle: String?,
        docDescription: String?,
        docCategoryId: String?,
        docDifficulty: Long?,
        docCookTimeMin: Long?,
        docCalories: Long?,
        docProtein: Double?,
        docFat: Double?,
        docCarbs: Double?,
        docIsVegetarian: Boolean?,
        docIsPublic: Boolean?,
        docRatingAvg: Double?,
        docRatingsCount: Long?,
        docMainImageUrl: String?,
        docCreatedAt: Long?,
        updatedAt: Long,
        syncStatus: Int
    ): RecipeEntity =
        RecipeEntity(
            id = docId,
            ownerId = docOwnerId ?: ownerIdFallback,
            title = docTitle ?: "",
            description = docDescription ?: "",
            categoryId = docCategoryId,
            difficulty = (docDifficulty ?: 1L).toInt(),
            cookTimeMin = (docCookTimeMin ?: 0L).toInt(),
            calories = docCalories?.toInt(),
            protein = docProtein,
            fat = docFat,
            carbs = docCarbs,
            isVegetarian = docIsVegetarian ?: false,
            isPublic = docIsPublic ?: false,
            ratingAvg = docRatingAvg,
            ratingsCount = docRatingsCount?.toInt(),
            mainImageUrl = docMainImageUrl,
            createdAt = docCreatedAt ?: System.currentTimeMillis(),
            updatedAt = updatedAt,
            syncStatus = syncStatus
        )

    private fun mapDocToIngredients(recipeId: String, raw: Any?): List<IngredientEntity> {
        val list = raw as? List<*> ?: return emptyList()
        return list.mapNotNull { item ->
            val m = item as? Map<*, *> ?: return@mapNotNull null
            val id = m["id"] as? String ?: return@mapNotNull null
            IngredientEntity(
                id = id,
                recipeId = recipeId,
                name = m["name"] as? String ?: "",
                amount = (m["amount"] as? Number)?.toDouble(),
                unit = m["unit"] as? String,
                position = (m["position"] as? Number)?.toInt() ?: 0
            )
        }
    }

    private fun mapDocToSteps(recipeId: String, raw: Any?): List<StepEntity> {
        val list = raw as? List<*> ?: return emptyList()
        return list.mapNotNull { item ->
            val m = item as? Map<*, *> ?: return@mapNotNull null
            val id = m["id"] as? String ?: return@mapNotNull null
            StepEntity(
                id = id,
                recipeId = recipeId,
                text = m["text"] as? String ?: "",
                photoUrl = m["photoUrl"] as? String,
                position = (m["position"] as? Number)?.toInt() ?: 0
            )
        }
    }
}
