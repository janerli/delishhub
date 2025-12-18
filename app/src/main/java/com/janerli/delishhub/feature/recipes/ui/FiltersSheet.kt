package com.janerli.delishhub.feature.recipes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersSheet(
    current: RecipesFiltersUi,
    onApply: (RecipesFiltersUi) -> Unit,
    onReset: () -> Unit
) {
    val minTime = remember { mutableStateOf(current.minTime?.toString().orEmpty()) }
    val maxTime = remember { mutableStateOf(current.maxTime?.toString().orEmpty()) }
    val minDiff = remember { mutableStateOf(current.minDifficulty?.toString().orEmpty()) }
    val maxDiff = remember { mutableStateOf(current.maxDifficulty?.toString().orEmpty()) }
    val onlyFav = remember { mutableStateOf(current.onlyFavorites) }

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

        Divider()

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

        Divider()

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

        Divider()

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
                        onlyFavorites = onlyFav.value
                    )
                    onApply(applied)
                }
            ) {
                Text("Применить")
            }
        }
    }
}
