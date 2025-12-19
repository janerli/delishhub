package com.janerli.delishhub.data.local

import android.util.Log
import com.janerli.delishhub.data.local.entity.IngredientEntity
import com.janerli.delishhub.data.local.entity.RecipeEntity
import com.janerli.delishhub.data.local.entity.StepEntity
import com.janerli.delishhub.data.local.entity.TagEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID

object DemoDataSeeder {

    private const val TAG = "DemoDataSeeder"

    suspend fun seedIfNeeded(db: AppDatabase) = withContext(Dispatchers.IO) {

        // ✅ Если рецепты уже есть — ничего не делаем
        val existing = db.recipeDao().observeAllBase().first()
        if (existing.isNotEmpty()) {
            Log.d(TAG, "DB already filled, skip seeding")
            return@withContext
        }

        Log.d(TAG, "Seeding demo data…")

        val now = System.currentTimeMillis()

        // ---------- ТЕГИ ----------
        val tags = listOf(
            TagEntity(id = "tag_breakfast", name = "Завтрак"),
            TagEntity(id = "tag_lunch", name = "Обед"),
            TagEntity(id = "tag_dinner", name = "Ужин"),
            TagEntity(id = "tag_snack", name = "Перекус"),
            TagEntity(id = "tag_quick", name = "Быстро"),
            TagEntity(id = "tag_healthy", name = "Полезно"),
            TagEntity(id = "tag_budget", name = "Бюджетно"),
            TagEntity(id = "tag_veggie", name = "Без мяса"),
            TagEntity(id = "tag_sweet", name = "Сладкое"),
            TagEntity(id = "tag_baking", name = "Выпечка")
        )
        tags.forEach { db.tagDao().insertIgnore(it) }

        // ---------- HELPERS ----------
        suspend fun addRecipe(
            title: String,
            description: String,
            cookTimeMin: Int,
            difficulty: Int,
            ratingAvg: Double,
            ratingsCount: Int,
            tagIds: List<String>,
            ingredients: List<Pair<String, Pair<Double?, String?>>>, // name -> (amount, unit)
            steps: List<String>,
            updatedShiftMinutes: Long
        ) {
            val recipeId = UUID.randomUUID().toString()
            val createdAt = now - 5L * 24 * 60 * 60 * 1000 // -5 days
            val updatedAt = now - updatedShiftMinutes * 60_000

            val recipe = RecipeEntity(
                id = recipeId,
                ownerId = "demo",
                title = title,
                description = description,
                categoryId = null,
                difficulty = difficulty,
                cookTimeMin = cookTimeMin,
                ratingAvg = ratingAvg,
                ratingsCount = ratingsCount,
                mainImageUrl = null,
                createdAt = createdAt,
                updatedAt = updatedAt,
                syncStatus = 0
            )

            val ingredientEntities = ingredients.mapIndexed { idx, (name, amtUnit) ->
                IngredientEntity(
                    id = UUID.randomUUID().toString(),
                    recipeId = recipeId,
                    name = name,
                    amount = amtUnit.first,
                    unit = amtUnit.second,
                    position = idx + 1
                )
            }

            val stepEntities = steps.mapIndexed { idx, text ->
                StepEntity(
                    id = UUID.randomUUID().toString(),
                    recipeId = recipeId,
                    text = text,
                    photoUrl = null,
                    position = idx + 1
                )
            }

            db.recipeDao().upsertRecipeFull(
                recipe = recipe,
                ingredients = ingredientEntities,
                steps = stepEntities,
                tagIds = tagIds
            )
        }

        // ---------- РЕЦЕПТЫ (10 шт) ----------

        // 1
        addRecipe(
            title = "Омлет с овощами",
            description = "Быстрый и простой завтрак на каждый день.",
            cookTimeMin = 10,
            difficulty = 1,
            ratingAvg = 4.5,
            ratingsCount = 12,
            tagIds = listOf("tag_breakfast", "tag_quick", "tag_healthy", "tag_veggie"),
            ingredients = listOf(
                "Яйца" to (2.0 to "шт"),
                "Помидор" to (1.0 to "шт"),
                "Соль" to (null to "по вкусу"),
                "Масло" to (1.0 to "ч.л.")
            ),
            steps = listOf(
                "Взбей яйца с солью.",
                "Нарежь помидор кубиками.",
                "Разогрей сковороду, добавь масло.",
                "Вылей яйца, добавь помидоры, готовь 5–7 минут."
            ),
            updatedShiftMinutes = 30
        )

        // 2
        addRecipe(
            title = "Овсянка с бананом",
            description = "Сытный завтрак без заморочек.",
            cookTimeMin = 8,
            difficulty = 1,
            ratingAvg = 4.6,
            ratingsCount = 25,
            tagIds = listOf("tag_breakfast", "tag_quick", "tag_healthy", "tag_sweet", "tag_budget", "tag_veggie"),
            ingredients = listOf(
                "Овсяные хлопья" to (60.0 to "г"),
                "Молоко/вода" to (200.0 to "мл"),
                "Банан" to (1.0 to "шт"),
                "Мёд" to (1.0 to "ч.л.")
            ),
            steps = listOf(
                "Залей овсянку молоком/водой.",
                "Вари 5–6 минут, помешивая.",
                "Добавь банан и мёд, перемешай."
            ),
            updatedShiftMinutes = 70
        )

        // 3
        addRecipe(
            title = "Салат с тунцом",
            description = "Лёгкий обед: быстро и вкусно.",
            cookTimeMin = 12,
            difficulty = 2,
            ratingAvg = 4.4,
            ratingsCount = 18,
            tagIds = listOf("tag_lunch", "tag_quick", "tag_healthy"),
            ingredients = listOf(
                "Тунец консервированный" to (1.0 to "банка"),
                "Огурец" to (1.0 to "шт"),
                "Листья салата" to (80.0 to "г"),
                "Оливковое масло" to (1.0 to "ст.л."),
                "Соль" to (null to "по вкусу")
            ),
            steps = listOf(
                "Нарежь огурец.",
                "Смешай салат, огурец и тунец.",
                "Заправь маслом и посоли."
            ),
            updatedShiftMinutes = 120
        )

        // 4
        addRecipe(
            title = "Паста с томатным соусом",
            description = "Классика на ужин: просто и стабильно вкусно.",
            cookTimeMin = 20,
            difficulty = 2,
            ratingAvg = 4.7,
            ratingsCount = 40,
            tagIds = listOf("tag_dinner", "tag_budget", "tag_veggie"),
            ingredients = listOf(
                "Паста" to (200.0 to "г"),
                "Томатная паста" to (2.0 to "ст.л."),
                "Чеснок" to (1.0 to "зубчик"),
                "Оливковое масло" to (1.0 to "ст.л."),
                "Соль" to (null to "по вкусу")
            ),
            steps = listOf(
                "Отвари пасту до al dente.",
                "Обжарь чеснок на масле 30 секунд.",
                "Добавь томатную пасту и немного воды от пасты.",
                "Смешай пасту с соусом, посоли."
            ),
            updatedShiftMinutes = 180
        )

        // 5
        addRecipe(
            title = "Курица с рисом (сковорода)",
            description = "Быстрый обед без духовки.",
            cookTimeMin = 30,
            difficulty = 3,
            ratingAvg = 4.3,
            ratingsCount = 15,
            tagIds = listOf("tag_lunch", "tag_dinner", "tag_budget"),
            ingredients = listOf(
                "Куриное филе" to (250.0 to "г"),
                "Рис" to (150.0 to "г"),
                "Лук" to (1.0 to "шт"),
                "Морковь" to (1.0 to "шт"),
                "Соль" to (null to "по вкусу")
            ),
            steps = listOf(
                "Нарежь филе, обжарь 5 минут.",
                "Добавь лук и морковь, обжарь ещё 3–4 минуты.",
                "Добавь рис и воду (примерно 1:2), посоли.",
                "Томи под крышкой 15–18 минут."
            ),
            updatedShiftMinutes = 240
        )

        // 6
        addRecipe(
            title = "Тосты с авокадо",
            description = "Быстрый перекус или завтрак.",
            cookTimeMin = 7,
            difficulty = 1,
            ratingAvg = 4.2,
            ratingsCount = 10,
            tagIds = listOf("tag_breakfast", "tag_snack", "tag_quick", "tag_healthy", "tag_veggie"),
            ingredients = listOf(
                "Хлеб" to (2.0 to "ломтика"),
                "Авокадо" to (1.0 to "шт"),
                "Соль" to (null to "по вкусу"),
                "Лимонный сок" to (1.0 to "ч.л.")
            ),
            steps = listOf(
                "Подсуши хлеб в тостере/на сковороде.",
                "Разомни авокадо с солью и лимонным соком.",
                "Намажь на тосты."
            ),
            updatedShiftMinutes = 15
        )

        // 7
        addRecipe(
            title = "Суп-пюре из тыквы",
            description = "Тёплый и полезный ужин.",
            cookTimeMin = 35,
            difficulty = 3,
            ratingAvg = 4.6,
            ratingsCount = 22,
            tagIds = listOf("tag_dinner", "tag_healthy", "tag_veggie"),
            ingredients = listOf(
                "Тыква" to (400.0 to "г"),
                "Лук" to (1.0 to "шт"),
                "Сливки" to (100.0 to "мл"),
                "Соль" to (null to "по вкусу")
            ),
            steps = listOf(
                "Нарежь тыкву и лук.",
                "Отвари 20 минут до мягкости.",
                "Пробей блендером, добавь сливки и соль.",
                "Прогрей ещё 2–3 минуты."
            ),
            updatedShiftMinutes = 300
        )

        // 8
        addRecipe(
            title = "Блины на молоке",
            description = "Выпечка без заморочек (идеально для демо).",
            cookTimeMin = 25,
            difficulty = 3,
            ratingAvg = 4.8,
            ratingsCount = 55,
            tagIds = listOf("tag_breakfast", "tag_sweet", "tag_baking", "tag_budget"),
            ingredients = listOf(
                "Молоко" to (500.0 to "мл"),
                "Мука" to (180.0 to "г"),
                "Яйцо" to (1.0 to "шт"),
                "Сахар" to (1.0 to "ст.л."),
                "Соль" to (null to "щепотка")
            ),
            steps = listOf(
                "Смешай молоко, яйцо, сахар и соль.",
                "Постепенно вмешай муку до однородности.",
                "Жарь блины на разогретой сковороде."
            ),
            updatedShiftMinutes = 360
        )

        // 9
        addRecipe(
            title = "Греческий салат",
            description = "Классика: свежо и быстро.",
            cookTimeMin = 12,
            difficulty = 1,
            ratingAvg = 4.5,
            ratingsCount = 33,
            tagIds = listOf("tag_lunch", "tag_quick", "tag_healthy", "tag_veggie"),
            ingredients = listOf(
                "Огурец" to (1.0 to "шт"),
                "Помидор" to (2.0 to "шт"),
                "Сыр фета" to (80.0 to "г"),
                "Оливковое масло" to (1.0 to "ст.л."),
                "Соль" to (null to "по вкусу")
            ),
            steps = listOf(
                "Нарежь овощи.",
                "Добавь фету.",
                "Заправь маслом и посоли."
            ),
            updatedShiftMinutes = 90
        )

        // 10
        addRecipe(
            title = "Йогурт с гранолой",
            description = "Перекус за 2 минуты (очень удобно для демонстрации).",
            cookTimeMin = 2,
            difficulty = 1,
            ratingAvg = 4.4,
            ratingsCount = 19,
            tagIds = listOf("tag_snack", "tag_quick", "tag_healthy", "tag_sweet"),
            ingredients = listOf(
                "Йогурт" to (200.0 to "г"),
                "Гранола" to (40.0 to "г"),
                "Ягоды" to (80.0 to "г")
            ),
            steps = listOf(
                "Выложи йогурт в миску.",
                "Добавь гранолу и ягоды."
            ),
            updatedShiftMinutes = 5
        )

        Log.d(TAG, "Demo data seeded successfully (10 recipes)")
    }
}
