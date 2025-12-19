package com.janerli.delishhub.data.sync

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

/**
 * Единая точка запуска синхронизаций.
 *
 * Задача: синк должен стартовать не только при ручном логине, но и при
 * автологине/перезапуске приложения на другом устройстве.
 */
object SyncManager {

    /**
     * Запускает one-time синк + ставит periodic синк.
     * Безопасно вызывать сколько угодно раз: WorkManager unique work с KEEP.
     */
    fun start(context: Context) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        if (user.uid.isBlank()) return

        val appContext = context.applicationContext

        // Recipes
        RecipeSyncScheduler.enqueueOneTime(appContext)
        RecipeSyncScheduler.schedulePeriodic(appContext)

        // Favorites
        FavoriteSyncScheduler.enqueueOneTime(appContext)
        FavoriteSyncScheduler.schedulePeriodic(appContext)

        // Shopping
        ShoppingSyncScheduler.enqueueOneTime(appContext)
        ShoppingSyncScheduler.schedulePeriodic(appContext)

        // Meal plan
        MealPlanSyncScheduler.enqueueOneTime(appContext)
        MealPlanSyncScheduler.schedulePeriodic(appContext)
    }
}
