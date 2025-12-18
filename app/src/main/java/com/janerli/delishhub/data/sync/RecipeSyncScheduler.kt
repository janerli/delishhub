package com.janerli.delishhub.data.sync

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object RecipeSyncScheduler {

    fun enqueueOneTime(context: Context) {
        val req = OneTimeWorkRequestBuilder<RecipeSyncWorker>()
            .setConstraints(RecipeSyncWorker.defaultConstraints())
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            RecipeSyncWorker.UNIQUE_NAME_ONE_TIME,
            ExistingWorkPolicy.KEEP,
            req
        )
    }

    fun schedulePeriodic(context: Context) {
        val req = PeriodicWorkRequestBuilder<RecipeSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(RecipeSyncWorker.defaultConstraints())
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            RecipeSyncWorker.UNIQUE_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            req
        )
    }
}
