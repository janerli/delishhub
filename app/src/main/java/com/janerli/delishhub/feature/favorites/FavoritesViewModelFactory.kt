package com.janerli.delishhub.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.janerli.delishhub.domain.repository.RecipeRepository

class FavoritesViewModelFactory(
    private val repository: RecipeRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(FavoritesViewModel::class.java))
        return FavoritesViewModel(repository) as T
    }
}
