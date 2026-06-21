package cdglacier.mytool.ui.screen.positiontracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cdglacier.mytool.data.db.LocationRecordEntity
import cdglacier.mytool.data.repository.LocationPermissionRepository
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.data.repository.TrackingStateRepository
import cdglacier.mytool.domain.usecase.ExportPositionTrackingToJournalUseCase
import cdglacier.mytool.domain.usecase.ObserveLocationRecordsByDateUseCase
import cdglacier.mytool.domain.usecase.ToggleLocationTrackingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class PositionTrackingViewModel @Inject constructor(
    private val toggleLocationTrackingUseCase: ToggleLocationTrackingUseCase,
    private val observeLocationRecordsByDateUseCase: ObserveLocationRecordsByDateUseCase,
    private val exportPositionTrackingToJournalUseCase: ExportPositionTrackingToJournalUseCase,
    private val trackingStateRepository: TrackingStateRepository,
    private val locationPermissionRepository: LocationPermissionRepository,
    private val obsidianRepository: ObsidianRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PositionTrackingUiState())
    val uiState: StateFlow<PositionTrackingUiState> = _uiState.asStateFlow()

    private var recordsJob: Job? = null
    private var isFollowingToday: Boolean = true

    init {
        viewModelScope.launch {
            trackingStateRepository.trackingEnabled.collect { enabled ->
                _uiState.update { it.copy(trackingEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            locationPermissionRepository.fineLocationGranted.collect { granted ->
                _uiState.update { it.copy(foregroundLocationGranted = granted) }
            }
        }
        viewModelScope.launch {
            locationPermissionRepository.backgroundLocationGranted.collect { granted ->
                _uiState.update { it.copy(backgroundLocationGranted = granted) }
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
        observeRecords(_uiState.value.date)
        viewModelScope.launch {
            while (true) {
                delay(DATE_CHECK_INTERVAL_MS)
                if (!isFollowingToday) continue
                val today = LocalDate.now()
                if (today != _uiState.value.date) {
                    _uiState.update { it.copy(date = today, points = emptyList()) }
                    observeRecords(today)
                }
            }
        }
    }

    fun refreshPermissions() = locationPermissionRepository.refresh()

    fun onToggleTracking(enabled: Boolean) {
        if (enabled && !_uiState.value.permissionsReady) return
        viewModelScope.launch { toggleLocationTrackingUseCase(enabled) }
    }

    fun onDateChange(delta: Long) {
        val newDate = _uiState.value.date.plusDays(delta)
        isFollowingToday = newDate == LocalDate.now()
        _uiState.update { it.copy(date = newDate, points = emptyList()) }
        observeRecords(newDate)
    }

    fun onExportToJournal() {
        val state = _uiState.value
        val dirUri = state.journalDirUri ?: return
        if (state.isExporting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            val result = exportPositionTrackingToJournalUseCase(
                journalDirUri = dirUri.toString(),
                date = state.date,
                filenameFormat = state.filenameFormat,
            )
            _uiState.update {
                it.copy(
                    isExporting = false,
                    snackbarMessage = result.fold(
                        onSuccess = { count -> "JOURNALに出力しました ($count 件)" },
                        onFailure = { e -> "エラー: ${e.message}" },
                    ),
                )
            }
        }
    }

    fun onSnackbarShown() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun observeRecords(date: LocalDate) {
        recordsJob?.cancel()
        recordsJob = viewModelScope.launch {
            observeLocationRecordsByDateUseCase(date).collect { list ->
                _uiState.update { it.copy(points = list.map { e -> e.toUiModel() }) }
            }
        }
    }

    companion object {
        private const val DATE_CHECK_INTERVAL_MS = 60_000L
    }

    private fun LocationRecordEntity.toUiModel() = LocationPointUiModel(
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        batteryLevel = batteryLevel,
        sameLocationCount = sameLocationCount,
    )
}
