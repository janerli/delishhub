package com.janerli.delishhub.feature.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.janerli.delishhub.core.di.AppGraph
import com.janerli.delishhub.feature.recipes.ui.IngredientUi
import com.janerli.delishhub.feature.recipes.ui.StepUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeCreateScreen(onBack: () -> Unit) {

    val vm: RecipeEditorViewModel = viewModel(
        factory = RecipeEditorViewModelFactory(
            repository = AppGraph.recipeRepository,
            recipeId = null
        )
    )
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) {
            vm.consumeSaved()
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Создать рецепт") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        }
    ) { padding: PaddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {

            // --- Фото (заглушки) ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Фото рецепта")
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilledTonalButton(onClick = { /* Шаг 6 */ }) {
                                Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                                Text(" Галерея")
                            }
                            FilledTonalButton(onClick = { /* Шаг 6 */ }) {
                                Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                                Text(" Камера")
                            }
                        }
                        Text("Подключим загрузку фото через Storage на шаге 6.")
                    }
                }
            }

            // --- Основные поля ---
            item {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = vm::setTitle,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Название*") },
                    isError = state.titleError != null,
                    singleLine = true
                )
                if (state.titleError != null) {
                    Text(
                        text = state.titleError!!,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = state.description,
                    onValueChange = vm::setDescription,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Описание") },
                    minLines = 3
                )
            }

            item {
                OutlinedTextField(
                    value = state.cookTime,
                    onValueChange = vm::setCookTimeDigitsOnly,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Время приготовления (мин)") },
                    singleLine = true
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Сложность: ${state.difficulty.toInt()}/5")
                        Slider(
                            value = state.difficulty,
                            onValueChange = vm::setDifficulty,
                            valueRange = 1f..5f,
                            steps = 3
                        )
                    }
                }
            }

            item { Divider() }

            // --- Ингредиенты ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Ингредиенты")
                    Button(onClick = vm::addIngredient) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(" Добавить")
                    }
                }
            }

            items(items = state.ingredients, key = { it.id }) { ing ->
                IngredientRow(
                    ingredient = ing,
                    onChange = vm::updateIngredient,
                    onRemove = { vm.removeIngredient(ing.id) }
                )
            }

            item { Divider() }

            // --- Шаги ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Шаги приготовления")
                    Button(onClick = vm::addStep) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(" Добавить")
                    }
                }
            }

            items(items = state.steps, key = { it.id }) { step ->
                StepRow(
                    step = step,
                    onChange = vm::updateStep,
                    onRemove = { vm.removeStep(step.id) }
                )
            }

            item { Divider() }

            // --- Сохранить ---
            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = vm::save,
                    enabled = !state.saving
                ) {
                    Text(if (state.saving) "Сохранение..." else "Сохранить")
                }

                if (state.error != null) {
                    Text(
                        text = state.error!!,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun IngredientRow(
    ingredient: IngredientUi,
    onChange: (IngredientUi) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Ингредиент")
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = "Удалить")
                }
            }

            OutlinedTextField(
                value = ingredient.name,
                onValueChange = { onChange(ingredient.copy(name = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Название") },
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = ingredient.amount,
                    onValueChange = { onChange(ingredient.copy(amount = it)) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Кол-во") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = ingredient.unit,
                    onValueChange = { onChange(ingredient.copy(unit = it)) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Ед.") },
                    singleLine = true
                )
            }
        }
    }
}

@Composable
private fun StepRow(
    step: StepUi,
    onChange: (StepUi) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Шаг")
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = "Удалить")
                }
            }

            OutlinedTextField(
                value = step.text,
                onValueChange = { onChange(step.copy(text = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Описание шага") },
                minLines = 2
            )
        }
    }
}
