package cdglacier.mytool.ui.screen.copyjournal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.domain.usecase.CheckJournalTargetUseCase
import cdglacier.mytool.domain.usecase.CopyJournalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class CopyObsidianJournalViewModel @Inject constructor(
    private val obsidianRepository: ObsidianRepository,
    private val copyJournalUseCase: CopyJournalUseCase,
    private val checkJournalTargetUseCase: CheckJournalTargetUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CopyObsidianJournalUiState())
    val uiState: StateFlow<CopyObsidianJournalUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            obsidianRepository.journalDirUri.collect { uri ->
                _uiState.update { it.copy(journalDirUri = uri) }
            }
        }
        viewModelScope.launch {
            obsidianRepository.filenameFormat.collect { fmt ->
                _uiState.update { it.copy(filenameFormat = fmt) }
            }
        }
    }

    fun onSourceDateChange(date: LocalDate) {
        _uiState.update { it.copy(sourceDate = date) }
    }

    fun onTargetDateChange(date: LocalDate) {
        _uiState.update { it.copy(targetDate = date) }
    }

    fun onCopy() {
        val state = _uiState.value
        val dirUri = state.journalDirUri ?: return
        viewModelScope.launch {
            val hasContent = checkJournalTargetUseCase(dirUri, state.targetDate, state.filenameFormat)
            if (hasContent) {
                _uiState.update { it.copy(showOverwriteConfirmation = true) }
            } else {
                executeCopy()
            }
        }
    }

    fun onOverwriteConfirmed() {
        _uiState.update { it.copy(showOverwriteConfirmation = false) }
        executeCopy()
    }

    fun onOverwriteCancelled() {
        _uiState.update { it.copy(showOverwriteConfirmation = false) }
    }

    private fun executeCopy() {
        val state = _uiState.value
        val dirUri = state.journalDirUri ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCopying = true) }
            val startTime = System.currentTimeMillis()
            val result = copyJournalUseCase(
                journalDirUri = dirUri,
                sourceDate = state.sourceDate,
                targetDate = state.targetDate,
                filenameFormat = state.filenameFormat,
            )
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < 2000) delay(2000 - elapsed)
            _uiState.update {
                it.copy(
                    isCopying = false,
                    snackbarMessage = result.fold(
                        onSuccess = { "コピーしました" },
                        onFailure = { e -> "エラー: ${e.message}" }
                    )
                )
            }
        }
    }

    fun onSnackbarShown() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
