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
        private const val KEY_LAST_PULL_AT = "last_pull_at"

        fun defaultConstraints(): Constraints =
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
    }

    override suspend fun doWork(): Result {
        val user = FirebaseAuth.getInstance().currentUser
            ?: return Result.success(workDataOf("skipped" to "no_user"))

        val uid = user.uid
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
            val uploaded = uploadPending(uid, firestore, recipeDao)
            val pulled = pullUpdates(uid, firestore, recipeDao)
            Result.success(workDataOf("uploaded" to uploaded, "pulled" to pulled))
        } catch (_: Exception) {
            Result.retry()
        } finally {
            db.close()
        }
    }

    // ---------------------------------------------------------------------
    // Upload: local -> Firestore (общая коллекция recipes)
    // ---------------------------------------------------------------------

    private suspend fun uploadPending(
        uid: String,
        firestore: FirebaseFirestore,
        recipeDao: RecipeDao
    ): Int {
        val pending = recipeDao.getPendingBaseNow()
        if (pending.isEmpty()) return 0

        var uploaded = 0

        for (base in pending) {
            val full = recipeDao.getRecipeFull(base.id) ?: continue
            val recipe = full.recipe

            val isDeleted = recipe.syncStatus == SyncStatus.DELETED

            val payload = hashMapOf<String, Any?>(
                "id" to recipe.id,
                "ownerId" to uid,
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

            // ✅ Общая коллекция рецептов
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
    // Pull: Firestore -> local (подтягиваем только СВОИ рецепты)
    // ---------------------------------------------------------------------

    private suspend fun pullUpdates(
        uid: String,
        firestore: FirebaseFirestore,
        recipeDao: RecipeDao
    ): Int {
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastPullAt = prefs.getLong(KEY_LAST_PULL_AT, 0L)

        val snap = firestore.collection("recipes")
            .whereEqualTo("ownerId", uid)
            .whereGreaterThan("updatedAt", lastPullAt)
            .get()
            .await()

        var pulled = 0
        var maxUpdatedAt = lastPullAt

        for (doc in snap.documents) {
            val id = doc.getString("id") ?: doc.id
            val isDeleted = doc.getBoolean("isDeleted") ?: false
            val updatedAt = doc.getLong("updatedAt") ?: 0L

            if (updatedAt > maxUpdatedAt) maxUpdatedAt = updatedAt

            if (isDeleted) {
                recipeDao.hardDeleteRecipeDeep(id)
                pulled++
                continue
            }

            val recipe = RecipeEntity(
                id = id,
                ownerId = uid,
                title = doc.getString("title") ?: "",
                description = doc.getString("description") ?: "",
                categoryId = doc.getString("categoryId"),
                difficulty = (doc.getLong("difficulty") ?: 1L).toInt(),
                cookTimeMin = (doc.getLong("cookTimeMin") ?: 0L).toInt(),
                calories = doc.getLong("calories")?.toInt(),
                protein = doc.getDouble("protein"),
                fat = doc.getDouble("fat"),
                carbs = doc.getDouble("carbs"),
                isVegetarian = doc.getBoolean("isVegetarian") ?: false,
                isPublic = doc.getBoolean("isPublic") ?: false,
                ratingAvg = doc.getDouble("ratingAvg"),
                ratingsCount = doc.getLong("ratingsCount")?.toInt(),
                mainImageUrl = doc.getString("mainImageUrl"),
                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                updatedAt = updatedAt,
                syncStatus = SyncStatus.SYNCED
            )

            val ingredients = (doc.get("ingredients") as? List<*>)?.mapNotNull { item ->
                val m = item as? Map<*, *> ?: return@mapNotNull null
                IngredientEntity(
                    id = m["id"] as String,
                    recipeId = id,
                    name = m["name"] as? String ?: "",
                    amount = (m["amount"] as? Number)?.toDouble(),
                    unit = m["unit"] as? String,
                    position = (m["position"] as? Number)?.toInt() ?: 0
                )
            } ?: emptyList()

            val steps = (doc.get("steps") as? List<*>)?.mapNotNull { item ->
                val m = item as? Map<*, *> ?: return@mapNotNull null
                StepEntity(
                    id = m["id"] as String,
                    recipeId = id,
                    text = m["text"] as? String ?: "",
                    photoUrl = m["photoUrl"] as? String,
                    position = (m["position"] as? Number)?.toInt() ?: 0
                )
            } ?: emptyList()

            val tagIds = (doc.get("tagIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

            recipeDao.upsertRecipeFull(recipe, ingredients, steps, tagIds)
            recipeDao.markSynced(id)

            pulled++
        }

        prefs.edit().putLong(KEY_LAST_PULL_AT, maxUpdatedAt).apply()
        return pulled
    }
}
