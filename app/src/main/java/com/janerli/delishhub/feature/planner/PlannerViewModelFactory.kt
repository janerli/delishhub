package com.janerli.delishhub.feature.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.janerli.delishhub.domain.repository.RecipeRepository

class PlannerViewModelFactory(
    private val repository: RecipeRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(PlannerViewModel::class.java))
        return PlannerViewModel(repository) as T
    }
}
