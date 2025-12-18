package com.janerli.delishhub.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.janerli.delishhub.core.di.AppGraph
import com.janerli.delishhub.core.session.SessionManager
import com.janerli.delishhub.core.ui.MainScaffold
import com.janerli.delishhub.feature.recipes.RecipesViewModel
import com.janerli.delishhub.feature.recipes.RecipesViewModelFactory
import com.janerli.delishhub.feature.recipes.ui.RecipeCard
import com.janerli.delishhub.feature.recipes.ui.RecipeCardUi

@Composable
fun HomeScreen(navController: NavHostController) {

    val session by SessionManager.session.collectAsStateWithLifecycle()
    val isGuest = session.isGuest

    var search by remember { mutableStateOf("") }

    val recipesVm: RecipesViewModel = viewModel(
        factory = RecipesViewModelFactory(
            repository = AppGraph.recipeRepository,
            isMyMode = false
        )
    )
    val all: List<RecipeCardUi> by recipesVm.cards.collectAsStateWithLifecycle()

    val recipeOfDay = all.firstOrNull()
    val popular = all.take(10)
    val recent = all.takeLast(5)

    val filteredFeed = remember(all, search) {
        val q = search.trim()
        if (q.isEmpty()) all
        else all.filter { it.title.contains(q, ignoreCase = true) }
    }

    val quickPicks = remember {
        listOf("Быстро до 15 мин", "Без мяса", "Завтраки", "Десерты", "Супы", "Салаты")
    }

    MainScaffold(
        navController = navController,
        title = "DelishHub",
        showBack = false
    ) { padding: PaddingValues ->

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {

                // --- Поиск ---
                item(key = "search") {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Поиск по рецептам") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true
                    )
                }

                // --- Популярное ---
                item(key = "popular_header") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocalFireDepartment, contentDescription = null)
                                Text(" Популярное", style = MaterialTheme.typography.titleMedium)
                            }
                            Text(
                                if (popular.isEmpty())
                                    "Пока нет рецептов. Создай первый рецепт 🙂"
                                else
                                    "Топ рецепты из твоей базы."
                            )
                        }
                    }
                }

                // ✅ section-aware keys: popular-
                if (popular.isNotEmpty()) {
                    items(
                        items = popular,
                        key = { "popular-${it.id}" }
                    ) { item ->
                        RecipeCard(
                            item = item,
                            onOpen = { id -> navController.navigate("recipe_details/$id") },
                            onToggleFavorite = if (isGuest) null else ({ id -> recipesVm.toggleFavorite(id) })
                        )
                    }
                }

                // --- Подборки ---
                item(key = "picks_header") { SectionHeader(title = "Подборки") }
                item(key = "picks_row") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(quickPicks) { tag ->
                            Card(elevation = CardDefaults.cardElevation(1.dp)) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(tag)
                                }
                            }
                        }
                    }
                }

                // --- Рецепт дня ---
                item(key = "day_header") { SectionHeader(title = "Рецепт дня") }
                item(key = "day_card") {
                    recipeOfDay?.let { item ->
                        RecipeCard(
                            item = item,
                            onOpen = { id -> navController.navigate("recipe_details/$id") },
                            onToggleFavorite = if (isGuest) null else ({ id -> recipesVm.toggleFavorite(id) })
                        )
                    } ?: Text("Пока пусто")
                }

                // --- Недавние ---
                item(key = "recent_header") { SectionHeader(title = "Недавние") }

                // ✅ section-aware keys: recent-
                if (recent.isNotEmpty()) {
                    items(
                        items = recent,
                        key = { "recent-${it.id}" }
                    ) { item ->
                        RecipeCard(
                            item = item,
                            onOpen = { id -> navController.navigate("recipe_details/$id") },
                            onToggleFavorite = if (isGuest) null else ({ id -> recipesVm.toggleFavorite(id) })
                        )
                    }
                } else {
                    item(key = "recent_empty") { Text("Нет недавних рецептов.") }
                }

                // --- Лента ---
                item(key = "feed_header") { SectionHeader(title = "Лента") }

                // ✅ section-aware keys: feed-
                if (filteredFeed.isNotEmpty()) {
                    items(
                        items = filteredFeed,
                        key = { "feed-${it.id}" }
                    ) { item ->
                        RecipeCard(
                            item = item,
                            onOpen = { id -> navController.navigate("recipe_details/$id") },
                            onToggleFavorite = if (isGuest) null else ({ id -> recipesVm.toggleFavorite(id) })
                        )
                    }
                } else {
                    item(key = "feed_empty") {
                        Text(
                            if (search.isBlank()) "Лента пока пустая."
                            else "Ничего не найдено по запросу."
                        )
                    }
                }

                // --- Совет ---
                item(key = "tip") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.TipsAndUpdates, contentDescription = null)
                                Text(" Совет дня", style = MaterialTheme.typography.titleMedium)
                            }
                            Text("Добавляй рецепт в план — и список покупок соберётся автоматически.")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}
