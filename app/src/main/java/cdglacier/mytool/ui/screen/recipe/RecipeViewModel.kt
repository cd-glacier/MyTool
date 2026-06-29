package cdglacier.mytool.ui.screen.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.data.repository.Ogp
import cdglacier.mytool.data.repository.OgpRepository
import cdglacier.mytool.data.repository.RecipeRepository
import cdglacier.mytool.domain.usecase.ScanRecipesUseCase
import cdglacier.mytool.ui.component.RecipeItemUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

    private val ogpByUrl = MutableStateFlow<Map<String, Ogp>>(emptyMap())
    private val isScanning = AtomicBoolean(false)

    init {
        viewModelScope.launch {
            combine(recipeRepository.sections, ogpByUrl) { sections, ogpMap ->
                sections.map { section ->
                    RecipeSectionUiModel(
                        date = section.date,
                        items = section.items.map { item ->
                            val ogp = ogpMap[item.url]
                            RecipeItemUiModel(
                                title = item.title,
                                url = item.url,
                                ogpTitle = ogp?.title,
                                ogpImageUrl = ogp?.imageUrl,
                            )
                        },
                    )
                }
            }.collect { ui ->
                _uiState.update { it.copy(sections = ui) }
                launch { fetchMissingOgp(ui) }
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
            } finally {
                _uiState.update { it.copy(isLoading = false) }
                isScanning.set(false)
            }
        }
    }

    private suspend fun fetchMissingOgp(sections: List<RecipeSectionUiModel>) {
        val urls = sections.flatMap { it.items }
            .map { it.url }
            .distinct()
            .filter { it !in ogpByUrl.value }
        for (url in urls) {
            if (url in ogpByUrl.value) continue
            val ogp = runCatching { ogpRepository.fetch(url) }.getOrNull() ?: continue
            ogpByUrl.update { it + (url to ogp) }
        }
    }
}
