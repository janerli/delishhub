package com.janerli.delishhub.data.sync

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object FavoriteSyncScheduler {

    fun enqueueOneTime(context: Context) {
        val req = OneTimeWorkRequestBuilder<FavoriteSyncWorker>()
            .setConstraints(FavoriteSyncWorker.defaultConstraints())
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            FavoriteSyncWorker.UNIQUE_NAME_ONE_TIME,
            ExistingWorkPolicy.KEEP,
            req
        )
    }

    fun schedulePeriodic(context: Context) {
        val req = PeriodicWorkRequestBuilder<FavoriteSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(FavoriteSyncWorker.defaultConstraints())
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            FavoriteSyncWorker.UNIQUE_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            req
        )
    }
}
