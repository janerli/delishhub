package com.janerli.delishhub.feature.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.janerli.delishhub.core.di.AppGraph
import com.janerli.delishhub.data.local.entity.RecipeEntity
import kotlinx.coroutines.launch

private enum class AdminFilter(val label: String) {
    ALL("Все"),
    PUBLIC("Публичные"),
    PRIVATE("Приватные"),
    DELETED("Удалённые")
}

private enum class AdminSort(val label: String) {
    UPDATED_DESC("Обновл. ↓"),
    UPDATED_ASC("Обновл. ↑"),
    TITLE_ASC("A→Z"),
    TITLE_DESC("Z→A")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRecipesScreen() {
    val scope = rememberCoroutineScope()

    var filter by remember { mutableStateOf(AdminFilter.ALL) }
    var sort by remember { mutableStateOf(AdminSort.UPDATED_DESC) }
    var query by remember { mutableStateOf("") }

    val recipes by AppGraph.recipeDao
        .observeAllForAdmin(
            filter = filter.name,
            query = query.takeIf { it.isNotBlank() },
            sort = sort.name
        )
        .collectAsStateWithLifecycle(initialValue = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // --- поиск ---
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            label = { Text("Поиск (title / ownerId)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        // --- фильтры ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminFilter.entries.forEach { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = { Text(f.label) }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // --- сортировка ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Сортировка:", style = MaterialTheme.typography.bodyMedium)

            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                @Suppress("DEPRECATION")
                OutlinedTextField(
                    value = sort.label,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier
                        .menuAnchor() // ✅ старый API (у тебя он есть)
                        .widthIn(min = 180.dp)
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    AdminSort.entries.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s.label) },
                            onClick = {
                                sort = s
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            AssistChip(
                onClick = {},
                label = { Text("Всего: ${recipes.size}") },
                enabled = false
            )
        }

        Spacer(Modifier.height(12.dp))

        if (recipes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Ничего не найдено")
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(recipes, key = { it.id }) { r ->
                AdminRecipeRow(
                    recipe = r,
                    onTogglePublic = { isPublic ->
                        scope.launch {
                            AppGraph.recipeDao.setPublic(r.id, isPublic, System.currentTimeMillis())

                            val base = AppGraph.recipeDao.getRecipeBaseNow(r.id) ?: return@launch
                            if (base.syncStatus != 1 && base.syncStatus != 3) {
                                AppGraph.recipeDao.upsertRecipe(
                                    base.copy(
                                        isPublic = isPublic,
                                        updatedAt = System.currentTimeMillis(),
                                        syncStatus = 2
                                    )
                                )
                            }
                        }
                    },
                    onDelete = {
                        scope.launch {
                            AppGraph.recipeDao.softDeleteRecipe(r.id, System.currentTimeMillis())
                        }
                    },
                    onRestore = {
                        scope.launch {
                            AppGraph.recipeDao.restoreRecipe(r.id, System.currentTimeMillis())
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AdminRecipeRow(
    recipe: RecipeEntity,
    onTogglePublic: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit
) {
    val isDeleted = recipe.syncStatus == 3

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            when {
                                isDeleted -> "DELETED"
                                recipe.isPublic -> "PUBLIC"
                                else -> "PRIVATE"
                            }
                        )
                    },
                    enabled = false
                )
            }

            Text(
                text = "ownerId: ${recipe.ownerId}",
                style = MaterialTheme.typography.bodySmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Public", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = recipe.isPublic,
                        onCheckedChange = { onTogglePublic(it) },
                        enabled = !isDeleted
                    )
                }

                if (isDeleted) {
                    IconButton(onClick = onRestore) {
                        Icon(Icons.Filled.Restore, contentDescription = "Restore")
                    }
                } else {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }
}
