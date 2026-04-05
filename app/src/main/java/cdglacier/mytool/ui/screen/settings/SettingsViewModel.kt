package cdglacier.mytool.ui.screen.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.widget.CalendarWidgetUpdateWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val obsidianRepository: ObsidianRepository,
    @ApplicationContext private val context: Context,
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
        refreshCalendarPermission()
    }

    fun refreshCalendarPermission() {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        _uiState.update { it.copy(calendarPermissionGranted = granted) }
    }

    fun onCalendarPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(calendarPermissionGranted = granted) }
        if (granted) CalendarWidgetUpdateWorker.runOnce(context)
    }

    fun onVaultUriPicked(uri: Uri) {
        viewModelScope.launch { obsidianRepository.setVaultUri(uri) }
    }

    fun onJournalDirPicked(uri: Uri) {
        viewModelScope.launch { obsidianRepository.setJournalDirUri(uri) }
    }

    fun onFilenameFormatChange(format: String) {
        viewModelScope.launch { obsidianRepository.setFilenameFormat(format) }
    }
}
