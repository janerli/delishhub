package com.janerli.delishhub.feature.planner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(navController: NavHostController) {
    val session by SessionManager.session.collectAsStateWithLifecycle()
    val isGuest = session.isGuest

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
            factory = PlannerViewModelFactory(AppGraph.recipeRepository)
        )

        // Берём все рецепты (для выбора в план)
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

        fun recipeTitleById(id: String?): String? {
            if (id == null) return null
            return allRecipes.firstOrNull { it.id == id }?.title
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text("Неделя", style = MaterialTheme.typography.titleMedium)

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(days) { d ->
                    val isSelected = d.date.toEpochDay() == selectedEpoch
                    FilledTonalButton(onClick = { plannerVm.selectDate(d.date.toEpochDay()) }) {
                        Text("${d.label} ${d.date.format(dateFormatter)}")
                        if (isSelected) Text(" ✓")
                    }
                }
            }

            // Карточки слотов
            slots.forEach { slot ->
                val currentTitle = recipeTitleById(slot.recipeId)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(slot.mealType.title, style = MaterialTheme.typography.titleSmall)
                            Text(currentTitle ?: "Не назначено")
                        }

                        androidx.compose.foundation.layout.Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // удалить слот
                            FilledTonalButton(
                                onClick = { plannerVm.removeMeal(slot.mealType) },
                                enabled = slot.recipeId != null
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = null)
                            }

                            // выбрать рецепт
                            FilledTonalButton(
                                onClick = {
                                    choosingMealType = slot.mealType
                                    showChooseSheet = true
                                }
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Text(" Добавить")
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
    }
}
