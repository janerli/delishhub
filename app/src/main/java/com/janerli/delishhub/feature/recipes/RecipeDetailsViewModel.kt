package com.janerli.delishhub.feature.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.janerli.delishhub.core.session.SessionManager
import com.janerli.delishhub.data.local.model.RecipeFull
import com.janerli.delishhub.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecipeDetailsViewModel(
    private val repository: RecipeRepository,
    private val recipeId: String
) : ViewModel() {

    data class UiState(
        val recipe: RecipeFull? = null,
        val isFavorite: Boolean = false
    )

    private val userId: String get() = SessionManager.session.value.userId

    val state: StateFlow<UiState> =
        combine(
            repository.observeRecipeFull(recipeId),
            repository.observeIsFavorite(userId, recipeId)
        ) { full, fav ->
            UiState(
                recipe = full,
                isFavorite = fav
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            UiState()
        )

    fun toggleFavorite() {
        viewModelScope.launch {
            repository.toggleFavorite(userId, recipeId)
        }
    }

    /** ✅ Шаг 5.6: добавить ингредиенты этого рецепта в покупки */
    fun addToShopping() {
        viewModelScope.launch {
            repository.addToShoppingFromRecipe(
                userId = userId,
                recipeId = recipeId
            )
        }
    }
}
