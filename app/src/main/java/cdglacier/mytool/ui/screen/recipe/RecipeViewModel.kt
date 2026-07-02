package cdglacier.mytool.ui.screen.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cdglacier.mytool.data.repository.EmbeddingProgress
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.data.repository.Ogp
import cdglacier.mytool.data.repository.OgpRepository
import cdglacier.mytool.data.repository.RecipeEmbeddingRepository
import cdglacier.mytool.data.repository.RecipeRepository
import cdglacier.mytool.domain.usecase.AddRecipeToJournalUseCase
import cdglacier.mytool.domain.usecase.ScanRecipesUseCase
import cdglacier.mytool.domain.usecase.SearchRecipesUseCase
import cdglacier.mytool.ui.component.RecipeItemUiModel
import cdglacier.mytool.worker.RecipeEmbeddingScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class RecipeViewModel @Inject constructor(
    private val obsidianRepository: ObsidianRepository,
    private val recipeRepository: RecipeRepository,
    private val scanRecipesUseCase: ScanRecipesUseCase,
    private val ogpRepository: OgpRepository,
    private val addRecipeToJournalUseCase: AddRecipeToJournalUseCase,
    private val searchRecipesUseCase: SearchRecipesUseCase,
    private val embeddingRepository: RecipeEmbeddingRepository,
    private val recipeEmbeddingScheduler: RecipeEmbeddingScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeUiState())
    val uiState: StateFlow<RecipeUiState> = _uiState.asStateFlow()

    private val ogpByUrl = MutableStateFlow<Map<String, Ogp>>(emptyMap())
    private val searchQueryFlow = MutableStateFlow("")
    private val isScanning = AtomicBoolean(false)
    private var searchJob: Job? = null

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
                if (_uiState.value.searchQuery.isNotBlank()) runSearch(_uiState.value.searchQuery)
            }
        }
        viewModelScope.launch {
            embeddingRepository.progress.collect { progress ->
                _uiState.update { it.copy(embeddingProgress = progress.toUiModel()) }
            }
        }
        viewModelScope.launch {
            searchQueryFlow.drop(1).debounce(250).collect { query ->
                runSearch(query)
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

    fun ensureEmbeddingsGenerated() {
        recipeEmbeddingScheduler.schedule()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchQueryFlow.value = query
        if (query.isBlank()) {
            searchJob?.cancel()
            _uiState.update { it.copy(isSearching = false, searchResults = emptyList()) }
        }
    }

    private fun runSearch(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(isSearching = false, searchResults = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            val urls = runCatching { searchRecipesUseCase(query) }.getOrDefault(emptyList())
            val allItems = _uiState.value.sections
                .flatMap { it.items }
                .associateBy { it.url }
            val ordered = urls.mapNotNull { allItems[it] }
            _uiState.update { it.copy(isSearching = false, searchResults = ordered) }
        }
    }

    fun onAddUrlChange(url: String) {
        _uiState.update { it.copy(addForm = it.addForm.copy(url = url, error = null)) }
    }

    fun onAddTitleChange(title: String) {
        _uiState.update { it.copy(addForm = it.addForm.copy(title = title, error = null)) }
    }

    fun setPrefilledUrl(url: String) {
        if (url.isBlank()) return
        if (_uiState.value.addForm.url == url) return
        _uiState.update { it.copy(addForm = AddRecipeFormUiModel(url = url)) }
        fetchAddTitle()
    }

    fun fetchAddTitle() {
        val url = _uiState.value.addForm.url.trim()
        if (url.isBlank()) {
            _uiState.update { it.copy(addForm = it.addForm.copy(error = "URLを入力してください")) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(addForm = it.addForm.copy(isFetching = true, error = null)) }
            val ogp = runCatching { ogpRepository.fetch(url) }.getOrNull()
            val title = ogp?.title?.takeIf { it.isNotBlank() }
            if (ogp != null) ogpByUrl.update { it + (url to ogp) }
            _uiState.update {
                it.copy(
                    addForm = it.addForm.copy(
                        isFetching = false,
                        title = title ?: "",
                        error = if (title == null) "タイトルを取得できませんでした" else null,
                    )
                )
            }
        }
    }

    fun submitAdd() {
        val form = _uiState.value.addForm
        val url = form.url.trim()
        val title = form.title?.trim().orEmpty()
        if (url.isBlank() || title.isBlank()) {
            _uiState.update { it.copy(addForm = it.addForm.copy(error = "URLとタイトルを入力してください")) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(addForm = it.addForm.copy(isSubmitting = true, error = null)) }
            val uri = obsidianRepository.journalDirUri.first()
            if (uri == null) {
                _uiState.update { it.copy(addForm = it.addForm.copy(isSubmitting = false, error = "Vault未設定")) }
                return@launch
            }
            val format = obsidianRepository.filenameFormat.first()
            val result = addRecipeToJournalUseCase(
                journalDirUri = uri.toString(),
                date = LocalDate.now(),
                filenameFormat = format,
                title = title,
                url = url,
            )
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(addForm = AddRecipeFormUiModel()) }
                    refresh()
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(addForm = it.addForm.copy(isSubmitting = false, error = e.message ?: "追加失敗"))
                    }
                },
            )
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

    private fun EmbeddingProgress.toUiModel(): EmbeddingProgressUiModel? =
        if (isRunning) EmbeddingProgressUiModel(processed = processed, total = total) else null
}
