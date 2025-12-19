package com.janerli.delishhub.feature.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.janerli.delishhub.core.session.SessionManager
import com.janerli.delishhub.data.local.entity.IngredientEntity
import com.janerli.delishhub.data.local.entity.RecipeEntity
import com.janerli.delishhub.data.local.entity.StepEntity
import com.janerli.delishhub.data.local.entity.TagEntity
import com.janerli.delishhub.data.local.model.RecipeFull
import com.janerli.delishhub.domain.repository.RecipeRepository
import com.janerli.delishhub.feature.recipes.ui.IngredientUi
import com.janerli.delishhub.feature.recipes.ui.StepUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

class RecipeEditorViewModel(
    private val repository: RecipeRepository,
    private val recipeId: String?
) : ViewModel() {

    private val stableId: String = recipeId ?: UUID.randomUUID().toString()

    data class UiState(
        val loading: Boolean = false,
        val saving: Boolean = false,
        val saved: Boolean = false,
        val error: String? = null,

        val draftId: String = "",

        val title: String = "",
        val description: String = "",
        val cookTime: String = "20",
        val difficulty: Float = 2f,

        val isPublic: Boolean = false,
        val mainImageUrl: String? = null,

        // --- validation ---
        val titleError: String? = null,
        val cookTimeError: String? = null,
        val ingredientsError: String? = null,
        val stepsError: String? = null,

        val ingredients: List<IngredientUi> = listOf(
            IngredientUi(id = "i1", name = "", amount = "", unit = "")
        ),
        val steps: List<StepUi> = listOf(
            StepUi(id = "s1", text = "")
        ),

        val allTags: List<TagEntity> = emptyList(),
        val selectedTagIds: Set<String> = emptySet(),

        val existingCreatedAt: Long? = null
    )

    private val _state = MutableStateFlow(
        UiState(
            loading = recipeId != null,
            draftId = stableId
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAllTags().collect { tags ->
                _state.value = _state.value.copy(allTags = tags)
            }
        }

        if (recipeId != null) {
            viewModelScope.launch {
                runCatching { repository.getRecipeFull(recipeId) }
                    .onSuccess { full ->
                        if (full == null) {
                            _state.value = _state.value.copy(
                                loading = false,
                                error = "Рецепт не найден"
                            )
                        } else {
                            applyFullToState(full)
                        }
                    }
                    .onFailure {
                        _state.value = _state.value.copy(
                            loading = false,
                            error = it.message ?: "Ошибка загрузки рецепта"
                        )
                    }
            }
        }
    }

    private fun applyFullToState(full: RecipeFull) {
        val r = full.recipe

        _state.value = _state.value.copy(
            loading = false,
            error = null,
            title = r.title,
            description = r.description,
            cookTime = r.cookTimeMin.toString(),
            difficulty = r.difficulty.coerceIn(1, 5).toFloat(),
            isPublic = r.isPublic,
            mainImageUrl = r.mainImageUrl,
            selectedTagIds = full.tags.map { it.id }.toSet(),
            existingCreatedAt = r.createdAt,
            ingredients = full.ingredients
                .sortedBy { it.position }
                .map {
                    IngredientUi(
                        id = it.id,
                        name = it.name,
                        amount = it.amount?.let { a ->
                            val isInt = abs(a - a.toInt()) < 1e-9
                            if (isInt) a.toInt().toString() else a.toString()
                        } ?: "",
                        unit = it.unit.orEmpty()
                    )
                }
                .ifEmpty { listOf(IngredientUi(id = "i1", name = "", amount = "", unit = "")) },
            steps = full.steps
                .sortedBy { it.position }
                .map { StepUi(id = it.id, text = it.text) }
                .ifEmpty { listOf(StepUi(id = "s1", text = "")) }
        )
    }

    // ----------------------------------------------------------------
    // TAGS (FIX)
    // ----------------------------------------------------------------

    fun toggleTag(tagId: String) {
        val current = _state.value.selectedTagIds.toMutableSet()
        if (current.contains(tagId)) current.remove(tagId) else current.add(tagId)
        _state.value = _state.value.copy(selectedTagIds = current)
    }

    fun addTag(name: String) {
        val clean = name.trim()
        if (clean.isBlank()) return

        viewModelScope.launch {
            repository.upsertTag(clean)
        }
    }

    // ----------------------------------------------------------------
    // setters
    // ----------------------------------------------------------------

    fun setTitle(v: String) {
        _state.value = _state.value.copy(title = v, titleError = null)
    }

    fun setDescription(v: String) {
        _state.value = _state.value.copy(description = v)
    }

    fun setCookTimeDigitsOnly(v: String) {
        _state.value = _state.value.copy(
            cookTime = v.filter { it.isDigit() }.take(4),
            cookTimeError = null
        )
    }

    fun setDifficulty(v: Float) {
        _state.value = _state.value.copy(difficulty = v)
    }

    fun setPublic(v: Boolean) {
        _state.value = _state.value.copy(isPublic = v)
    }

    fun setMainImageUrl(url: String?) {
        _state.value = _state.value.copy(mainImageUrl = url)
    }

    fun removeMainPhoto() {
        _state.value = _state.value.copy(mainImageUrl = null)
    }

    // -------- ingredients --------

    fun addIngredient() {
        val list = _state.value.ingredients.toMutableList()
        list.add(IngredientUi(id = "i${System.currentTimeMillis()}", name = "", amount = "", unit = ""))
        _state.value = _state.value.copy(ingredients = list, ingredientsError = null)
    }

    fun updateIngredient(updated: IngredientUi) {
        val list = _state.value.ingredients.toMutableList()
        val idx = list.indexOfFirst { it.id == updated.id }
        if (idx >= 0) list[idx] = updated
        _state.value = _state.value.copy(ingredients = list, ingredientsError = null)
    }

    fun removeIngredient(id: String) {
        val list = _state.value.ingredients.toMutableList()
        if (list.size > 1) {
            list.removeAll { it.id == id }
            _state.value = _state.value.copy(ingredients = list)
        } else {
            _state.value = _state.value.copy(
                ingredients = list.map { if (it.id == id) it.copy(name = "", amount = "", unit = "") else it }
            )
        }
    }

    // -------- steps --------

    fun addStep() {
        val list = _state.value.steps.toMutableList()
        list.add(StepUi(id = "s${System.currentTimeMillis()}", text = ""))
        _state.value = _state.value.copy(steps = list, stepsError = null)
    }

    fun updateStep(updated: StepUi) {
        val list = _state.value.steps.toMutableList()
        val idx = list.indexOfFirst { it.id == updated.id }
        if (idx >= 0) list[idx] = updated
        _state.value = _state.value.copy(steps = list, stepsError = null)
    }

    fun removeStep(id: String) {
        val list = _state.value.steps.toMutableList()
        if (list.size > 1) {
            list.removeAll { it.id == id }
            _state.value = _state.value.copy(steps = list)
        } else {
            _state.value = _state.value.copy(
                steps = list.map { if (it.id == id) it.copy(text = "") else it }
            )
        }
    }

    // -------- validation --------

    private fun validate(): Boolean {
        val s = _state.value

        var titleError: String? = null
        var cookTimeError: String? = null
        var ingredientsError: String? = null
        var stepsError: String? = null

        val title = s.title.trim()
        if (title.length < 2) titleError = "Название должно быть не короче 2 символов"
        if (title.length > 60) titleError = "Название слишком длинное"

        val cook = s.cookTime.toIntOrNull()
        if (cook == null || cook !in 0..1440) {
            cookTimeError = "Время должно быть от 0 до 1440 минут"
        }

        val validIngredients = s.ingredients.count { it.name.trim().isNotEmpty() }
        if (validIngredients == 0) {
            ingredientsError = "Добавь хотя бы один ингредиент"
        }

        val validSteps = s.steps.count { it.text.trim().isNotEmpty() }
        if (validSteps == 0) {
            stepsError = "Добавь хотя бы один шаг приготовления"
        }

        _state.value = s.copy(
            titleError = titleError,
            cookTimeError = cookTimeError,
            ingredientsError = ingredientsError,
            stepsError = stepsError
        )

        return titleError == null &&
                cookTimeError == null &&
                ingredientsError == null &&
                stepsError == null
    }

    // -------- save --------

    fun save() {
        if (!validate()) return

        val s = _state.value
        _state.value = s.copy(saving = true, error = null)

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val ownerId = SessionManager.session.value.userId

            val id = recipeId ?: s.draftId
            val createdAt = s.existingCreatedAt ?: now

            val recipe = RecipeEntity(
                id = id,
                ownerId = ownerId,
                title = s.title.trim(),
                description = s.description.trim(),
                cookTimeMin = s.cookTime.toInt(),
                difficulty = s.difficulty.roundToInt().coerceIn(1, 5),
                isPublic = s.isPublic,
                mainImageUrl = s.mainImageUrl,
                createdAt = createdAt,
                updatedAt = now,
                syncStatus = if (recipeId == null) 1 else 2
            )

            val ingredients = s.ingredients.mapIndexedNotNull { index, ui ->
                val name = ui.name.trim()
                if (name.isEmpty()) return@mapIndexedNotNull null

                IngredientEntity(
                    id = ui.id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
                    recipeId = id,
                    name = name,
                    amount = ui.amount.replace(',', '.').toDoubleOrNull(),
                    unit = ui.unit.trim().takeIf { it.isNotBlank() },
                    position = index
                )
            }

            val steps = s.steps.mapIndexedNotNull { index, ui ->
                val text = ui.text.trim()
                if (text.isEmpty()) return@mapIndexedNotNull null

                StepEntity(
                    id = ui.id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
                    recipeId = id,
                    text = text,
                    photoUrl = null,
                    position = index
                )
            }

            runCatching {
                repository.upsertRecipeFull(
                    recipe = recipe,
                    ingredients = ingredients,
                    steps = steps,
                    tagIds = s.selectedTagIds.toList()
                )
            }.onSuccess {
                _state.value = _state.value.copy(saving = false, saved = true)
            }.onFailure {
                _state.value = _state.value.copy(
                    saving = false,
                    error = it.message ?: "Ошибка сохранения"
                )
            }
        }
    }

    fun consumeSaved() {
        _state.value = _state.value.copy(saved = false)
    }
}
