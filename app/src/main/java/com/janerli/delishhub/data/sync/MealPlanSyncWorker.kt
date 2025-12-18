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
import com.janerli.delishhub.data.local.dao.MealPlanDao
import com.janerli.delishhub.data.local.entity.MealPlanEntryEntity
import kotlinx.coroutines.tasks.await

class MealPlanSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val UNIQUE_NAME_ONE_TIME = "mealplan_sync_one_time"
        const val UNIQUE_NAME_PERIODIC = "mealplan_sync_periodic"

        private const val PREFS = "mealplan_sync_prefs"
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

        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, DbConfig.DB_NAME)
            .fallbackToDestructiveMigration()
            .build()

        val dao = db.mealPlanDao()
        val firestore = FirebaseFirestore.getInstance()

        return try {
            val uploaded = runUpload(uid, firestore, dao)
            val pulled = runPull(uid, firestore, dao)
            Result.success(workDataOf("uploaded" to uploaded, "pulled" to pulled))
        } catch (_: Exception) {
            Result.retry()
        } finally {
            db.close()
        }
    }

    private suspend fun runUpload(uid: String, fs: FirebaseFirestore, dao: MealPlanDao): Int {
        val pending = dao.getPendingNow(uid)
        if (pending.isEmpty()) return 0

        var count = 0
        for (e in pending) {
            val isDeleted = e.syncStatus == SyncStatus.DELETED

            val payload = hashMapOf<String, Any?>(
                "id" to e.id,
                "userId" to uid,
                "dateEpochDay" to e.dateEpochDay,
                "mealType" to e.mealType,
                "recipeId" to e.recipeId,
                "servings" to e.servings,
                "createdAt" to e.createdAt,
                "updatedAt" to e.updatedAt,
                "isDeleted" to isDeleted
            )

            // ✅ Новый путь: users/{uid}/mealPlan/{entryId}
            fs.collection("users")
                .document(uid)
                .collection("mealPlan")
                .document(e.id)
                .set(payload, SetOptions.merge())
                .await()

            count++
            if (isDeleted) dao.hardDeleteById(e.id) else dao.markSynced(e.id)
        }
        return count
    }

    private suspend fun runPull(uid: String, fs: FirebaseFirestore, dao: MealPlanDao): Int {
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastPullAt = prefs.getLong(KEY_LAST_PULL_AT, 0L)

        val snap = fs.collection("users")
            .document(uid)
            .collection("mealPlan")
            .whereGreaterThan("updatedAt", lastPullAt)
            .get()
            .await()

        var pulled = 0
        var maxUpdatedAt = lastPullAt

        for (doc in snap.documents) {
            val id = doc.getString("id") ?: doc.id
            val updatedAt = doc.getLong("updatedAt") ?: 0L
            val isDeleted = doc.getBoolean("isDeleted") ?: false
            if (updatedAt > maxUpdatedAt) maxUpdatedAt = updatedAt

            if (isDeleted) {
                dao.hardDeleteById(id)
                pulled++
                continue
            }

            val entity = MealPlanEntryEntity(
                id = id,
                userId = uid,
                dateEpochDay = doc.getLong("dateEpochDay") ?: 0L,
                mealType = doc.getString("mealType") ?: "",
                recipeId = doc.getString("recipeId") ?: "",
                servings = (doc.getLong("servings") ?: 1L).toInt(),
                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                updatedAt = updatedAt,
                syncStatus = SyncStatus.SYNCED
            )
            dao.upsert(entity)
            dao.markSynced(id)
            pulled++
        }

        prefs.edit().putLong(KEY_LAST_PULL_AT, maxUpdatedAt).apply()
        return pulled
    }
}
