package com.janerli.delishhub.feature.planner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.janerli.delishhub.core.di.AppGraph
import com.janerli.delishhub.core.session.SessionManager
import com.janerli.delishhub.core.ui.MainScaffold
import com.janerli.delishhub.feature.recipes.RecipesViewModel
import com.janerli.delishhub.feature.recipes.RecipesViewModelFactory
import com.janerli.delishhub.feature.recipes.ui.RecipeCardUi
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

private data class DayChipUi(
    val label: String, // "Пн"
    val date: LocalDate
)

private fun formatTimeMinutes(timeMinutes: Int?): String {
    if (timeMinutes == null) return "Без времени"
    val h = (timeMinutes / 60).coerceIn(0, 23)
    val m = (timeMinutes % 60).coerceIn(0, 59)
    return "%02d:%02d".format(h, m)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(navController: NavHostController) {
    val session by SessionManager.session.collectAsStateWithLifecycle()
    val isGuest = session.isGuest

    val appContext = LocalContext.current.applicationContext

    MainScaffold(
        navController = navController,
        title = "План питания",
        showBack = false
    ) { padding: PaddingValues ->

        if (isGuest) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("План питания скрыт в гостевом режиме.")
            }
            return@MainScaffold
        }

        val plannerVm: PlannerViewModel = viewModel(
            factory = PlannerViewModelFactory(
                repository = AppGraph.recipeRepository,
                appContext = appContext
            )
        )

        val recipesVm: RecipesViewModel = viewModel(
            factory = RecipesViewModelFactory(
                repository = AppGraph.recipeRepository,
                isMyMode = false
            )
        )
        val allRecipes: List<RecipeCardUi> by recipesVm.cards.collectAsStateWithLifecycle()

        val selectedEpoch by plannerVm.selectedDateEpoch.collectAsStateWithLifecycle()
        val slots by plannerVm.daySlots.collectAsStateWithLifecycle()

        val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM") }
        val today = remember { LocalDate.now() }
        val monday = remember { today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }

        val days = remember(monday) {
            val labels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
            (0..6).map { i ->
                DayChipUi(label = labels[i], date = monday.plusDays(i.toLong()))
            }
        }

        var showChooseSheet by remember { mutableStateOf(false) }
        var choosingMealType by remember { mutableStateOf(PlannerViewModel.MealType.BREAKFAST) }

        var showTimeDialog by remember { mutableStateOf(false) }
        var timeEditingMealType by remember { mutableStateOf(PlannerViewModel.MealType.BREAKFAST) }

        fun recipeTitleById(id: String?): String? {
            if (id == null) return null
            return allRecipes.firstOrNull { it.id == id }?.title
        }

        fun timeMinutesByMealType(t: PlannerViewModel.MealType): Int? =
            slots.firstOrNull { it.mealType == t }?.timeMinutes

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Неделя", style = MaterialTheme.typography.titleMedium)

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 8.dp)
            ) {
                items(days) { d ->
                    val isSelected = d.date.toEpochDay() == selectedEpoch
                    FilterChip(
                        selected = isSelected,
                        onClick = { plannerVm.selectDate(d.date.toEpochDay()) },
                        label = { Text("${d.label} ${d.date.format(dateFormatter)}") }
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            slots.forEach { slot ->
                val currentTitle = recipeTitleById(slot.recipeId)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(slot.mealType.title, style = MaterialTheme.typography.titleSmall)
                            Text(currentTitle ?: "Не назначено", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                formatTimeMinutes(slot.timeMinutes),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalIconButton(
                                onClick = {
                                    timeEditingMealType = slot.mealType
                                    showTimeDialog = true
                                }
                            ) {
                                Icon(Icons.Filled.Schedule, contentDescription = "Время")
                            }

                            FilledTonalIconButton(
                                onClick = { plannerVm.removeMeal(slot.mealType) },
                                enabled = slot.recipeId != null
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Удалить")
                            }

                            FilledTonalIconButton(
                                onClick = {
                                    choosingMealType = slot.mealType
                                    showChooseSheet = true
                                }
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Добавить")
                            }
                        }
                    }
                }
            }
        }

        if (showChooseSheet) {
            ModalBottomSheet(onDismissRequest = { showChooseSheet = false }) {
                ChooseRecipeSheet(
                    allRecipes = allRecipes,
                    onPick = { picked ->
                        plannerVm.setMeal(choosingMealType, picked.id)
                        showChooseSheet = false
                    },
                    onClose = { showChooseSheet = false }
                )
            }
        }

        if (showTimeDialog) {
            val current = timeMinutesByMealType(timeEditingMealType)
            val initHour = (current?.div(60)) ?: 8
            val initMinute = (current?.rem(60)) ?: 0

            key(selectedEpoch, timeEditingMealType.key, current) {
                val pickerState = rememberTimePickerState(
                    initialHour = initHour.coerceIn(0, 23),
                    initialMinute = initMinute.coerceIn(0, 59),
                    is24Hour = true
                )

                AlertDialog(
                    onDismissRequest = { showTimeDialog = false },
                    title = { Text("Время • ${timeEditingMealType.title}") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "Текущее: ${formatTimeMinutes(current)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            TimePicker(state = pickerState)
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val mins = pickerState.hour * 60 + pickerState.minute
                                plannerVm.updateMealTime(timeEditingMealType, mins)
                                showTimeDialog = false
                            }
                        ) { Text("Сохранить") }
                    },
                    dismissButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = {
                                    plannerVm.updateMealTime(timeEditingMealType, null)
                                    showTimeDialog = false
                                }
                            ) { Text("Сбросить") }

                            TextButton(onClick = { showTimeDialog = false }) { Text("Отмена") }
                        }
                    }
                )
            }
        }
    }
}
