package com.janerli.delishhub.feature.export

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.janerli.delishhub.core.session.SessionManager
import com.janerli.delishhub.data.local.entity.RecipeEntity
import com.janerli.delishhub.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ExportViewModel(
    private val repository: RecipeRepository
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val items: List<RecipeEntity> = emptyList(),
        val error: String? = null,
        val exportingId: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val session = SessionManager.session.value
            val ownerId = if (session.isGuest) "guest" else session.userId

            // На экране "Экспорт" логичнее показывать свои рецепты
            val onlyMine = true

            repository.observeCatalog(
                ownerId = ownerId,
                onlyMine = onlyMine,
                filters = RecipeRepository.RecipeFilters(),
                sort = RecipeRepository.RecipeSort.UPDATED_DESC
            ).collectLatest { list ->
                _state.value = _state.value.copy(
                    loading = false,
                    items = list,
                    error = null
                )
            }
        }
    }

    fun exportAndShare(context: Context, recipeId: String, onShareReady: (RecipeExportManager.ExportResult) -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(exportingId = recipeId, error = null)
            try {
                val full = repository.getRecipeFull(recipeId)
                    ?: throw IllegalStateException("Рецепт не найден")
                val result = RecipeExportManager.exportRecipeToPdf(context, full)
                onShareReady(result)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(error = t.message ?: "Ошибка экспорта")
            } finally {
                _state.value = _state.value.copy(exportingId = null)
            }
        }
    }
}
