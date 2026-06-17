package cdglacier.mytool.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.domain.usecase.GetHabitCompletionRatesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val obsidianRepository: ObsidianRepository,
    private val getHabitCompletionRatesUseCase: GetHabitCompletionRatesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val uri = obsidianRepository.journalDirUri.first()?.toString()
            val format = obsidianRepository.filenameFormat.first()
            loadRates(uri, format)
        }
    }

    private suspend fun loadRates(journalDirUri: String?, filenameFormat: String) {
        _uiState.update {
            it.copy(
                journalDirUri = journalDirUri?.let(android.net.Uri::parse),
                isLoading = journalDirUri != null,
            )
        }
        if (journalDirUri == null) return
        val today = LocalDate.now()
        val dates = (0 until 182).map { daysAgo -> today.minusDays(daysAgo.toLong()) }
        val rates = runCatching {
            getHabitCompletionRatesUseCase(journalDirUri, filenameFormat, dates)
        }.getOrDefault(emptyMap())
        _uiState.update { it.copy(habitCompletionRates = rates, isLoading = false) }
    }
}
