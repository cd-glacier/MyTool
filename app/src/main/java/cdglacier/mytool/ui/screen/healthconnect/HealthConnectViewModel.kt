package cdglacier.mytool.ui.screen.healthconnect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cdglacier.mytool.data.repository.HealthPageRepository
import cdglacier.mytool.data.repository.HealthRepository
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.domain.usecase.SyncHealthDayUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HealthConnectViewModel @Inject constructor(
    private val healthRepository: HealthRepository,
    private val healthPageRepository: HealthPageRepository,
    private val syncHealthDayUseCase: SyncHealthDayUseCase,
    private val obsidianRepository: ObsidianRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HealthConnectUiState())
    val uiState: StateFlow<HealthConnectUiState> = _uiState.asStateFlow()

    val healthRequiredPermissions: Set<String> = healthRepository.requiredPermissions

    init {
        _uiState.update { it.copy(healthConnectAvailable = healthRepository.isAvailable) }
        viewModelScope.launch {
            healthRepository.permissionsGranted.collect { g ->
                _uiState.update { it.copy(permissionsGranted = g) }
            }
        }
        viewModelScope.launch {
            obsidianRepository.pagesDirUri.collect { uri ->
                _uiState.update { it.copy(isPagesDirConfigured = uri != null) }
            }
        }
        viewModelScope.launch {
            healthPageRepository.observeBook().collect { book ->
                _uiState.update { it.copy(book = book, isLoading = false) }
            }
        }
    }

    fun refreshPermissions() {
        viewModelScope.launch {
            healthRepository.refreshPermissions()
            _uiState.update { it.copy(healthConnectAvailable = healthRepository.isAvailable) }
        }
    }

    fun onDateChange(delta: Long) {
        val step = if (_uiState.value.viewMode == HealthViewMode.WEEK) delta * 7 else delta
        _uiState.update { it.copy(anchorDate = it.anchorDate.plusDays(step)) }
    }

    fun onViewModeChange(mode: HealthViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun onSyncNow() {
        val state = _uiState.value
        if (state.isSyncing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            val targets = when (state.viewMode) {
                HealthViewMode.DAY -> listOf(state.anchorDate)
                HealthViewMode.WEEK -> HealthConnectUiState.weekDates(state.anchorDate)
            }
            val failures = mutableListOf<Throwable>()
            for (date in targets) {
                val r = syncHealthDayUseCase(date, overwrite = true)
                r.exceptionOrNull()?.let { failures += it }
            }
            _uiState.update {
                it.copy(
                    isSyncing = false,
                    snackbarMessage = if (failures.isEmpty()) "SYNCED"
                    else "ERROR: ${failures.first().message}",
                )
            }
        }
    }

    fun onSnackbarShown() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
