package cdglacier.mytool.ui.screen.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.data.repository.OgpRepository
import cdglacier.mytool.data.repository.RecipeRepository
import cdglacier.mytool.domain.usecase.ScanRecipesUseCase
import cdglacier.mytool.ui.component.RecipeItemUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class RecipeViewModel @Inject constructor(
    private val obsidianRepository: ObsidianRepository,
    private val recipeRepository: RecipeRepository,
    private val scanRecipesUseCase: ScanRecipesUseCase,
    private val ogpRepository: OgpRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeUiState())
    val uiState: StateFlow<RecipeUiState> = _uiState.asStateFlow()

    private val isScanning = AtomicBoolean(false)

    init {
        viewModelScope.launch {
            recipeRepository.sections.collect { sections ->
                val ui = sections.map { section ->
                    RecipeSectionUiModel(
                        date = section.date,
                        items = section.items.map { item ->
                            RecipeItemUiModel(
                                title = item.title,
                                url = item.url,
                                ogpTitle = item.ogpTitle,
                                ogpImageUrl = item.ogpImageUrl,
                            )
                        },
                    )
                }
                _uiState.update { it.copy(sections = ui) }
            }
        }
    }

    fun refresh() {
        if (!isScanning.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val uri = obsidianRepository.journalDirUri.first() ?: return@launch
                val format = obsidianRepository.filenameFormat.first()
                scanRecipesUseCase(uri.toString(), format)
                fetchMissingOgp()
            } finally {
                _uiState.update { it.copy(isLoading = false) }
                isScanning.set(false)
            }
        }
    }

    private suspend fun fetchMissingOgp() {
        val sections = recipeRepository.sections.first()
        for (section in sections) {
            for (item in section.items) {
                if (item.ogpImageUrl != null || item.ogpTitle != null) continue
                val ogp = runCatching { ogpRepository.fetch(item.url) }.getOrNull() ?: continue
                if (ogp.imageUrl == null && ogp.title == null) continue
                runCatching {
                    recipeRepository.updateOgp(section.date, item.url, ogp.title, ogp.imageUrl)
                }
            }
        }
    }
}
