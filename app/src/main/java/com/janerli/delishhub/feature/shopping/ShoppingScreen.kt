package com.janerli.delishhub.feature.shopping

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.janerli.delishhub.core.di.AppGraph
import com.janerli.delishhub.core.session.SessionManager
import com.janerli.delishhub.core.ui.MainScaffold
import com.janerli.delishhub.data.local.entity.ShoppingItemEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingScreen(navController: NavHostController) {

    val session by SessionManager.session.collectAsStateWithLifecycle()
    val isGuest = session.isGuest

    val vm: ShoppingViewModel = viewModel(
        factory = ShoppingViewModelFactory(AppGraph.recipeRepository)
    )

    var query by remember { mutableStateOf("") }

    var showClearDoneDialog by remember { mutableStateOf(false) }
    var isAddOpen by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }

    var showChoosePlanDay by remember { mutableStateOf(false) }

    val itemsAll by vm.items.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()

    MainScaffold(
        navController = navController,
        title = "Покупки",
        showBack = false,
        fab = {
            if (!isGuest) {
                FloatingActionButton(onClick = { isAddOpen = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Добавить")
                }
            }
        }
    ) { padding: PaddingValues ->

        if (isGuest) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Список покупок скрыт в гостевом режиме.")
            }
            return@MainScaffold
        }

        val filtered = remember(itemsAll, query) {
            val q = query.trim()
            if (q.isEmpty()) itemsAll
            else itemsAll.filter { it.name.contains(q, ignoreCase = true) }
        }
        val todo = filtered.filter { !it.isChecked }
        val done = filtered.filter { it.isChecked }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Поиск покупок") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Прогресс")
                        IconButton(onClick = { showClearDoneDialog = true }) {
                            Icon(Icons.Filled.TaskAlt, contentDescription = "Очистить купленное")
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )

                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showChoosePlanDay = true }
                    ) {
                        Text("Добавить покупки из плана (выбрать день)")
                    }
                }
            }

            if (itemsAll.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Пока пусто")
                        Text("Добавь вручную через кнопку + или импортируй из плана.")
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {

                    if (todo.isNotEmpty()) {
                        item { Text("Нужно купить") }
                        items(items = todo, key = { it.id }) { item ->
                            ShoppingRow(
                                item = item,
                                onToggle = { checked -> vm.toggleChecked(item.id, checked) },
                                onDelete = { vm.delete(item.id) }
                            )
                        }
                    }

                    if (done.isNotEmpty()) {
                        item { Text("Куплено") }
                        items(items = done, key = { it.id }) { item ->
                            ShoppingRow(
                                item = item,
                                onToggle = { checked -> vm.toggleChecked(item.id, checked) },
                                onDelete = { vm.delete(item.id) }
                            )
                        }
                    }
                }
            }
        }

        if (showClearDoneDialog) {
            AlertDialog(
                onDismissRequest = { showClearDoneDialog = false },
                title = { Text("Очистить купленное?") },
                text = { Text("Удалить все элементы из раздела «Куплено»?") },
                confirmButton = {
                    TextButton(onClick = {
                        showClearDoneDialog = false
                        vm.clearChecked()
                    }) { Text("Очистить") }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDoneDialog = false }) { Text("Отмена") }
                }
            )
        }

        // -----------------------
        // ✅ Валидация Add Dialog
        // -----------------------
        if (isAddOpen) {
            val nameTrim = name.trim()
            val qtyTrim = qty.trim()

            val nameError: String? = when {
                nameTrim.isEmpty() -> "Введите название"
                nameTrim.length < 2 -> "Минимум 2 символа"
                nameTrim.length > 60 -> "Максимум 60 символов"
                else -> null
            }

            val qtyError: String? = when {
                qtyTrim.isEmpty() -> null
                qtyTrim.length > 40 -> "Слишком длинно (макс. 40 символов)"
                else -> null
            }

            AlertDialog(
                onDismissRequest = { isAddOpen = false },
                title = { Text("Добавить покупку") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { if (it.length <= 80) name = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Название*") },
                            singleLine = true,
                            isError = nameError != null,
                            supportingText = {
                                if (nameError != null) {
                                    Text(nameError, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        )
                        OutlinedTextField(
                            value = qty,
                            onValueChange = { if (it.length <= 60) qty = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Количество (текст, не обязательно)") },
                            singleLine = true,
                            isError = qtyError != null,
                            supportingText = {
                                if (qtyError != null) {
                                    Text(qtyError, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            vm.addManual(nameTrim, qtyTrim.takeIf { it.isNotEmpty() })
                            name = ""
                            qty = ""
                            isAddOpen = false
                        },
                        enabled = nameError == null && qtyError == null
                    ) { Text("Добавить") }
                },
                dismissButton = {
                    TextButton(onClick = { isAddOpen = false }) { Text("Отмена") }
                }
            )
        }

        if (showChoosePlanDay) {
            ModalBottomSheet(onDismissRequest = { showChoosePlanDay = false }) {
                ChoosePlanDaySheet(
                    onPick = { epoch ->
                        vm.addFromPlanDay(epoch)
                        showChoosePlanDay = false
                    },
                    onClose = { showChoosePlanDay = false }
                )
            }
        }
    }
}

@Composable
private fun ShoppingRow(
    item: ShoppingItemEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.name)

                val qty = when {
                    item.amount != null && !item.unit.isNullOrBlank() ->
                        "${item.amount.toString().removeSuffix(".0")} ${item.unit}"
                    item.amount != null ->
                        item.amount.toString().removeSuffix(".0")
                    !item.qtyText.isNullOrBlank() ->
                        item.qtyText
                    else -> ""
                }
                if (qty.isNotBlank()) Text(qty)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = item.isChecked,
                    onCheckedChange = { onToggle(it) }
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Удалить")
                }
            }
        }
    }
}
