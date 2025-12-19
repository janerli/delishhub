package com.janerli.delishhub.feature.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.SubcomposeAsyncImage
import com.janerli.delishhub.core.di.AppGraph
import com.janerli.delishhub.core.navigation.Routes
import com.janerli.delishhub.core.session.SessionManager
import com.janerli.delishhub.core.share.ShareHelper
import com.janerli.delishhub.core.ui.MainScaffold
import com.janerli.delishhub.feature.export.RecipeExportManager

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeDetailsScreen(
    navController: NavHostController,
    recipeId: String
) {
    val context = LocalContext.current

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

    MainScaffold(
        navController = navController,
        title = "Детали рецепта",
        showBack = true,
        onBack = { navController.popBackStack() }
    ) { padding: PaddingValues ->

        if (full == null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Text("Рецепт не найден или удалён.") }
            }
            return@MainScaffold
        }

        val recipe = full.recipe
        val isOwner = !isGuest && recipe.ownerId == session.userId

        val sortedIngredients = full.ingredients.sortedBy { it.position }
        val sortedSteps = full.steps.sortedBy { it.position }
        val sortedTags = full.tags.sortedBy { it.name.lowercase() }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Фото
            if (!recipe.mainImageUrl.isNullOrBlank()) {
                item {
                    SubcomposeAsyncImage(
                        model = recipe.mainImageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                                contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator() }
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                                contentAlignment = Alignment.Center
                            ) { Text("Не удалось загрузить фото") }
                        }
                    )
                }
            }

            // Заголовок + мета
            item {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            item {
                Text(
                    text = "Время: ${recipe.cookTimeMin} мин • Сложность: ${recipe.difficulty}/5",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Теги
            if (sortedTags.isNotEmpty()) {
                item {
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sortedTags.forEach { tag ->
                            AssistChip(
                                onClick = { },
                                label = { Text(tag.name) },
                                colors = AssistChipDefaults.assistChipColors()
                            )
                        }
                    }
                }
            }

            // Кнопки (только не гость)
            if (!isGuest) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (isOwner) {
                            FilledTonalButton(
                                onClick = { navController.navigate(Routes.recipeEdit(recipeId)) }
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = null)
                                Text(" Редактировать")
                            }
                        }

                        FilledTonalButton(onClick = { vm.toggleFavorite() }) {
                            Icon(Icons.Filled.Favorite, contentDescription = null)
                            Text(if (state.isFavorite) " Убрать" else " В избранное")
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(onClick = { vm.addToShopping() }) {
                            Icon(Icons.Filled.ShoppingCart, contentDescription = null)
                            Text(" В покупки")
                        }
                    }
                }
            }

            // Экспорт PDF + Share
            item {
                FilledTonalButton(
                    onClick = {
                        val result = RecipeExportManager.exportRecipeToPdf(context, full)
                        ShareHelper.sharePdf(context, result.file, chooserTitle = "Поделиться рецептом")
                    }
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Text(" Поделиться (PDF)")
                }
            }

            // ===== Слитная карточка: Ингредиенты =====
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Ингредиенты", style = MaterialTheme.typography.titleMedium)

                        if (sortedIngredients.isEmpty()) {
                            Text(
                                text = "Нет ингредиентов",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            sortedIngredients.forEachIndexed { index, ing ->
                                val amountPart = buildString {
                                    val a = ing.amount
                                    val u = ing.unit
                                    if (a != null) append(a.toString().removeSuffix(".0"))
                                    if (!u.isNullOrBlank()) {
                                        if (isNotEmpty()) append(" ")
                                        append(u.trim())
                                    }
                                }

                                Text(
                                    text = "• ${ing.name}${if (amountPart.isNotBlank()) " — $amountPart" else ""}",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                if (index != sortedIngredients.lastIndex) {
                                    HorizontalDivider(
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ===== Слитная карточка: Шаги =====
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Шаги", style = MaterialTheme.typography.titleMedium)

                        if (sortedSteps.isEmpty()) {
                            Text(
                                text = "Нет шагов",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            sortedSteps.forEachIndexed { index, s ->
                                val idx = (s.position ?: index)

                                Text(
                                    text = "$idx. ${s.text}",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                if (index != sortedSteps.lastIndex) {
                                    HorizontalDivider(
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
