package cdglacier.mytool.ui.screen.positiontracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cdglacier.mytool.data.db.LocationRecordEntity
import cdglacier.mytool.data.repository.LocationPermissionRepository
import cdglacier.mytool.data.repository.TrackingStateRepository
import cdglacier.mytool.domain.usecase.ObserveLocationRecordsByDateUseCase
import cdglacier.mytool.domain.usecase.ToggleLocationTrackingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class PositionTrackingViewModel @Inject constructor(
    private val toggleLocationTrackingUseCase: ToggleLocationTrackingUseCase,
    private val observeLocationRecordsByDateUseCase: ObserveLocationRecordsByDateUseCase,
    private val trackingStateRepository: TrackingStateRepository,
    private val locationPermissionRepository: LocationPermissionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PositionTrackingUiState())
    val uiState: StateFlow<PositionTrackingUiState> = _uiState.asStateFlow()

    private var recordsJob: Job? = null

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
        observeRecords(_uiState.value.date)
    }

    fun refreshPermissions() = locationPermissionRepository.refresh()

    fun onToggleTracking(enabled: Boolean) {
        if (enabled && !_uiState.value.permissionsReady) return
        viewModelScope.launch { toggleLocationTrackingUseCase(enabled) }
    }

    fun onDateChange(delta: Long) {
        val newDate = _uiState.value.date.plusDays(delta)
        _uiState.update { it.copy(date = newDate, points = emptyList()) }
        observeRecords(newDate)
    }

    private fun observeRecords(date: LocalDate) {
        recordsJob?.cancel()
        recordsJob = viewModelScope.launch {
            observeLocationRecordsByDateUseCase(date).collect { list ->
                _uiState.update { it.copy(points = list.map { e -> e.toUiModel() }) }
            }
        }
    }

    private fun LocationRecordEntity.toUiModel() = LocationPointUiModel(
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        batteryLevel = batteryLevel,
        sameLocationCount = sameLocationCount,
    )
}
