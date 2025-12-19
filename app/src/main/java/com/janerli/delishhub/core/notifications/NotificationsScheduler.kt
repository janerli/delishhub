package com.janerli.delishhub.core.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.janerli.delishhub.data.notifications.MealPlanReminderWorker
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object NotificationsScheduler {

    /**
     * Ежедневное напоминание в 09:00 (локальное время устройства).
     * WorkManager не гарантирует “в секунду”, но для учебного проекта идеально.
     */
    fun scheduleDailyMealPlanReminder(context: Context) {
        NotificationHelper.ensureChannels(context)

        val delay = initialDelayToNext(LocalTime.of(9, 0))

        val req = PeriodicWorkRequestBuilder<MealPlanReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay.toMinutes(), TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            MealPlanReminderWorker.UNIQUE_NAME_DAILY,
            ExistingPeriodicWorkPolicy.UPDATE,
            req
        )
    }

    fun enqueueTestNow(context: Context) {
        NotificationHelper.ensureChannels(context)
        NotificationHelper.showMealPlanNotification(
            context = context,
            title = "DelishHub • Тест",
            text = "Если ты видишь это — уведомления работают ✅"
        )
    }

    private fun initialDelayToNext(target: LocalTime): Duration {
        val now = LocalDateTime.now()
        var next = now.withHour(target.hour).withMinute(target.minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }
}
