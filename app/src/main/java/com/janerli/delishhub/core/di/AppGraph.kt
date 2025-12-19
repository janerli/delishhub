package com.janerli.delishhub.core.di

import android.content.Context
import androidx.room.Room
import com.janerli.delishhub.data.local.AppDatabase
import com.janerli.delishhub.data.local.DbConfig
import com.janerli.delishhub.data.local.DemoDataSeeder
import com.janerli.delishhub.data.local.dao.FavoriteDao
import com.janerli.delishhub.data.local.dao.MealPlanDao
import com.janerli.delishhub.data.local.dao.RecipeDao
import com.janerli.delishhub.data.local.dao.ShoppingDao
import com.janerli.delishhub.data.local.dao.TagDao
import com.janerli.delishhub.data.repository.RecipeRepositoryImpl
import com.janerli.delishhub.domain.repository.RecipeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AppGraph {

    private var initialized = false
    private lateinit var appContext: Context

    fun init(context: Context) {
        if (initialized) return
        appContext = context.applicationContext
        initialized = true

        // ✅ демо-данные: если БД пустая — добавим
        CoroutineScope(Dispatchers.Default).launch {
            DemoDataSeeder.seedIfNeeded(db)
        }
    }

    private val db: AppDatabase by lazy {
        check(initialized) { "AppGraph.init(context) must be called before using repositories" }

        Room.databaseBuilder(appContext, AppDatabase::class.java, DbConfig.DB_NAME)
            .addMigrations(AppDatabase.MIGRATION_7_8)
            .build()
    }

    // ✅ DAO доступны (нужно для простых Flow, типа счётчиков в профиле)
    val recipeDao: RecipeDao get() = db.recipeDao()
    val favoriteDao: FavoriteDao get() = db.favoriteDao()
    val mealPlanDao: MealPlanDao get() = db.mealPlanDao()

    // ✅ добавили, чтобы UI/админка могли работать напрямую при необходимости
    val tagDao: TagDao get() = db.tagDao()
    val shoppingDao: ShoppingDao get() = db.shoppingDao()

    val recipeRepository: RecipeRepository by lazy {
        RecipeRepositoryImpl(
            recipeDao = db.recipeDao(),
            favoriteDao = db.favoriteDao(),
            mealPlanDao = db.mealPlanDao(),
            shoppingDao = db.shoppingDao(),
            tagDao = db.tagDao()
        )
    }
}
