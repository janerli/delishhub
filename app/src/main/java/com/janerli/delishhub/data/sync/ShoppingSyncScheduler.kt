package com.janerli.delishhub.data.sync

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ShoppingSyncScheduler {

    fun enqueueOneTime(context: Context) {
        val req = OneTimeWorkRequestBuilder<ShoppingSyncWorker>()
            .setConstraints(ShoppingSyncWorker.defaultConstraints())
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ShoppingSyncWorker.UNIQUE_NAME_ONE_TIME,
            ExistingWorkPolicy.KEEP,
            req
        )
    }

    fun schedulePeriodic(context: Context) {
        val req = PeriodicWorkRequestBuilder<ShoppingSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(ShoppingSyncWorker.defaultConstraints())
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ShoppingSyncWorker.UNIQUE_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            req
        )
    }
}
