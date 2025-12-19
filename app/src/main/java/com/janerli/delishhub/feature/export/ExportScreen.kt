package com.janerli.delishhub.feature.export

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.janerli.delishhub.core.di.AppGraph
import com.janerli.delishhub.core.share.ShareHelper
import com.janerli.delishhub.core.ui.MainScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ExportScreen(
    navController: NavHostController,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val vm: ExportViewModel = viewModel(
        factory = ExportViewModelFactory(repository = AppGraph.recipeRepository)
    )

    val state by vm.state.collectAsStateWithLifecycle()

    var infoMessage by remember { mutableStateOf<String?>(null) }

    // Держим файл, который надо сохранить, пока пользователь выбирает место
    var pendingSaveFile by remember { mutableStateOf<File?>(null) }

    val saveToFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        val file = pendingSaveFile
        pendingSaveFile = null

        if (uri == null || file == null) return@rememberLauncherForActivityResult

        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        file.inputStream().use { input -> input.copyTo(out) }
                    } ?: throw IllegalStateException("Не удалось открыть место сохранения")
                }
                infoMessage = "PDF сохранён в файлы ✅"
            } catch (t: Throwable) {
                infoMessage = "Ошибка сохранения: ${t.message ?: "неизвестно"}"
            }
        }
    }

    MainScaffold(
        navController = navController,
        title = "Экспорт / Поделиться",
        showBack = true,
        onBack = onBack
    ) { padding: PaddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.loading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            if (state.error != null) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }

            if (infoMessage != null) {
                Text(infoMessage!!)
            }

            if (state.items.isEmpty()) {
                Text("Нет рецептов для экспорта.")
                return@Column
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = state.items,
                    key = { it.id }
                ) { recipe ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(recipe.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Время: ${recipe.cookTimeMin} мин • Сложность: ${recipe.difficulty}/5",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            // 1) Поделиться (как было)
                            FilledTonalButton(
                                modifier = Modifier.fillMaxWidth(),
                                enabled = state.exportingId == null,
                                onClick = {
                                    infoMessage = null
                                    vm.exportAndShare(context, recipe.id) { result ->
                                        ShareHelper.sharePdf(
                                            context = context,
                                            file = result.file,
                                            chooserTitle = "Поделиться PDF"
                                        )
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.FileUpload, contentDescription = null)
                                Text(if (state.exportingId == recipe.id) " Экспорт…" else " Поделиться PDF")
                            }

                            // 2) Сохранить в файлы (новое)
                            FilledTonalButton(
                                modifier = Modifier.fillMaxWidth(),
                                enabled = state.exportingId == null,
                                onClick = {
                                    infoMessage = null
                                    vm.exportAndShare(context, recipe.id) { result ->
                                        pendingSaveFile = result.file
                                        // откроем системный диалог Files с именем по умолчанию
                                        saveToFilesLauncher.launch(result.displayName)
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null)
                                Text(if (state.exportingId == recipe.id) " Экспорт…" else " Сохранить в файлы")
                            }
                        }
                    }
                }
            }
        }
    }
}
