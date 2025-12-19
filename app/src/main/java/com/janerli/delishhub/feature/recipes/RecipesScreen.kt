package com.janerli.delishhub.feature.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.janerli.delishhub.core.di.AppGraph
import com.janerli.delishhub.core.navigation.Routes
import com.janerli.delishhub.core.session.SessionManager
import com.janerli.delishhub.core.ui.MainScaffold
import com.janerli.delishhub.feature.recipes.ui.FiltersSheet
import com.janerli.delishhub.feature.recipes.ui.RecipeCard
import com.janerli.delishhub.feature.recipes.ui.RecipesFiltersUi
import com.janerli.delishhub.feature.recipes.ui.RecipesSortUi
import com.janerli.delishhub.feature.recipes.ui.RecipesTopControls

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen(
    navController: NavHostController,
    isMyMode: Boolean = false
) {
    val vm: RecipesViewModel = viewModel(
        factory = RecipesViewModelFactory(
            repository = AppGraph.recipeRepository,
            isMyMode = isMyMode
        )
    )

    val session by SessionManager.session.collectAsStateWithLifecycle()
    val isGuest = session.isGuest

    val allTags by vm.allTags.collectAsStateWithLifecycle()
    val tagMatchedIds by vm.tagMatchedRecipeIds.collectAsStateWithLifecycle()

    var query by rememberSaveable { mutableStateOf("") }
    var filters by remember { mutableStateOf(RecipesFiltersUi()) }
    var sort by remember { mutableStateOf(RecipesSortUi.TITLE_ASC) }

    var isFiltersOpen by remember { mutableStateOf(false) }
    var isSortMenuOpen by remember { mutableStateOf(false) }

    val items by vm.cards.collectAsStateWithLifecycle()

    val filtered = items
        .filter { it.title.contains(query, ignoreCase = true) }
        .filter { if (filters.onlyFavorites) it.isFavorite else true }
        .filter {
            val minT = filters.minTime
            val maxT = filters.maxTime
            (minT == null || it.cookTimeMin >= minT) &&
                    (maxT == null || it.cookTimeMin <= maxT)
        }
        .filter {
            val minD = filters.minDifficulty
            val maxD = filters.maxDifficulty
            (minD == null || it.difficulty >= minD) &&
                    (maxD == null || it.difficulty <= maxD)
        }
        // ✅ ТЕГИ: если выбраны — оставляем только те, чьи id в tagMatchedIds
        .filter {
            if (filters.tagIds.isEmpty()) true
            else tagMatchedIds.contains(it.id)
        }

    val shown = when (sort) {
        RecipesSortUi.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
        RecipesSortUi.TIME_ASC -> filtered.sortedBy { it.cookTimeMin }
        RecipesSortUi.DIFFICULTY_ASC -> filtered.sortedBy { it.difficulty }
    }

    MainScaffold(
        navController = navController,
        title = if (isMyMode) "Мои рецепты" else "Рецепты",
        showBack = isMyMode,
        onBack = { navController.popBackStack() },
        fab = {
            if (!isGuest) {
                FloatingActionButton(onClick = { navController.navigate(Routes.RECIPE_CREATE) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Добавить рецепт")
                }
            }
        }
    ) { padding: PaddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Поиск рецептов") },
                singleLine = true
            )

            RecipesTopControls(
                onOpenFilters = { isFiltersOpen = true },
                onOpenSort = { isSortMenuOpen = true },
                selectedTagsCount = filters.tagIds.size
            )

            if (shown.isEmpty()) {
                EmptyState(
                    title = "Ничего не найдено",
                    subtitle = "Попробуй изменить поиск или фильтры."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(shown, key = { it.id }) { item ->
                        RecipeCard(
                            item = item,
                            onOpen = { id -> navController.navigate(Routes.recipeDetails(id)) },
                            onToggleFavorite = if (isGuest) null else { id -> vm.toggleFavorite(id) }
                        )
                    }
                }
            }
        }

        if (isFiltersOpen) {
            ModalBottomSheet(onDismissRequest = { isFiltersOpen = false }) {
                FiltersSheet(
                    current = filters,
                    allTags = allTags,
                    onApply = {
                        filters = it
                        vm.setSelectedTagIds(it.tagIds) // ✅ обновляем VM, чтобы пошёл Flow ids
                        isFiltersOpen = false
                    },
                    onReset = {
                        val cleared = RecipesFiltersUi()
                        filters = cleared
                        vm.setSelectedTagIds(emptySet())
                        isFiltersOpen = false
                    }
                )
            }
        }

        DropdownMenu(
            expanded = isSortMenuOpen,
            onDismissRequest = { isSortMenuOpen = false }
        ) {
            DropdownMenuItem(
                text = { Text("Сортировка: Название (A–Z)") },
                onClick = { sort = RecipesSortUi.TITLE_ASC; isSortMenuOpen = false }
            )
            DropdownMenuItem(
                text = { Text("Сортировка: Время") },
                onClick = { sort = RecipesSortUi.TIME_ASC; isSortMenuOpen = false }
            )
            DropdownMenuItem(
                text = { Text("Сортировка: Сложность") },
                onClick = { sort = RecipesSortUi.DIFFICULTY_ASC; isSortMenuOpen = false }
            )
        }
    }
}

@Composable
private fun EmptyState(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium)
    }
}
