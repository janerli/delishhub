package com.janerli.delishhub.feature.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.janerli.delishhub.domain.repository.RecipeRepository

class RecipeEditorViewModelFactory(
    private val repository: RecipeRepository,
    private val recipeId: String?
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(RecipeEditorViewModel::class.java))
        return RecipeEditorViewModel(repository, recipeId) as T
    }
}
