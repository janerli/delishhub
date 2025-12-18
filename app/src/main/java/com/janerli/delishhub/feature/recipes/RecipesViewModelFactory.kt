package com.janerli.delishhub.feature.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.janerli.delishhub.domain.repository.RecipeRepository

class RecipesViewModelFactory(
    private val repository: RecipeRepository,
    private val isMyMode: Boolean
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(RecipesViewModel::class.java))
        return RecipesViewModel(repository, isMyMode) as T
    }
}
