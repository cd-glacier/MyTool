package cdglacier.mytool.ui.screen.healthconnect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cdglacier.mytool.data.repository.HealthPageRepository
import cdglacier.mytool.data.repository.HealthRepository
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.domain.model.HealthBook
import cdglacier.mytool.domain.usecase.SyncHealthDayUseCase
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
    private val healthRepository: HealthRepository,
    healthPageRepository: HealthPageRepository,
    private val syncHealthDayUseCase: SyncHealthDayUseCase,
    obsidianRepository: ObsidianRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HealthConnectUiState())
    val uiState: StateFlow<HealthConnectUiState> = _uiState.asStateFlow()

    private var book: HealthBook = HealthBook()

    val healthRequiredPermissions: Set<String> = healthRepository.requiredPermissions

    init {
        _uiState.update { it.copy(healthConnectAvailable = healthRepository.isAvailable) }
        recomputeDerived()
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
            healthPageRepository.observeBook().collect { b ->
                book = b
                _uiState.update { it.copy(isLoading = false) }
                recomputeDerived()
            }
        }
    }

    fun refreshPermissions() {
        viewModelScope.launch {
            healthRepository.refreshPermissions()
            _uiState.update { it.copy(healthConnectAvailable = healthRepository.isAvailable) }
            autoSync()
        }
    }

    fun onDateChange(delta: Long) {
        val step = if (_uiState.value.viewMode == HealthViewMode.WEEK) delta * 7 else delta
        val canSync = _uiState.value.healthConnectAvailable &&
            healthRepository.permissionsGranted.value &&
            _uiState.value.backfillProgress == null
        _uiState.update {
            it.copy(
                anchorDate = it.anchorDate.plusDays(step),
                isSyncing = it.isSyncing || canSync,
            )
        }
        recomputeDerived()
        viewModelScope.launch { autoSync() }
    }

    fun onViewModeChange(mode: HealthViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
        recomputeDerived()
    }

    fun onBackfill(days: Int) {
        val state = _uiState.value
        if (state.isSyncing || state.backfillProgress != null) return
        if (!state.healthConnectAvailable || !state.permissionsGranted) return
        viewModelScope.launch {
            val today = LocalDate.now()
            val targets = (1..days).map { today.minusDays(it.toLong()) }
            _uiState.update { it.copy(backfillProgress = BackfillProgress(0, targets.size)) }
            val failures = mutableListOf<Throwable>()
            targets.forEachIndexed { i, date ->
                val r = syncHealthDayUseCase(date, overwrite = false)
                r.exceptionOrNull()?.let { failures += it }
                _uiState.update { it.copy(backfillProgress = BackfillProgress(i + 1, targets.size)) }
            }
            _uiState.update {
                it.copy(
                    backfillProgress = null,
                    snackbarMessage = if (failures.isEmpty()) "BACKFILLED ${targets.size}d"
                    else "ERROR: ${failures.first().message}",
                )
            }
        }
    }

    fun onSnackbarShown() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private suspend fun autoSync() {
        val state = _uiState.value
        if (state.isSyncing || state.backfillProgress != null) return
        if (!state.healthConnectAvailable || !healthRepository.permissionsGranted.value) return
        _uiState.update { it.copy(isSyncing = true) }
        val targets = when (state.viewMode) {
            HealthViewMode.DAY -> listOf(state.anchorDate)
            HealthViewMode.WEEK -> weekDates(state.anchorDate)
        }
        for (date in targets) syncHealthDayUseCase(date, overwrite = false)
        _uiState.update { it.copy(isSyncing = false) }
    }

    private fun recomputeDerived() {
        val anchor = _uiState.value.anchorDate
        val week = weekDates(anchor)
        _uiState.update {
            it.copy(
                currentDay = book.dayOrEmpty(anchor),
                currentWeek = week.map(book::dayOrEmpty),
                weekLabel = "${week.first()} ~ ${week.last()}",
            )
        }
    }

    private fun weekDates(anchor: LocalDate): List<LocalDate> {
        val mondayOffset = ((anchor.dayOfWeek.value + 6) % 7).toLong()
        val monday = anchor.minusDays(mondayOffset)
        return (0L..6L).map { monday.plusDays(it) }
    }
}
