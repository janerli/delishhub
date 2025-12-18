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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.janerli.delishhub.core.di.AppGraph
import com.janerli.delishhub.core.ui.MainScaffold
import com.janerli.delishhub.feature.recipes.ui.IngredientUi
import com.janerli.delishhub.feature.recipes.ui.StepUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditScreen(
    navController: NavHostController,
    recipeId: String
) {
    val vm: RecipeEditorViewModel = viewModel(
        factory = RecipeEditorViewModelFactory(
            repository = AppGraph.recipeRepository,
            recipeId = recipeId
        )
    )
    val state by vm.state.collectAsStateWithLifecycle()

    var showSavedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            vm.consumeSaved()
            showSavedDialog = true
        }
    }

    MainScaffold(
        navController = navController,
        title = "Редактировать рецепт",
        showBack = true,
        onBack = { navController.popBackStack() }
    ) { padding: PaddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {

            item {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = vm::setTitle,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Название") },
                    isError = state.titleError != null,
                    singleLine = true
                )
                if (state.titleError != null) {
                    Text(text = state.titleError!!, modifier = Modifier.padding(top = 4.dp))
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
                    label = { Text("Время (мин)") },
                    singleLine = true
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp)
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

            // ---------- ИНГРЕДИЕНТЫ ----------
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(1.dp)
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
                            IconButton(onClick = { vm.removeIngredient(ing.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Удалить")
                            }
                        }

                        OutlinedTextField(
                            value = ing.name,
                            onValueChange = { v: String ->
                                vm.updateIngredient(ing.copy(name = v))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Название") },
                            singleLine = true
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = ing.amount,
                                onValueChange = { v: String ->
                                    vm.updateIngredient(ing.copy(amount = v))
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text("Кол-во") },
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = ing.unit,
                                onValueChange = { v: String ->
                                    vm.updateIngredient(ing.copy(unit = v))
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text("Ед.") },
                                singleLine = true
                            )
                        }
                    }
                }
            }

            // ---------- ШАГИ ----------
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Шаги")
                    Button(onClick = vm::addStep) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(" Добавить")
                    }
                }
            }

            items(items = state.steps, key = { it.id }) { step ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(1.dp)
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
                            IconButton(onClick = { vm.removeStep(step.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Удалить")
                            }
                        }

                        OutlinedTextField(
                            value = step.text,
                            onValueChange = { v: String ->
                                vm.updateStep(step.copy(text = v))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Описание шага") },
                            minLines = 2
                        )
                    }
                }
            }

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

        if (showSavedDialog) {
            AlertDialog(
                onDismissRequest = { showSavedDialog = false },
                title = { Text("Готово") },
                text = { Text("Рецепт сохранён.") },
                confirmButton = {
                    TextButton(onClick = {
                        showSavedDialog = false
                        navController.popBackStack()
                    }) { Text("ОК") }
                }
            )
        }
    }
}
