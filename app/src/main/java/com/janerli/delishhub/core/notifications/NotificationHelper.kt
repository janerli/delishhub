package com.janerli.delishhub.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.janerli.delishhub.R

object NotificationHelper {

    const val CHANNEL_ID_MEALPLAN = "mealplan_reminders"
    private const val CHANNEL_NAME_MEALPLAN = "Напоминания о плане питания"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID_MEALPLAN,
            CHANNEL_NAME_MEALPLAN,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Напоминания о том, что готовим сегодня"
        }
        nm.createNotificationChannel(channel)
    }

    private fun hasPostNotificationsPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun showMealPlanNotification(
        context: Context,
        title: String,
        text: String,
        notificationId: Int = 2001
    ) {
        if (!hasPostNotificationsPermission(context)) return

        ensureChannels(context)

        val launchIntent: Intent? =
            context.packageManager.getLaunchIntentForPackage(context.packageName)

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val n = NotificationCompat.Builder(context, CHANNEL_ID_MEALPLAN)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, n)
        } catch (_: SecurityException) {
            // Пользователь мог отклонить/отозвать разрешение, либо OEM-особенность.
            // Тихо игнорируем: уведомление просто не покажется.
        }
    }
}
