package cdglacier.mytool.ui.screen.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cdglacier.mytool.data.repository.AiRepository
import cdglacier.mytool.data.repository.CalendarPermissionRepository
import cdglacier.mytool.data.repository.LocationPermissionRepository
import cdglacier.mytool.data.repository.ObsidianRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val obsidianRepository: ObsidianRepository,
    private val locationPermissionRepository: LocationPermissionRepository,
    private val calendarPermissionRepository: CalendarPermissionRepository,
    private val aiRepository: AiRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            obsidianRepository.vaultUri.collect { uri ->
                _uiState.update { it.copy(vaultUri = uri) }
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
        viewModelScope.launch {
            obsidianRepository.pagesDirUri.collect { uri ->
                _uiState.update { it.copy(pagesDirUri = uri) }
            }
        }
        viewModelScope.launch {
            calendarPermissionRepository.calendarGranted.collect { granted ->
                _uiState.update { it.copy(calendarPermissionGranted = granted) }
            }
        }
        viewModelScope.launch {
            locationPermissionRepository.fineLocationGranted.collect { granted ->
                _uiState.update { it.copy(fineLocationGranted = granted) }
            }
        }
        viewModelScope.launch {
            locationPermissionRepository.backgroundLocationGranted.collect { granted ->
                _uiState.update { it.copy(backgroundLocationGranted = granted) }
            }
        }
        viewModelScope.launch {
            aiRepository.availability.collect { availability ->
                _uiState.update { it.copy(aiAvailability = availability) }
            }
        }
    }

    fun refreshPermissions() {
        calendarPermissionRepository.refresh()
        locationPermissionRepository.refresh()
    }

    fun refreshAiAvailability() {
        viewModelScope.launch { aiRepository.refreshAvailability() }
    }

    fun downloadAiModel() {
        viewModelScope.launch { aiRepository.downloadModel() }
    }

    fun onVaultUriPicked(uri: Uri) {
        viewModelScope.launch { obsidianRepository.setVaultUri(uri) }
    }

    fun onJournalDirPicked(uri: Uri) {
        viewModelScope.launch { obsidianRepository.setJournalDirUri(uri) }
    }

    fun onPagesDirPicked(uri: Uri) {
        viewModelScope.launch { obsidianRepository.setPagesDirUri(uri) }
    }

    fun onFilenameFormatChange(format: String) {
        viewModelScope.launch { obsidianRepository.setFilenameFormat(format) }
    }
}
