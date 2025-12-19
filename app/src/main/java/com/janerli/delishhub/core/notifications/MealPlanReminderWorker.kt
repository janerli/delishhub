package com.janerli.delishhub.data.notifications

import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.auth.FirebaseAuth
import com.janerli.delishhub.data.local.AppDatabase
import com.janerli.delishhub.data.local.DbConfig
import java.time.LocalDate

class MealPlanReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val UNIQUE_NAME_DAILY = "mealplan_reminder_daily"

        private val MEAL_TYPES = listOf("BREAKFAST", "LUNCH", "DINNER", "SNACK")
    }

    override suspend fun doWork(): Result {
        val user = FirebaseAuth.getInstance().currentUser
            ?: return Result.success(workDataOf("skipped" to "no_user"))

        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) return Result.success(workDataOf("skipped" to "no_permission"))
        }

        val uid = user.uid

        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, DbConfig.DB_NAME)
            .addMigrations(AppDatabase.MIGRATION_7_8)
            .build()

        return try {
            val mealPlanDao = db.mealPlanDao()
            val recipeDao = db.recipeDao()

            val today = LocalDate.now()
            val days = listOf(today, today.plusDays(1)) // ✅ сегодня + завтра

            var scheduledCount = 0
            var cancelledCount = 0

            for (day in days) {
                val epochDay = day.toEpochDay()

                // ✅ сначала отменяем все 4 слота на этот день (чтобы не оставались старые воркеры)
                for (t in MEAL_TYPES) {
                    MealPlanReminderScheduler.cancelSlot(applicationContext, epochDay, t)
                    cancelledCount++
                }

                // ✅ потом ставим актуальные timed-слоты
                val entries = mealPlanDao.getDayNow(uid, epochDay)
                    .filter { it.syncStatus != 3 }
                    .filter { it.timeMinutes != null }

                for (e in entries) {
                    val title = recipeDao.getRecipeBaseNow(e.recipeId)?.title?.trim()
                    if (title.isNullOrBlank()) continue

                    val ok = MealPlanReminderScheduler.scheduleSlotOnce(
                        context = applicationContext,
                        dateEpochDay = epochDay,
                        mealType = e.mealType,
                        timeMinutes = e.timeMinutes!!,
                        recipeTitle = title
                    )
                    if (ok) scheduledCount++
                }
            }

            // ✅ гарантируем daily рескейджулер (на ближайшие 00:05)
            MealPlanReminderScheduler.scheduleDaily(applicationContext)

            Result.success(
                workDataOf(
                    "scheduled" to scheduledCount,
                    "cancelled" to cancelledCount,
                    "days" to days.size
                )
            )
        } catch (_: Exception) {
            Result.retry()
        } finally {
            db.close()
        }
    }
}
