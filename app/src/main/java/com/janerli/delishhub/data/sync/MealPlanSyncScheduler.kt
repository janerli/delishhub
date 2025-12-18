package com.janerli.delishhub.data.sync

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object MealPlanSyncScheduler {

    fun enqueueOneTime(context: Context) {
        val req = OneTimeWorkRequestBuilder<MealPlanSyncWorker>()
            .setConstraints(MealPlanSyncWorker.defaultConstraints())
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            MealPlanSyncWorker.UNIQUE_NAME_ONE_TIME,
            ExistingWorkPolicy.KEEP,
            req
        )
    }

    fun schedulePeriodic(context: Context) {
        val req = PeriodicWorkRequestBuilder<MealPlanSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(MealPlanSyncWorker.defaultConstraints())
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            MealPlanSyncWorker.UNIQUE_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            req
        )
    }
}
