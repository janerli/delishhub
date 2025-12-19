package com.janerli.delishhub.feature.planner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.janerli.delishhub.feature.recipes.ui.RecipeCardUi

private enum class QuickTimeFilter(val label: String, val maxMinutes: Int?) {
    ALL("Все", null),
    LE_15("≤ 15 мин", 15),
    LE_30("≤ 30 мин", 30)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseRecipeSheet(
    allRecipes: List<RecipeCardUi>,
    onPick: (RecipeCardUi) -> Unit,
    onClose: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var timeFilter by remember { mutableStateOf(QuickTimeFilter.ALL) }

    val filtered = remember(allRecipes, query, timeFilter) {
        val q = query.trim()

        allRecipes
            .asSequence()
            .filter { item ->
                // Поиск по названию
                q.isEmpty() || item.title.contains(q, ignoreCase = true)
            }
            .filter { item ->
                // Быстрый фильтр по времени приготовления
                val max = timeFilter.maxMinutes
                if (max == null) true
                else {
                    val t = item.cookTimeMin
                    // если у рецепта 0/не задано — не показываем в ограниченных фильтрах
                    t > 0 && t <= max
                }
            }
            .toList()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Выбрать рецепт",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Закрыть")
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Поиск") },
            singleLine = true
        )

        // Быстрые фильтры
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 8.dp)
        ) {
            items(QuickTimeFilter.entries) { f ->
                FilterChip(
                    selected = (timeFilter == f),
                    onClick = { timeFilter = f },
                    label = { Text(f.label) }
                )
            }
        }

        if (filtered.isEmpty()) {
            val hint = when (timeFilter) {
                QuickTimeFilter.ALL -> "Ничего не найдено"
                QuickTimeFilter.LE_15 -> "Нет рецептов ≤ 15 мин"
                QuickTimeFilter.LE_30 -> "Нет рецептов ≤ 30 мин"
            }
            Text(hint, style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(
                    items = filtered,
                    key = { it.id }
                ) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(item) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                val subtitle = buildString {
                                    if (item.cookTimeMin > 0) {
                                        append("${item.cookTimeMin} мин")
                                    }
                                    if (item.difficulty != null) {
                                        if (isNotEmpty()) append(" • ")
                                        append("сложн. ${item.difficulty}")
                                    }
                                }

                                if (subtitle.isNotBlank()) {
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(Modifier.width(10.dp))

                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Выбрать"
                            )
                        }
                    }
                }
            }
        }
    }
}
