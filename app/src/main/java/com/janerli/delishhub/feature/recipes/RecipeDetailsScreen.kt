package com.janerli.delishhub.feature.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.janerli.delishhub.core.di.AppGraph
import com.janerli.delishhub.core.navigation.Routes
import com.janerli.delishhub.core.session.SessionManager
import com.janerli.delishhub.core.ui.MainScaffold
import com.janerli.delishhub.data.local.entity.IngredientEntity
import com.janerli.delishhub.data.local.entity.StepEntity

@Composable
fun RecipeDetailsScreen(
    navController: NavHostController,
    recipeId: String
) {
    val vm: RecipeDetailsViewModel = viewModel(
        factory = RecipeDetailsViewModelFactory(
            repository = AppGraph.recipeRepository,
            recipeId = recipeId
        )
    )

    val state by vm.state.collectAsStateWithLifecycle()
    val session by SessionManager.session.collectAsStateWithLifecycle()
    val isGuest = session.isGuest

    val full = state.recipe
    val recipe = full?.recipe

    MainScaffold(
        navController = navController,
        title = "Детали рецепта",
        showBack = true,
        onBack = { navController.popBackStack() }
    ) { padding: PaddingValues ->

        if (recipe == null || full == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Рецепт не найден или удалён.")
            }
            return@MainScaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(recipe.title)
            Text("Время: ${recipe.cookTimeMin} мин • Сложность: ${recipe.difficulty}/5")

            // ✅ действия скрыты для гостя
            if (!isGuest) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(onClick = { navController.navigate(Routes.recipeEdit(recipeId)) }) {
                        Icon(Icons.Filled.Edit, contentDescription = null)
                        Text(" Редактировать")
                    }
                    FilledTonalButton(onClick = { vm.toggleFavorite() }) {
                        Icon(Icons.Filled.Favorite, contentDescription = null)
                        Text(if (state.isFavorite) " Убрать" else " В избранное")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(onClick = { /* выбор рецепта в план — уже сделан в Planner */ }) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null)
                        Text(" В план")
                    }

                    // ✅ Шаг 5.6: добавляем ингредиенты рецепта в покупки
                    FilledTonalButton(onClick = { vm.addToShopping() }) {
                        Icon(Icons.Filled.ShoppingCart, contentDescription = null)
                        Text(" В покупки")
                    }
                }
            }

            FilledTonalButton(onClick = { /* позже: share/export */ }) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Text(" Поделиться")
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Ингредиенты")
                    full.ingredients
                        .sortedBy { it.position }
                        .forEach { ing: IngredientEntity ->
                            val amountPart = buildString {
                                val a = ing.amount
                                val u = ing.unit
                                if (a != null) append(a.toString().removeSuffix(".0"))
                                if (!u.isNullOrBlank()) {
                                    if (isNotEmpty()) append(" ")
                                    append(u.trim())
                                }
                            }
                            Text("• ${ing.name}${if (amountPart.isNotBlank()) " — $amountPart" else ""}")
                        }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Шаги")
                    full.steps
                        .sortedBy { it.position }
                        .forEachIndexed { idx: Int, s: StepEntity ->
                            Text("${idx + 1}. ${s.text}")
                        }
                }
            }
        }
    }
}
