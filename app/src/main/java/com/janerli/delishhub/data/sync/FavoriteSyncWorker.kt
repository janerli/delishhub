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
import com.janerli.delishhub.data.local.entity.FavoriteEntity
import kotlinx.coroutines.tasks.await

class FavoriteSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val UNIQUE_NAME_ONE_TIME = "favorites_sync_one_time"
        const val UNIQUE_NAME_PERIODIC = "favorites_sync_periodic"

        private const val PREFS = "favorites_sync_prefs"
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

        val dao = db.favoriteDao()
        val fs = FirebaseFirestore.getInstance()

        return try {
            val uploaded = runUpload(uid, fs, dao)
            val pulled = runPull(uid, fs, dao)

            Result.success(
                workDataOf(
                    "uploaded" to uploaded,
                    "pulled" to pulled
                )
            )
        } catch (_: Exception) {
            Result.retry()
        } finally {
            db.close()
        }
    }

    /**
     * UPLOAD: local pending -> server
     * Локальные изменения “побеждают” (последние правки остаются).
     */
    private suspend fun runUpload(uid: String, fs: FirebaseFirestore, dao: com.janerli.delishhub.data.local.dao.FavoriteDao): Int {
        val pending = dao.getPendingNow(uid)
        if (pending.isEmpty()) return 0

        var uploaded = 0
        for (f in pending) {
            val isDeleted = f.syncStatus == SyncStatus.DELETED

            val payload = hashMapOf<String, Any?>(
                "userId" to f.userId,
                "recipeId" to f.recipeId,
                "createdAt" to f.createdAt,
                "updatedAt" to f.updatedAt,
                "isDeleted" to isDeleted
            )

            fs.collection("users")
                .document(uid)
                .collection("favorites")
                .document(f.recipeId)
                .set(payload, SetOptions.merge())
                .await()

            uploaded++

            if (isDeleted) dao.hardDelete(f.userId, f.recipeId)
            else dao.markSynced(f.userId, f.recipeId)
        }

        return uploaded
    }

    /**
     * PULL: server -> local
     * - initial pull: если lastPullAt == 0 → тянем все
     * - иначе тянем только updatedAt > lastPullAt
     * - conflict guard: не затираем локальные pending, если они новее сервера
     */
    private suspend fun runPull(uid: String, fs: FirebaseFirestore, dao: com.janerli.delishhub.data.local.dao.FavoriteDao): Int {
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastPullAt = prefs.getLong(KEY_LAST_PULL_AT, 0L)

        val query = fs.collection("users")
            .document(uid)
            .collection("favorites")
            .let { q ->
                if (lastPullAt == 0L) q else q.whereGreaterThan("updatedAt", lastPullAt)
            }

        val snap = query.get().await()

        var pulled = 0
        var maxUpdatedAt = lastPullAt

        for (doc in snap.documents) {
            val recipeId = doc.getString("recipeId") ?: doc.id
            val userId = doc.getString("userId") ?: uid
            val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
            val updatedAt = doc.getLong("updatedAt") ?: createdAt
            val isDeleted = doc.getBoolean("isDeleted") ?: false

            if (updatedAt > maxUpdatedAt) maxUpdatedAt = updatedAt

            // conflict guard: если локально pending и локальная версия новее — пропускаем сервер
            val local = dao.getNow(userId, recipeId)
            if (local != null && local.syncStatus != SyncStatus.SYNCED && local.updatedAt > updatedAt) {
                continue
            }

            if (isDeleted) {
                dao.hardDelete(userId, recipeId)
                pulled++
                continue
            }

            dao.addFavorite(
                FavoriteEntity(
                    userId = userId,
                    recipeId = recipeId,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                    syncStatus = SyncStatus.SYNCED
                )
            )
            dao.markSynced(userId, recipeId)
            pulled++
        }

        prefs.edit().putLong(KEY_LAST_PULL_AT, maxUpdatedAt).apply()
        return pulled
    }
}
