package cdglacier.mytool.ui.screen.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.data.repository.OgpRepository
import cdglacier.mytool.domain.usecase.GetRecentRecipesUseCase
import cdglacier.mytool.ui.component.RecipeItemUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecipeViewModel @Inject constructor(
    private val obsidianRepository: ObsidianRepository,
    private val getRecentRecipesUseCase: GetRecentRecipesUseCase,
    private val ogpRepository: OgpRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeUiState())
    val uiState: StateFlow<RecipeUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val uri = obsidianRepository.journalDirUri.first()
            if (uri == null) {
                _uiState.update { it.copy(isLoading = false, sections = emptyList()) }
                return@launch
            }
            val format = obsidianRepository.filenameFormat.first()
            val grouped = getRecentRecipesUseCase(uri.toString(), format)
            val sections = grouped.map { group ->
                RecipeSectionUiModel(
                    date = group.date,
                    items = group.recipes.map { RecipeItemUiModel(title = it.title, url = it.url) },
                )
            }
            _uiState.update { it.copy(isLoading = false, sections = sections) }
            fetchOgpForAll(sections)
        }
    }

    private fun fetchOgpForAll(sections: List<RecipeSectionUiModel>) {
        sections.forEachIndexed { sectionIndex, section ->
            section.items.forEachIndexed { itemIndex, item ->
                viewModelScope.launch {
                    val ogp = ogpRepository.fetch(item.url) ?: return@launch
                    _uiState.update { state ->
                        val newSections = state.sections.toMutableList()
                        val target = newSections.getOrNull(sectionIndex) ?: return@update state
                        val newItems = target.items.toMutableList()
                        val current = newItems.getOrNull(itemIndex) ?: return@update state
                        if (current.url != item.url) return@update state
                        newItems[itemIndex] = current.copy(
                            ogpImageUrl = ogp.imageUrl,
                            ogpTitle = ogp.title,
                        )
                        newSections[sectionIndex] = target.copy(items = newItems)
                        state.copy(sections = newSections)
                    }
                }
            }
        }
    }
}
