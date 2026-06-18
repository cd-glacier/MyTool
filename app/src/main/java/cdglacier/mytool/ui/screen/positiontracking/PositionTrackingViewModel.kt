package cdglacier.mytool.ui.screen.positiontracking

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cdglacier.mytool.data.repository.TrackingStateRepository
import cdglacier.mytool.domain.usecase.ObserveLocationRecordsByDateUseCase
import cdglacier.mytool.domain.usecase.ToggleLocationTrackingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
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
        observeRecords(_uiState.value.date)
        refreshPermissions()
    }

    fun refreshPermissions() {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val bg = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        _uiState.update { it.copy(foregroundLocationGranted = fine, backgroundLocationGranted = bg) }
    }

    fun onToggleTracking(enabled: Boolean) {
        if (enabled && !_uiState.value.permissionsReady) return
        viewModelScope.launch { toggleLocationTrackingUseCase(enabled) }
    }

    fun onDateChange(delta: Long) {
        val newDate = _uiState.value.date.plusDays(delta)
        _uiState.update { it.copy(date = newDate, records = emptyList()) }
        observeRecords(newDate)
    }

    private fun observeRecords(date: LocalDate) {
        recordsJob?.cancel()
        recordsJob = viewModelScope.launch {
            observeLocationRecordsByDateUseCase(date).collect { list ->
                _uiState.update { it.copy(records = list) }
            }
        }
    }
}
