package com.janerli.delishhub.feature.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.janerli.delishhub.core.session.SessionManager
import com.janerli.delishhub.data.local.entity.RecipeEntity
import com.janerli.delishhub.data.local.entity.TagEntity
import com.janerli.delishhub.domain.repository.RecipeRepository
import com.janerli.delishhub.feature.recipes.ui.RecipeCardUi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf

class RecipesViewModel(
    private val repository: RecipeRepository,
    private val isMyMode: Boolean
) : ViewModel() {

    private val sessionFlow = SessionManager.session

    // ✅ все теги (для FiltersSheet)
    val allTags: StateFlow<List<TagEntity>> =
        repository.observeAllTags()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ✅ выбранные теги (для получения recipeIds)
    private val _selectedTagIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedTagIds: StateFlow<Set<String>> = _selectedTagIds.asStateFlow()

    fun setSelectedTagIds(ids: Set<String>) {
        _selectedTagIds.value = ids
    }

    // ✅ recipeIds подходящих по тегам (OR-логика)
    @OptIn(ExperimentalCoroutinesApi::class)
    val tagMatchedRecipeIds: StateFlow<Set<String>> =
        _selectedTagIds.flatMapLatest { ids ->
            if (ids.isEmpty()) flowOf(emptySet())
            else repository.observeRecipeIdsByTagIds(ids.toList())
                .flatMapLatest { list -> flowOf(list.toSet()) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val baseRecipesFlow = sessionFlow.flatMapLatest { session ->
        repository.observeCatalog(
            ownerId = session.userId,
            onlyMine = isMyMode,
            filters = RecipeRepository.RecipeFilters(),
            sort = RecipeRepository.RecipeSort.UPDATED_DESC
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val favoritesFlow = sessionFlow.flatMapLatest { session ->
        repository.observeFavorites(
            userId = session.userId,
            filters = RecipeRepository.RecipeFilters(),
            sort = RecipeRepository.RecipeSort.UPDATED_DESC
        )
    }

    val cards: StateFlow<List<RecipeCardUi>> =
        combine(sessionFlow, baseRecipesFlow, favoritesFlow) { session, recipes: List<RecipeEntity>, favs: List<RecipeEntity> ->
            val favSet = favs.asSequence().map { it.id }.toHashSet()
            val myId = session.userId
            val isGuest = session.isGuest

            recipes.map { r ->
                RecipeCardUi(
                    id = r.id,
                    title = r.title,
                    cookTimeMin = r.cookTimeMin,
                    difficulty = r.difficulty,
                    isFavorite = favSet.contains(r.id),
                    imageUrl = r.mainImageUrl,
                    ownerId = r.ownerId,
                    isPublic = r.isPublic,
                    isMine = !isGuest && r.ownerId == myId
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
