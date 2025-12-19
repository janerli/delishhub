package com.janerli.delishhub.feature.planner

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.janerli.delishhub.core.session.SessionManager
import com.janerli.delishhub.data.local.entity.MealPlanEntryEntity
import com.janerli.delishhub.data.notifications.MealPlanReminderScheduler
import com.janerli.delishhub.domain.repository.RecipeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class PlannerViewModel(
    private val repository: RecipeRepository,
    private val appContext: Context
) : ViewModel() {

    enum class MealType(val key: String, val title: String) {
        BREAKFAST("BREAKFAST", "Завтрак"),
        LUNCH("LUNCH", "Обед"),
        DINNER("DINNER", "Ужин"),
        SNACK("SNACK", "Перекус")
    }

    private val _selectedDateEpoch = MutableStateFlow(LocalDate.now().toEpochDay())
    val selectedDateEpoch: StateFlow<Long> = _selectedDateEpoch

    fun selectDate(epochDay: Long) {
        _selectedDateEpoch.value = epochDay
    }

    data class SlotState(
        val mealType: MealType,
        val recipeId: String?,
        val timeMinutes: Int?
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val daySlots: StateFlow<List<SlotState>> =
        _selectedDateEpoch
            .flatMapLatest { day ->
                repository.observeMealPlanDay(
                    userId = SessionManager.session.value.userId,
                    dateEpochDay = day
                )
            }
            .map { list: List<MealPlanEntryEntity> ->
                val map = list.associateBy { it.mealType }
                MealType.entries.map { t ->
                    val e = map[t.key]
                    SlotState(
                        mealType = t,
                        recipeId = e?.recipeId,
                        timeMinutes = e?.timeMinutes
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = MealType.entries.map { SlotState(it, null, null) }
            )

    fun setMeal(mealType: MealType, recipeId: String) {
        viewModelScope.launch {
            repository.setMeal(
                userId = SessionManager.session.value.userId,
                dateEpochDay = _selectedDateEpoch.value,
                mealType = mealType.key,
                recipeId = recipeId,
                servings = 1,
                timeMinutes = null // ✅ не затираем существующее время
            )
            // ✅ пересобираем уведомления сразу
            MealPlanReminderScheduler.scheduleRefreshNow(appContext)
        }
    }

    fun removeMeal(mealType: MealType) {
        viewModelScope.launch {
            repository.removeMeal(
                userId = SessionManager.session.value.userId,
                dateEpochDay = _selectedDateEpoch.value,
                mealType = mealType.key
            )
            // ✅ пересобираем уведомления сразу
            MealPlanReminderScheduler.scheduleRefreshNow(appContext)
        }
    }

    fun updateMealTime(mealType: MealType, timeMinutes: Int?) {
        viewModelScope.launch {
            repository.updateMealTime(
                userId = SessionManager.session.value.userId,
                dateEpochDay = _selectedDateEpoch.value,
                mealType = mealType.key,
                timeMinutes = timeMinutes
            )
            // ✅ пересобираем уведомления сразу
            MealPlanReminderScheduler.scheduleRefreshNow(appContext)
        }
    }
}
