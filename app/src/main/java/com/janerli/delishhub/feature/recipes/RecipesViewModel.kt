package com.janerli.delishhub.feature.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.janerli.delishhub.core.session.SessionManager
import com.janerli.delishhub.data.local.entity.RecipeEntity
import com.janerli.delishhub.domain.repository.RecipeRepository
import com.janerli.delishhub.feature.recipes.ui.RecipeCardUi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Шаг 5.1 (под твой проект):
 * - Берём список рецептов из Room через repository.observeCatalog(...)
 * - Берём список избранного через repository.observeFavorites(...)
 * - Склеиваем в RecipeCardUi с корректным isFavorite
 *
 * Фильтры/сортировка пока остаются на UI-уровне (как у тебя уже сделано).
 */
class RecipesViewModel(
    private val repository: RecipeRepository,
    private val isMyMode: Boolean
) : ViewModel() {

    private val sessionFlow = SessionManager.session

    private val baseRecipesFlow = sessionFlow.flatMapLatest { session ->
        repository.observeCatalog(
            ownerId = session.userId,
            onlyMine = isMyMode,
            filters = RecipeRepository.RecipeFilters(),
            sort = RecipeRepository.RecipeSort.UPDATED_DESC
        )
    }

    private val favoritesFlow = sessionFlow.flatMapLatest { session ->
        repository.observeFavorites(
            userId = session.userId,
            filters = RecipeRepository.RecipeFilters(),
            sort = RecipeRepository.RecipeSort.UPDATED_DESC
        )
    }

    val cards: StateFlow<List<RecipeCardUi>> =
        combine(baseRecipesFlow, favoritesFlow) { recipes: List<RecipeEntity>, favs: List<RecipeEntity> ->
            val favSet = favs.asSequence().map { it.id }.toHashSet()

            recipes.map { r ->
                RecipeCardUi(
                    id = r.id,
                    title = r.title,
                    cookTimeMin = r.cookTimeMin,
                    difficulty = r.difficulty,
                    isFavorite = favSet.contains(r.id)
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun toggleFavorite(recipeId: String) {
        viewModelScope.launch {
            val userId = SessionManager.session.value.userId
            repository.toggleFavorite(userId, recipeId)
        }
    }
}
