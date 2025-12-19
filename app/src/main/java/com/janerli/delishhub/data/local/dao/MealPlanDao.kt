package com.janerli.delishhub.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.janerli.delishhub.data.local.entity.MealPlanEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealPlanDao {

    // UI observe (не показываем удалённые)
    @Query(
        """
        SELECT * FROM meal_plan_entries
        WHERE userId = :userId
          AND dateEpochDay = :dateEpochDay
          AND syncStatus != 3
        ORDER BY 
          CASE WHEN timeMinutes IS NULL THEN 1 ELSE 0 END,
          timeMinutes ASC,
          mealType ASC
        """
    )
    fun observeDay(userId: String, dateEpochDay: Long): Flow<List<MealPlanEntryEntity>>

    // used in repo (нужно для “добавить ингредиенты из плана”)
    @Query(
        """
        SELECT * FROM meal_plan_entries
        WHERE userId = :userId
          AND dateEpochDay = :dateEpochDay
          AND syncStatus != 3
        ORDER BY 
          CASE WHEN timeMinutes IS NULL THEN 1 ELSE 0 END,
          timeMinutes ASC,
          mealType ASC
        """
    )
    suspend fun getDayNow(userId: String, dateEpochDay: Long): List<MealPlanEntryEntity>

    @Query(
        """
        SELECT * FROM meal_plan_entries
        WHERE userId = :userId
          AND dateEpochDay = :dateEpochDay
          AND mealType = :mealType
        LIMIT 1
        """
    )
    suspend fun getSlot(userId: String, dateEpochDay: Long, mealType: String): MealPlanEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: MealPlanEntryEntity)

    // ✅ soft delete для синка
    @Query(
        """
        UPDATE meal_plan_entries
        SET syncStatus = 3, updatedAt = :updatedAt
        WHERE userId = :userId
          AND dateEpochDay = :dateEpochDay
          AND mealType = :mealType
        """
    )
    suspend fun softDeleteSlot(userId: String, dateEpochDay: Long, mealType: String, updatedAt: Long)

    // оставляем для совместимости (если где-то еще вызывается)
    @Query(
        """
        DELETE FROM meal_plan_entries
        WHERE userId = :userId AND dateEpochDay = :dateEpochDay AND mealType = :mealType
        """
    )
    suspend fun deleteSlot(userId: String, dateEpochDay: Long, mealType: String)

    // ---- sync helpers ----
    @Query("SELECT * FROM meal_plan_entries WHERE userId = :userId AND syncStatus != 0")
    suspend fun getPendingNow(userId: String): List<MealPlanEntryEntity>

    @Query("UPDATE meal_plan_entries SET syncStatus = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("DELETE FROM meal_plan_entries WHERE id = :id")
    suspend fun hardDeleteById(id: String)
}
