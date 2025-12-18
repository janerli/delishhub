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
            // 1) UPLOAD: local pending -> server (чтобы локальные изменения "побеждали")
            val pending = dao.getPendingNow(uid)
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

                // ✅ Новый путь: users/{uid}/favorites/{recipeId}
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

            // 2) PULL: server -> local (восстановление на новом устройстве/после чистки БД)
            val snap = fs.collection("users")
                .document(uid)
                .collection("favorites")
                .get()
                .await()

            var pulled = 0

            for (doc in snap.documents) {
                val recipeId = doc.getString("recipeId") ?: doc.id
                val userId = doc.getString("userId") ?: uid
                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                val updatedAt = doc.getLong("updatedAt") ?: createdAt
                val isDeleted = doc.getBoolean("isDeleted") ?: false

                if (isDeleted) {
                    // server says deleted -> remove locally
                    dao.hardDelete(userId, recipeId)
                    pulled++
                } else {
                    // server says exists -> upsert locally as SYNCED
                    dao.addFavorite(
                        FavoriteEntity(
                            userId = userId,
                            recipeId = recipeId,
                            createdAt = createdAt,
                            updatedAt = updatedAt,
                            syncStatus = SyncStatus.SYNCED
                        )
                    )
                    pulled++
                }
            }

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
}
