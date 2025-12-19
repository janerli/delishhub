package com.janerli.delishhub.data.notifications

import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.auth.FirebaseAuth
import com.janerli.delishhub.core.notifications.NotificationHelper

class MealPlanSlotReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_MEAL_TYPE = "mealType"
        const val KEY_RECIPE_TITLE = "recipeTitle"
        const val KEY_TIME_MINUTES = "timeMinutes"
        const val KEY_DATE_EPOCH_DAY = "dateEpochDay"
    }

    override suspend fun doWork(): Result {
        // ✅ только залогиненный пользователь
        val user = FirebaseAuth.getInstance().currentUser
            ?: return Result.success(workDataOf("skipped" to "no_user"))

        // ✅ Android 13+: если нет разрешения — тихо выходим
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) return Result.success(workDataOf("skipped" to "no_permission"))
        }

        val mealType = inputData.getString(KEY_MEAL_TYPE) ?: return Result.success(workDataOf("skipped" to "no_mealType"))
        val recipeTitle = inputData.getString(KEY_RECIPE_TITLE) ?: return Result.success(workDataOf("skipped" to "no_title"))
        val timeMinutes = inputData.getInt(KEY_TIME_MINUTES, -1).takeIf { it >= 0 }
        val dateEpochDay = inputData.getLong(KEY_DATE_EPOCH_DAY, -1L).takeIf { it >= 0 }

        val mealTitleRu = when (mealType) {
            "BREAKFAST" -> "Завтрак"
            "LUNCH" -> "Обед"
            "DINNER" -> "Ужин"
            "SNACK" -> "Перекус"
            else -> "Приём пищи"
        }

        val timeText = if (timeMinutes != null) {
            val h = (timeMinutes / 60).coerceIn(0, 23)
            val m = (timeMinutes % 60).coerceIn(0, 59)
            "%02d:%02d".format(h, m)
        } else null

        val title = buildString {
            append("DelishHub • ")
            append(mealTitleRu)
            if (timeText != null) {
                append(" • ")
                append(timeText)
            }
        }

        val text = recipeTitle

        NotificationHelper.showMealPlanNotification(
            context = applicationContext,
            title = title,
            text = text
        )

        return Result.success(
            workDataOf(
                "shown" to true,
                "mealType" to mealType,
                "dateEpochDay" to (dateEpochDay ?: -1L),
                "uid" to user.uid
            )
        )
    }
}
