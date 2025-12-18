package com.janerli.delishhub.feature.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.janerli.delishhub.core.session.SessionManager
import com.janerli.delishhub.data.local.entity.ShoppingItemEntity
import com.janerli.delishhub.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class ShoppingViewModel(
    private val repository: RecipeRepository
) : ViewModel() {

    private val userId: String get() = SessionManager.session.value.userId

    val items: StateFlow<List<ShoppingItemEntity>> =
        repository.observeShopping(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val progress: StateFlow<Float> =
        items.map { list ->
            if (list.isEmpty()) 0f
            else list.count { it.isChecked }.toFloat() / list.size.toFloat()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)

    fun addManual(name: String, qtyText: String?) {
        viewModelScope.launch {
            repository.addShoppingManual(userId, name, qtyText)
        }
    }

    fun toggleChecked(id: String, checked: Boolean) {
        viewModelScope.launch {
            repository.toggleShoppingChecked(userId, id, checked)
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            repository.deleteShoppingItem(userId, id)
        }
    }

    fun clearChecked() {
        viewModelScope.launch {
            repository.clearCheckedShopping(userId)
        }
    }

    /** было: только today */
    fun addFromPlanToday() {
        addFromPlanDay(LocalDate.now().toEpochDay())
    }

    /** ✅ Шаг 5.6: импорт из выбранного дня */
    fun addFromPlanDay(epochDay: Long) {
        viewModelScope.launch {
            repository.addToShoppingFromPlanDay(userId, epochDay)
        }
    }
}
