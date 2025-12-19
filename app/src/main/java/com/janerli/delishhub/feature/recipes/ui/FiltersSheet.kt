package com.janerli.delishhub.feature.recipes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.janerli.delishhub.data.local.entity.TagEntity

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FiltersSheet(
    current: RecipesFiltersUi,
    allTags: List<TagEntity>,
    onApply: (RecipesFiltersUi) -> Unit,
    onReset: () -> Unit
) {
    val minTime = remember { mutableStateOf(current.minTime?.toString().orEmpty()) }
    val maxTime = remember { mutableStateOf(current.maxTime?.toString().orEmpty()) }
    val minDiff = remember { mutableStateOf(current.minDifficulty?.toString().orEmpty()) }
    val maxDiff = remember { mutableStateOf(current.maxDifficulty?.toString().orEmpty()) }
    val onlyFav = remember { mutableStateOf(current.onlyFavorites) }

    // ✅ теги (ВАЖНО: дальше работаем только через КОПИИ, без мутаций того же объекта)
    val selectedTagIds = remember { mutableStateOf(current.tagIds.toSet()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Фильтры")

        OutlinedTextField(
            value = minTime.value,
            onValueChange = { minTime.value = it.filter(Char::isDigit).take(3) },
            label = { Text("Мин. время (мин)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = maxTime.value,
            onValueChange = { maxTime.value = it.filter(Char::isDigit).take(3) },
            label = { Text("Макс. время (мин)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider()

        OutlinedTextField(
            value = minDiff.value,
            onValueChange = { minDiff.value = it.filter(Char::isDigit).take(1) },
            label = { Text("Мин. сложность (1–5)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = maxDiff.value,
            onValueChange = { maxDiff.value = it.filter(Char::isDigit).take(1) },
            label = { Text("Макс. сложность (1–5)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Только избранное")
            Switch(
                checked = onlyFav.value,
                onCheckedChange = { onlyFav.value = it }
            )
        }

        HorizontalDivider()

        Text("Теги (можно несколько)")

        val sorted = allTags.sortedBy { it.name.lowercase() }
        if (sorted.isEmpty()) {
            Text("Тегов пока нет")
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sorted.forEach { tag ->
                    val selected = selectedTagIds.value.contains(tag.id)

                    FilterChip(
                        selected = selected,
                        onClick = {
                            // ✅ создаём новый Set -> Compose гарантированно увидит изменение
                            selectedTagIds.value =
                                if (selected) selectedTagIds.value - tag.id
                                else selectedTagIds.value + tag.id
                        },
                        label = { Text(tag.name) }
                    )
                }
            }
        }

        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = onReset
            ) {
                Text("Сброс")
            }

            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    fun toIntOrNull(s: String): Int? = s.toIntOrNull()

                    val applied = RecipesFiltersUi(
                        minTime = toIntOrNull(minTime.value),
                        maxTime = toIntOrNull(maxTime.value),
                        minDifficulty = toIntOrNull(minDiff.value),
                        maxDifficulty = toIntOrNull(maxDiff.value),
                        onlyFavorites = onlyFav.value,
                        tagIds = selectedTagIds.value
                    )
                    onApply(applied)
                }
            ) {
                Text("Применить")
            }
        }
    }
}
