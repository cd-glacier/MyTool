package cdglacier.mytool.ui.screen.positiontracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cdglacier.mytool.data.db.JournalLocationEntity
import cdglacier.mytool.data.db.LocationRecordEntity
import cdglacier.mytool.data.repository.JournalLocationRepository
import cdglacier.mytool.data.repository.LocationPermissionRepository
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.data.repository.TrackingStateRepository
import cdglacier.mytool.domain.usecase.AutoExportPositionTrackingIfNeededUseCase
import cdglacier.mytool.domain.usecase.ExportPositionTrackingToJournalUseCase
import cdglacier.mytool.domain.usecase.ObserveLocationRecordsByDateUseCase
import cdglacier.mytool.domain.usecase.ScanJournalLocationsUseCase
import cdglacier.mytool.domain.usecase.ToggleLocationTrackingUseCase
import cdglacier.mytool.ui.component.LocationPointUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    private val autoExportPositionTrackingIfNeededUseCase: AutoExportPositionTrackingIfNeededUseCase,
    private val scanJournalLocationsUseCase: ScanJournalLocationsUseCase,
    private val trackingStateRepository: TrackingStateRepository,
    private val locationPermissionRepository: LocationPermissionRepository,
    private val obsidianRepository: ObsidianRepository,
    private val journalLocationRepository: JournalLocationRepository,
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
            trackingStateRepository.mode.collect { mode ->
                _uiState.update { it.copy(trackingMode = mode) }
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
        viewModelScope.launch { scanJournals() }
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

    fun autoExportIfNeeded() {
        val state = _uiState.value
        val dirUri = state.journalDirUri ?: return
        if (state.isExporting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            val result = autoExportPositionTrackingIfNeededUseCase(
                journalDirUri = dirUri.toString(),
                date = state.date,
                filenameFormat = state.filenameFormat,
            )
            _uiState.update {
                it.copy(
                    isExporting = false,
                    snackbarMessage = result.fold(
                        onSuccess = { count ->
                            if (count != null) "JOURNALに自動出力しました ($count 件)" else null
                        },
                        onFailure = { e -> "自動出力エラー: ${e.message}" },
                    ),
                )
            }
        }
    }

    fun onToggleTracking(enabled: Boolean) {
        if (enabled && !_uiState.value.permissionsReady) return
        viewModelScope.launch { toggleLocationTrackingUseCase(enabled) }
    }

    fun onDateChange(delta: Long) {
        val newDate = _uiState.value.date.plusDays(delta)
        isFollowingToday = newDate == LocalDate.now()
        _uiState.update { it.copy(date = newDate, points = emptyList()) }
        observeRecords(newDate)
        autoExportIfNeeded()
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
            combine(
                observeLocationRecordsByDateUseCase(date),
                journalLocationRepository.observeByDate(date),
            ) { dbRecords, journalRecords ->
                mergePoints(dbRecords, journalRecords)
            }.collect { points ->
                _uiState.update { it.copy(points = points) }
            }
        }
    }

    private fun mergePoints(
        dbRecords: List<LocationRecordEntity>,
        journalRecords: List<JournalLocationEntity>,
    ): List<LocationPointUiModel> {
        val dbTimestamps = dbRecords.map { it.timestamp }.toHashSet()
        val merged = dbRecords.map { it.toUiModel() } +
            journalRecords.filter { it.timestamp !in dbTimestamps }.map { it.toUiModel() }
        return merged.sortedBy { it.timestampMillis }
    }

    private suspend fun scanJournals() {
        val uri = obsidianRepository.journalDirUri.first()?.toString() ?: return
        val format = obsidianRepository.filenameFormat.first()
        runCatching { scanJournalLocationsUseCase(uri, format) }
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
        timestampMillis = timestamp,
    )

    private fun JournalLocationEntity.toUiModel() = LocationPointUiModel(
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        batteryLevel = batteryLevel,
        sameLocationCount = sameLocationCount,
        timestampMillis = timestamp,
    )
}
