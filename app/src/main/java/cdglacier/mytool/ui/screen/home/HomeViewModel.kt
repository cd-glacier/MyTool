package cdglacier.mytool.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.domain.usecase.GetJournalLineCountsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val obsidianRepository: ObsidianRepository,
    private val getJournalLineCountsUseCase: GetJournalLineCountsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                obsidianRepository.journalDirUri,
                obsidianRepository.filenameFormat,
            ) { uri, format -> Pair(uri, format) }
                .collect { (uri, format) ->
                    _uiState.update { it.copy(journalDirUri = uri, isLoading = uri != null) }
                    if (uri != null) {
                        val today = LocalDate.now()
                        val dates = (0 until 112).map { daysAgo -> today.minusDays(daysAgo.toLong()) }
                        val counts = getJournalLineCountsUseCase(uri, format, dates)
                        _uiState.update { it.copy(journalLineCounts = counts, isLoading = false) }
                    }
                }
        }
    }
}
