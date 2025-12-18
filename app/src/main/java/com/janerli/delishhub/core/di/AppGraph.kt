package com.janerli.delishhub.core.di

import android.content.Context
import androidx.room.Room
import com.janerli.delishhub.data.local.AppDatabase
import com.janerli.delishhub.data.local.DbConfig
import com.janerli.delishhub.data.repository.RecipeRepositoryImpl
import com.janerli.delishhub.domain.repository.RecipeRepository

/**
 * Service Locator (без Hilt).
 * Даёт доступ к Room + репозиториям из Compose/ViewModel.
 */
object AppGraph {

    private var initialized = false
    private lateinit var appContext: Context

    fun init(context: Context) {
        if (initialized) return
        appContext = context.applicationContext
        initialized = true
    }

    private val db: AppDatabase by lazy {
        check(initialized) { "AppGraph.init(context) must be called before using repositories" }
        Room.databaseBuilder(appContext, AppDatabase::class.java, DbConfig.DB_NAME)
            // для учебного проекта можно так (если меняешь схему)
            .fallbackToDestructiveMigration()
            .build()
    }

    val recipeRepository: RecipeRepository by lazy {
        RecipeRepositoryImpl(
            recipeDao = db.recipeDao(),
            favoriteDao = db.favoriteDao(),
            mealPlanDao = db.mealPlanDao(),
            shoppingDao = db.shoppingDao()
        )
    }
}
