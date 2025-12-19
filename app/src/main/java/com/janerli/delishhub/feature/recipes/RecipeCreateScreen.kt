package com.janerli.delishhub.feature.recipes

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.janerli.delishhub.core.di.AppGraph
import com.janerli.delishhub.core.media.ImageStorage
import com.janerli.delishhub.feature.recipes.ui.IngredientUi
import com.janerli.delishhub.feature.recipes.ui.StepUi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecipeCreateScreen(onBack: () -> Unit) {

    val vm: RecipeEditorViewModel = viewModel(
        factory = RecipeEditorViewModelFactory(
            repository = AppGraph.recipeRepository,
            recipeId = null
        )
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Tags UI state
    var showTagSheet by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }

    val pickFromGallery = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val saved = ImageStorage.copyFromUriToInternal(
                filesDir = context.filesDir,
                contentResolver = context.contentResolver,
                sourceUri = uri,
                recipeId = state.draftId
            )
            vm.setMainImageUrl(saved?.toString())
        }
    }

    val takePhotoPreview = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp: Bitmap? ->
        if (bmp != null) {
            val saved = ImageStorage.saveBitmapToInternal(
                filesDir = context.filesDir,
                bitmap = bmp,
                recipeId = state.draftId
            )
            vm.setMainImageUrl(saved?.toString())
        }
    }

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
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

            // --- Фото ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Главное фото (локально)")

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilledTonalButton(onClick = { pickFromGallery.launch("image/*") }) {
                                Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                                Text(" Галерея")
                            }
                            FilledTonalButton(onClick = { takePhotoPreview.launch(null) }) {
                                Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                                Text(" Камера")
                            }
                        }

                        if (!state.mainImageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = state.mainImageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                            )
                            FilledTonalButton(onClick = vm::removeMainPhoto) {
                                Text("Убрать фото")
                            }
                        } else {
                            Text("Фото не выбрано")
                        }
                    }
                }
            }

            // --- Публичность ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Публичный рецепт")
                            Text("Если включено — рецепт увидят все (включая гостей).")
                        }
                        Switch(
                            checked = state.isPublic,
                            onCheckedChange = vm::setPublic,
                            enabled = !state.saving
                        )
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

            // --- Теги ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Теги")

                        val selected = state.selectedTagIds
                            .mapNotNull { id -> state.allTags.find { it.id == id } }
                            .sortedBy { it.name.lowercase() }

                        if (selected.isEmpty()) {
                            Text("Нет выбранных тегов")
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                selected.forEach { tag ->
                                    AssistChip(
                                        onClick = { vm.toggleTag(tag.id) },
                                        label = { Text(tag.name) },
                                        colors = AssistChipDefaults.assistChipColors()
                                    )
                                }
                            }
                        }

                        TextButton(onClick = { showTagSheet = true }) {
                            Text("Выбрать теги")
                        }
                    }
                }
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

            // --- Ингредиенты ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Ингредиенты")
                    Button(onClick = vm::addIngredient, enabled = !state.saving) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(" Добавить")
                    }
                }
            }

            items(items = state.ingredients, key = { it.id }) { ing: IngredientUi ->
                CreateIngredientRow(
                    ingredient = ing,
                    onChange = vm::updateIngredient,
                    onRemove = { vm.removeIngredient(ing.id) }
                )
            }

            // --- Шаги ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Шаги приготовления")
                    Button(onClick = vm::addStep, enabled = !state.saving) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(" Добавить")
                    }
                }
            }

            items(items = state.steps, key = { it.id }) { step: StepUi ->
                CreateStepRow(
                    step = step,
                    onChange = vm::updateStep,
                    onRemove = { vm.removeStep(step.id) }
                )
            }

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
                    Text(text = state.error!!, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }

    // BottomSheet выбора/создания тегов
    if (showTagSheet) {
        ModalBottomSheet(onDismissRequest = { showTagSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Все теги")

                if (state.allTags.isEmpty()) {
                    Text("Тегов пока нет. Создай первый ниже 👇")
                } else {
                    state.allTags
                        .sortedBy { it.name.lowercase() }
                        .forEach { tag ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(tag.name)
                                Switch(
                                    checked = state.selectedTagIds.contains(tag.id),
                                    onCheckedChange = { vm.toggleTag(tag.id) }
                                )
                            }
                        }
                }

                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    label = { Text("Новый тег") },
                    singleLine = true
                )

                Button(
                    onClick = {
                        vm.addTag(newTagName)
                        newTagName = ""
                    }
                ) { Text("Добавить тег") }

                TextButton(onClick = { showTagSheet = false }) { Text("Готово") }
            }
        }
    }
}

@Composable
private fun CreateIngredientRow(
    ingredient: IngredientUi,
    onChange: (IngredientUi) -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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
private fun CreateStepRow(
    step: StepUi,
    onChange: (StepUi) -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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
