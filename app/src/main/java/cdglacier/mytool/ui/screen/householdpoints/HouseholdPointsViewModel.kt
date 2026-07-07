package cdglacier.mytool.ui.screen.householdpoints

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.domain.model.HouseholdPoint
import cdglacier.mytool.domain.usecase.GetHouseholdPointsUseCase
import cdglacier.mytool.domain.usecase.SaveHouseholdPointsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HouseholdPointsViewModel @Inject constructor(
    private val obsidianRepository: ObsidianRepository,
    private val getHouseholdPointsUseCase: GetHouseholdPointsUseCase,
    private val saveHouseholdPointsUseCase: SaveHouseholdPointsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HouseholdPointsUiState())
    val uiState: StateFlow<HouseholdPointsUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val pagesConfigured = obsidianRepository.pagesDirUri.first() != null
            val points = if (pagesConfigured) getHouseholdPointsUseCase() else emptyList()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    pagesConfigured = pagesConfigured,
                    points = points,
                )
            }
        }
    }

    fun onNewNameChange(name: String) {
        _uiState.update { it.copy(newName = name) }
    }

    fun onNewValueChange(text: String) {
        _uiState.update { it.copy(newPointsText = text) }
    }

    fun onAddPoint() {
        val state = _uiState.value
        val name = state.newName.trim()
        val points = state.newPointsText.toIntOrNull()
        if (name.isEmpty() || points == null) {
            _uiState.update { it.copy(errorMessage = "家事名とポイントを入力してください") }
            return
        }
        if (state.points.any { it.name == name }) {
            _uiState.update { it.copy(errorMessage = "同名の家事が既に存在します") }
            return
        }
        val updated = state.points + HouseholdPoint(name, points)
        persist(updated) {
            _uiState.update { it.copy(newName = "", newPointsText = "") }
        }
    }

    fun onRequestDelete(name: String) {
        _uiState.update { it.copy(pendingDeleteName = name) }
    }

    fun onCancelDelete() {
        _uiState.update { it.copy(pendingDeleteName = null) }
    }

    fun onConfirmDelete() {
        val name = _uiState.value.pendingDeleteName ?: return
        val updated = _uiState.value.points.filterNot { it.name == name }
        _uiState.update { it.copy(pendingDeleteName = null) }
        persist(updated) {}
    }

    fun onRequestEdit(name: String) {
        val target = _uiState.value.points.firstOrNull { it.name == name } ?: return
        _uiState.update {
            it.copy(editingName = target.name, editingPointsText = target.points.toString())
        }
    }

    fun onEditValueChange(text: String) {
        _uiState.update { it.copy(editingPointsText = text) }
    }

    fun onCancelEdit() {
        _uiState.update { it.copy(editingName = null, editingPointsText = "") }
    }

    fun onConfirmEdit() {
        val state = _uiState.value
        val name = state.editingName ?: return
        val points = state.editingPointsText.toIntOrNull()
        if (points == null) {
            _uiState.update { it.copy(errorMessage = "ポイントを入力してください") }
            return
        }
        val updated = state.points.map {
            if (it.name == name) HouseholdPoint(it.name, points) else it
        }
        _uiState.update { it.copy(editingName = null, editingPointsText = "") }
        persist(updated) {}
    }

    private fun persist(updated: List<HouseholdPoint>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = saveHouseholdPointsUseCase(updated)
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
            } else {
                _uiState.update { it.copy(points = updated) }
                onSuccess()
            }
        }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
