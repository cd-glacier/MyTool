package cdglacier.mytool.ui.screen.healthconnect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cdglacier.mytool.data.repository.HealthRepository
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.domain.usecase.GetHealthDataForDateUseCase
import cdglacier.mytool.domain.usecase.HealthData
import cdglacier.mytool.domain.usecase.WriteHealthSectionToJournalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HealthConnectViewModel @Inject constructor(
    private val getHealthDataForDateUseCase: GetHealthDataForDateUseCase,
    private val writeHealthSectionToJournalUseCase: WriteHealthSectionToJournalUseCase,
    private val healthRepository: HealthRepository,
    private val obsidianRepository: ObsidianRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HealthConnectUiState())
    val uiState: StateFlow<HealthConnectUiState> = _uiState.asStateFlow()

    val healthRequiredPermissions: Set<String> = healthRepository.requiredPermissions

    init {
        _uiState.update { it.copy(healthConnectAvailable = healthRepository.isAvailable) }
        viewModelScope.launch {
            healthRepository.permissionsGranted.collect { granted ->
                _uiState.update { it.copy(permissionsGranted = granted) }
            }
        }
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

    fun refresh() {
        viewModelScope.launch {
            healthRepository.refreshPermissions()
            _uiState.update {
                it.copy(
                    isLoading = true,
                    healthConnectAvailable = healthRepository.isAvailable,
                )
            }
            val data = runCatching { getHealthDataForDateUseCase(_uiState.value.date) }
                .getOrDefault(HealthData())
            _uiState.update {
                it.copy(
                    isLoading = false,
                    steps = data.steps,
                    sleep = data.sleep,
                )
            }
        }
    }

    fun onDateChange(delta: Long) {
        val newDate = _uiState.value.date.plusDays(delta)
        _uiState.update { it.copy(date = newDate, steps = null, sleep = null) }
        refresh()
    }

    fun onWriteToJournal() {
        val state = _uiState.value
        val dirUri = state.journalDirUri ?: return
        if (!state.canWrite) return
        viewModelScope.launch {
            _uiState.update { it.copy(isWriting = true) }
            val result = writeHealthSectionToJournalUseCase(
                journalDirUri = dirUri.toString(),
                date = state.date,
                filenameFormat = state.filenameFormat,
                health = HealthData(sleep = state.sleep, steps = state.steps),
            )
            _uiState.update {
                it.copy(
                    isWriting = false,
                    snackbarMessage = result.fold(
                        onSuccess = { "JOURNALに出力しました" },
                        onFailure = { e -> "エラー: ${e.message}" },
                    ),
                )
            }
        }
    }

    fun onSnackbarShown() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
