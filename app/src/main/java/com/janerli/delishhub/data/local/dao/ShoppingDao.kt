package com.janerli.delishhub.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.janerli.delishhub.data.local.entity.ShoppingItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {

    // UI observe (не показываем удалённые)
    @Query(
        """
        SELECT * FROM shopping_items
        WHERE userId = :userId
          AND syncStatus != 3
        ORDER BY isChecked ASC, updatedAt DESC
        """
    )
    fun observeAll(userId: String): Flow<List<ShoppingItemEntity>>

    @Query(
        """
        SELECT * FROM shopping_items
        WHERE userId = :userId
          AND syncStatus != 3
        """
    )
    suspend fun getAllNow(userId: String): List<ShoppingItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ShoppingItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ShoppingItemEntity>)

    @Query(
        """
        UPDATE shopping_items
        SET isChecked = :checked, updatedAt = :updatedAt,
            syncStatus = CASE WHEN syncStatus = 1 THEN 1 ELSE 2 END
        WHERE id = :id
        """
    )
    suspend fun setChecked(id: String, checked: Boolean, updatedAt: Long)

    // soft delete для синка
    @Query("UPDATE shopping_items SET syncStatus = 3, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteById(id: String, updatedAt: Long)

    // оставим, если где-то используется (но репозиторий переведём на soft)
    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun deleteById(id: String)

    // “очистка купленного” — делаем soft delete, чтобы синкнуть удаления
    @Query(
        """
        UPDATE shopping_items
        SET syncStatus = 3, updatedAt = :updatedAt
        WHERE userId = :userId
          AND isChecked = 1
          AND syncStatus != 3
        """
    )
    suspend fun softDeleteChecked(userId: String, updatedAt: Long)

    // старый метод оставлен для совместимости (если где-то ещё вызывается)
    @Query("DELETE FROM shopping_items WHERE userId = :userId AND isChecked = 1")
    suspend fun clearChecked(userId: String)

    // ---- sync helpers ----
    @Query("SELECT * FROM shopping_items WHERE userId = :userId AND syncStatus != 0")
    suspend fun getPendingNow(userId: String): List<ShoppingItemEntity>

    @Query("UPDATE shopping_items SET syncStatus = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun hardDeleteById(id: String)
}
