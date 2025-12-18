package com.janerli.delishhub.feature.planner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.janerli.delishhub.feature.recipes.ui.RecipeCard
import com.janerli.delishhub.feature.recipes.ui.RecipeCardUi

@Composable
fun ChooseRecipeSheet(
    allRecipes: List<RecipeCardUi>,
    onPick: (RecipeCardUi) -> Unit,
    onClose: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    val filtered = remember(allRecipes, query) {
        val q = query.trim()
        if (q.isEmpty()) allRecipes
        else allRecipes.filter { it.title.contains(q, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Выбрать рецепт", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Поиск") },
            singleLine = true
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(
                items = filtered,
                key = { it.id }
            ) { item ->
                // onToggleFavorite = null (в планере не нужно)
                RecipeCard(
                    item = item,
                    onOpen = { /* здесь не открываем details */ },
                    onToggleFavorite = null
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onPick(item) }
                ) {
                    Text("Выбрать")
                }
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClose
        ) {
            Text("Закрыть")
        }
    }
}
