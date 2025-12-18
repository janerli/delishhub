package com.janerli.delishhub.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.janerli.delishhub.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    // ✅ учитываем soft delete
    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM favorites
            WHERE userId = :userId
              AND recipeId = :recipeId
              AND syncStatus != 3
        )
        """
    )
    fun observeIsFavorite(userId: String, recipeId: String): Flow<Boolean>

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM favorites
            WHERE userId = :userId
              AND recipeId = :recipeId
              AND syncStatus != 3
        )
        """
    )
    suspend fun isFavorite(userId: String, recipeId: String): Boolean

    // --- оставляем твоё API, но делаем правильное поведение ---

    // вместо IGNORE используем REPLACE: если запись уже была DELETED — восстановим
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(entity: FavoriteEntity)

    // вместо hard delete — soft delete (для оффлайн синка)
    @Query(
        """
        UPDATE favorites
        SET syncStatus = 3, updatedAt = :updatedAt
        WHERE userId = :userId AND recipeId = :recipeId
        """
    )
    suspend fun removeFavorite(userId: String, recipeId: String, updatedAt: Long = System.currentTimeMillis())

    // очистка — тоже soft delete, чтобы синкнуть удаления
    @Query(
        """
        UPDATE favorites
        SET syncStatus = 3, updatedAt = :updatedAt
        WHERE userId = :userId AND syncStatus != 3
        """
    )
    suspend fun clearFavorites(userId: String, updatedAt: Long = System.currentTimeMillis())

    // ✅ не отдаём удалённые
    @Query(
        """
        SELECT recipeId FROM favorites
        WHERE userId = :userId
          AND syncStatus != 3
        ORDER BY createdAt DESC
        """
    )
    fun observeFavoriteRecipeIds(userId: String): Flow<List<String>>

    // --- методы для синка ---

    @Query("SELECT * FROM favorites WHERE userId = :userId AND syncStatus != 0")
    suspend fun getPendingNow(userId: String): List<FavoriteEntity>

    @Query(
        """
        UPDATE favorites
        SET syncStatus = 0
        WHERE userId = :userId AND recipeId = :recipeId
        """
    )
    suspend fun markSynced(userId: String, recipeId: String)

    @Query("DELETE FROM favorites WHERE userId = :userId AND recipeId = :recipeId")
    suspend fun hardDelete(userId: String, recipeId: String)

    @Query("SELECT * FROM favorites WHERE userId = :userId AND recipeId = :recipeId LIMIT 1")
    suspend fun getNow(userId: String, recipeId: String): FavoriteEntity?
}
