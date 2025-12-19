package com.janerli.delishhub.feature.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.janerli.delishhub.domain.repository.RecipeRepository

class ExportViewModelFactory(
    private val repository: RecipeRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ExportViewModel(repository) as T
    }
}
