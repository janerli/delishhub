package com.janerli.delishhub.feature.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.janerli.delishhub.core.di.AppGraph
import com.janerli.delishhub.data.local.entity.TagEntity
import kotlinx.coroutines.launch

@Composable
fun AdminTagsScreen() {
    val scope = rememberCoroutineScope()

    val tags by AppGraph.recipeRepository
        .observeAllTags()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<TagEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Теги",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            FilledTonalIconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить тег")
            }
        }

        Spacer(Modifier.height(12.dp))

        if (tags.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Тегов пока нет")
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(tags, key = { it.id }) { tag ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tag.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        IconButton(onClick = { deleteTarget = tag }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Удалить")
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTagDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { rawName ->
                scope.launch { AppGraph.recipeRepository.upsertTag(rawName) }
                showAddDialog = false
            }
        )
    }

    deleteTarget?.let { tag ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Удалить тег?") },
            text = { Text("Тег «${tag.name}» будет удалён.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { AppGraph.recipeRepository.deleteTag(tag.id) }
                    deleteTarget = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun AddTagDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var value by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый тег") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                label = { Text("Название тега") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onAdd(value) }) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
