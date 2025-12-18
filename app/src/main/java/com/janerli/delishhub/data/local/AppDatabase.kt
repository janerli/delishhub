package com.janerli.delishhub.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.janerli.delishhub.data.local.dao.FavoriteDao
import com.janerli.delishhub.data.local.dao.MealPlanDao
import com.janerli.delishhub.data.local.dao.RecipeDao
import com.janerli.delishhub.data.local.dao.ShoppingDao
import com.janerli.delishhub.data.local.dao.UserDao
import com.janerli.delishhub.data.local.entity.FavoriteEntity
import com.janerli.delishhub.data.local.entity.IngredientEntity
import com.janerli.delishhub.data.local.entity.MealPlanEntryEntity
import com.janerli.delishhub.data.local.entity.RecipeEntity
import com.janerli.delishhub.data.local.entity.RecipeTagCrossRef
import com.janerli.delishhub.data.local.entity.ShoppingItemEntity
import com.janerli.delishhub.data.local.entity.StepEntity
import com.janerli.delishhub.data.local.entity.TagEntity
import com.janerli.delishhub.data.local.entity.UserLocalEntity

@Database(
    entities = [
        UserLocalEntity::class,
        RecipeEntity::class,
        IngredientEntity::class,
        StepEntity::class,
        FavoriteEntity::class,
        MealPlanEntryEntity::class,
        ShoppingItemEntity::class,
        TagEntity::class,
        RecipeTagCrossRef::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun recipeDao(): RecipeDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun mealPlanDao(): MealPlanDao
    abstract fun shoppingDao(): ShoppingDao
}
