package com.janerli.delishhub.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import com.janerli.delishhub.data.sync.SyncStatus

@Entity(
    tableName = "favorites",
    primaryKeys = ["userId", "recipeId"],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["recipeId"]),
        Index(value = ["createdAt"])
    ]
)
data class FavoriteEntity(
    val userId: String,
    val recipeId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    /**
     * 0=SYNCED, 1=CREATED, 2=UPDATED (не используем), 3=DELETED
     */
    val syncStatus: Int = SyncStatus.SYNCED
)
