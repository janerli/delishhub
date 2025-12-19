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
        val fs = FirebaseFirestore.getInstance()

        return try {
            val uploaded = runUpload(uid, fs, dao)
            val pulled = runPull(uid, fs, dao)

            Result.success(workDataOf("uploaded" to uploaded, "pulled" to pulled))
        } catch (_: Exception) {
            Result.retry()
        } finally {
            db.close()
        }
    }

    // ---------------------------------------------------------------------
    // Upload
    // ---------------------------------------------------------------------

    private suspend fun runUpload(
        uid: String,
        fs: FirebaseFirestore,
        dao: com.janerli.delishhub.data.local.dao.MealPlanDao
    ): Int {
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
                "timeMinutes" to e.timeMinutes,
                "createdAt" to e.createdAt,
                "updatedAt" to e.updatedAt,
                "isDeleted" to isDeleted
            )

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

    // ---------------------------------------------------------------------
    // Pull (initial + conflict guard)
    // ---------------------------------------------------------------------

    private suspend fun runPull(
        uid: String,
        fs: FirebaseFirestore,
        dao: com.janerli.delishhub.data.local.dao.MealPlanDao
    ): Int {
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastPullAt = prefs.getLong(KEY_LAST_PULL_AT, 0L)

        val query = fs.collection("users")
            .document(uid)
            .collection("mealPlan")
            .let { q ->
                if (lastPullAt == 0L) q else q.whereGreaterThan("updatedAt", lastPullAt)
            }

        val snap = query.get().await()

        var pulled = 0
        var maxUpdatedAt = lastPullAt

        for (doc in snap.documents) {
            val id = doc.getString("id") ?: doc.id
            val updatedAt = doc.getLong("updatedAt") ?: 0L
            val isDeleted = doc.getBoolean("isDeleted") ?: false

            if (updatedAt > maxUpdatedAt) maxUpdatedAt = updatedAt

            val local = dao.getSlot(
                uid,
                doc.getLong("dateEpochDay") ?: 0L,
                doc.getString("mealType") ?: ""
            )
            if (local != null && local.syncStatus != SyncStatus.SYNCED && local.updatedAt > updatedAt) {
                continue
            }

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
                timeMinutes = doc.getLong("timeMinutes")?.toInt(),
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
