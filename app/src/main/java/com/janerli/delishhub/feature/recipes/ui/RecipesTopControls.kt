package com.janerli.delishhub.feature.recipes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RecipesTopControls(
    onOpenFilters: () -> Unit,
    onOpenSort: () -> Unit,
    selectedTagsCount: Int = 0
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AssistChip(
            onClick = onOpenFilters,
            label = {
                Text(
                    if (selectedTagsCount > 0) "Фильтры • Теги: $selectedTagsCount"
                    else "Фильтры"
                )
            },
            leadingIcon = { Icon(Icons.Filled.FilterList, contentDescription = null) }
        )
        IconButton(onClick = onOpenSort) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
        }
    }
}
