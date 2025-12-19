package com.janerli.delishhub.data.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object MealPlanReminderScheduler {

    private const val UNIQUE_NAME_REFRESH_NOW = "mealplan_reminder_refresh_now"

    fun scheduleDaily(context: Context) {
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val targetToday = LocalDateTime.of(today, LocalTime.of(0, 5))

        val target = if (now.isBefore(targetToday)) targetToday else targetToday.plusDays(1)
        val delayMs = Duration.between(now, target).toMillis().coerceAtLeast(0L)

        val req = OneTimeWorkRequestBuilder<MealPlanReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            MealPlanReminderWorker.UNIQUE_NAME_DAILY,
            ExistingWorkPolicy.REPLACE,
            req
        )
    }

    fun scheduleRefreshNow(context: Context) {
        val req = OneTimeWorkRequestBuilder<MealPlanReminderWorker>()
            .setInputData(workDataOf("reason" to "refresh_now"))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_NAME_REFRESH_NOW,
            ExistingWorkPolicy.REPLACE,
            req
        )
    }

    fun scheduleSlotOnce(
        context: Context,
        dateEpochDay: Long,
        mealType: String,
        timeMinutes: Int,
        recipeTitle: String
    ): Boolean {
        val now = LocalDateTime.now()

        val date = LocalDate.ofEpochDay(dateEpochDay)
        val h = (timeMinutes / 60).coerceIn(0, 23)
        val m = (timeMinutes % 60).coerceIn(0, 59)
        val target = LocalDateTime.of(date, LocalTime.of(h, m))

        if (!target.isAfter(now)) return false

        val delayMs = Duration.between(now, target).toMillis().coerceAtLeast(0L)

        val input = workDataOf(
            MealPlanSlotReminderWorker.KEY_DATE_EPOCH_DAY to dateEpochDay,
            MealPlanSlotReminderWorker.KEY_MEAL_TYPE to mealType,
            MealPlanSlotReminderWorker.KEY_TIME_MINUTES to timeMinutes,
            MealPlanSlotReminderWorker.KEY_RECIPE_TITLE to recipeTitle
        )

        val uniqueName = slotUniqueName(dateEpochDay, mealType)

        val req = OneTimeWorkRequestBuilder<MealPlanSlotReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(input)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueName,
            ExistingWorkPolicy.REPLACE,
            req
        )

        return true
    }

    fun cancelSlot(context: Context, dateEpochDay: Long, mealType: String) {
        WorkManager.getInstance(context).cancelUniqueWork(slotUniqueName(dateEpochDay, mealType))
    }

    private fun slotUniqueName(dateEpochDay: Long, mealType: String): String =
        "mealplan_slot_${dateEpochDay}_$mealType"

    fun cancelDaily(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(MealPlanReminderWorker.UNIQUE_NAME_DAILY)
    }
}
