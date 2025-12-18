package com.janerli.delishhub.feature.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.janerli.delishhub.core.session.SessionManager
import com.janerli.delishhub.data.local.entity.IngredientEntity
import com.janerli.delishhub.data.local.entity.RecipeEntity
import com.janerli.delishhub.data.local.entity.StepEntity
import com.janerli.delishhub.data.local.model.RecipeFull
import com.janerli.delishhub.domain.repository.RecipeRepository
import com.janerli.delishhub.feature.recipes.ui.IngredientUi
import com.janerli.delishhub.feature.recipes.ui.StepUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

class RecipeEditorViewModel(
    private val repository: RecipeRepository,
    private val recipeId: String?
) : ViewModel() {

    data class UiState(
        val loading: Boolean = false,
        val saving: Boolean = false,
        val saved: Boolean = false,
        val error: String? = null,

        val title: String = "",
        val description: String = "",
        val cookTime: String = "20",
        val difficulty: Float = 2f,

        val titleError: String? = null,

        val ingredients: List<IngredientUi> = listOf(IngredientUi(id = "i1", name = "", amount = "", unit = "")),
        val steps: List<StepUi> = listOf(StepUi(id = "s1", text = "")),

        // для корректного updatedAt/createdAt при редактировании
        val existingCreatedAt: Long? = null
    )

    private val _state = MutableStateFlow(UiState(loading = recipeId != null))
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        if (recipeId != null) {
            viewModelScope.launch {
                runCatching {
                    repository.getRecipeFull(recipeId)
                }.onSuccess { full ->
                    if (full == null) {
                        _state.value = _state.value.copy(
                            loading = false,
                            error = "Рецепт не найден"
                        )
                    } else {
                        applyFullToState(full)
                    }
                }.onFailure {
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
            existingCreatedAt = r.createdAt,
            ingredients = full.ingredients
                .sortedBy { it.position }
                .map {
                    IngredientUi(
                        id = it.id,
                        name = it.name,
                        amount = it.amount?.let { a ->
                            val isInt = kotlin.math.abs(a - a.toInt()) < 1e-9
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

    fun setTitle(v: String) {
        _state.value = _state.value.copy(title = v, titleError = null)
    }

    fun setDescription(v: String) {
        _state.value = _state.value.copy(description = v)
    }

    fun setCookTimeDigitsOnly(v: String) {
        _state.value = _state.value.copy(cookTime = v.filter { it.isDigit() }.take(3))
    }

    fun setDifficulty(v: Float) {
        _state.value = _state.value.copy(difficulty = v)
    }

    fun addIngredient() {
        val list = _state.value.ingredients.toMutableList()
        list.add(
            IngredientUi(
                id = "i${System.currentTimeMillis()}",
                name = "",
                amount = "",
                unit = ""
            )
        )
        _state.value = _state.value.copy(ingredients = list)
    }

    fun updateIngredient(updated: IngredientUi) {
        val list = _state.value.ingredients.toMutableList()
        val idx = list.indexOfFirst { it.id == updated.id }
        if (idx >= 0) list[idx] = updated
        _state.value = _state.value.copy(ingredients = list)
    }

    fun removeIngredient(id: String) {
        val list = _state.value.ingredients.toMutableList()
        if (list.size > 1) {
            list.removeAll { it.id == id }
            _state.value = _state.value.copy(ingredients = list)
        } else {
            // оставить 1 строку, просто очистить
            _state.value = _state.value.copy(
                ingredients = list.map { if (it.id == id) it.copy(name = "", amount = "", unit = "") else it }
            )
        }
    }

    fun addStep() {
        val list = _state.value.steps.toMutableList()
        list.add(StepUi(id = "s${System.currentTimeMillis()}", text = ""))
        _state.value = _state.value.copy(steps = list)
    }

    fun updateStep(updated: StepUi) {
        val list = _state.value.steps.toMutableList()
        val idx = list.indexOfFirst { it.id == updated.id }
        if (idx >= 0) list[idx] = updated
        _state.value = _state.value.copy(steps = list)
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

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun consumeSaved() {
        _state.value = _state.value.copy(saved = false)
    }

    fun save() {
        val s = _state.value
        val trimmedTitle = s.title.trim()
        if (trimmedTitle.isEmpty()) {
            _state.value = s.copy(titleError = "Введите название рецепта")
            return
        }

        val cookTimeMin = s.cookTime.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val difficultyInt = s.difficulty.roundToInt().coerceIn(1, 5)

        _state.value = s.copy(saving = true, error = null, titleError = null)

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val ownerId = SessionManager.session.value.userId

            val id = recipeId ?: UUID.randomUUID().toString()
            val createdAt = s.existingCreatedAt ?: now

            // syncStatus: created/updated (для будущего Firestore)
            val syncStatus = if (recipeId == null) 1 else 2

            val recipe = RecipeEntity(
                id = id,
                ownerId = ownerId,
                title = trimmedTitle,
                description = s.description.trim(),
                cookTimeMin = cookTimeMin,
                difficulty = difficultyInt,
                createdAt = createdAt,
                updatedAt = now,
                syncStatus = syncStatus
            )

            val ingredients = s.ingredients
                .mapIndexedNotNull { index, ui ->
                    val name = ui.name.trim()
                    if (name.isEmpty()) return@mapIndexedNotNull null

                    val amount = ui.amount.trim().replace(',', '.').toDoubleOrNull()
                    val unit = ui.unit.trim().takeIf { it.isNotBlank() }

                    IngredientEntity(
                        id = ui.id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
                        recipeId = id,
                        name = name,
                        amount = amount,
                        unit = unit,
                        position = index
                    )
                }

            val steps = s.steps
                .mapIndexedNotNull { index, ui ->
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
                    tagIds = emptyList()
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
}
