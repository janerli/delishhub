package com.janerli.delishhub.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.janerli.delishhub.core.session.SessionManager
import com.janerli.delishhub.data.local.entity.RecipeEntity
import com.janerli.delishhub.domain.repository.RecipeRepository
import com.janerli.delishhub.feature.recipes.ui.RecipeCardUi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val repository: RecipeRepository
) : ViewModel() {

    private val favoritesFlow = SessionManager.session.flatMapLatest { session ->
        repository.observeFavorites(
            userId = session.userId,
            filters = RecipeRepository.RecipeFilters(),
            sort = RecipeRepository.RecipeSort.UPDATED_DESC
        )
    }

    val cards: StateFlow<List<RecipeCardUi>> =
        favoritesFlow
            .map { list: List<RecipeEntity> ->
                list.map { r ->
                    RecipeCardUi(
                        id = r.id,
                        title = r.title,
                        cookTimeMin = r.cookTimeMin,
                        difficulty = r.difficulty,
                        isFavorite = true // это экран избранного
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun toggleFavorite(recipeId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(
                userId = SessionManager.session.value.userId,
                recipeId = recipeId
            )
        }
    }
}
