package cdglacier.mytool.ui.screen.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cdglacier.mytool.data.repository.HabitHistoryRepository
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.domain.model.Habit
import cdglacier.mytool.domain.usecase.GetTodayHabitsUseCase
import cdglacier.mytool.domain.usecase.SyncHabitHistoryUseCase
import cdglacier.mytool.domain.usecase.ToggleHabitUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HabitTrackingViewModel @Inject constructor(
    private val obsidianRepository: ObsidianRepository,
    private val habitHistoryRepository: HabitHistoryRepository,
    private val getTodayHabitsUseCase: GetTodayHabitsUseCase,
    private val toggleHabitUseCase: ToggleHabitUseCase,
    private val syncHabitHistoryUseCase: SyncHabitHistoryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitTrackingUiState())
    val uiState: StateFlow<HabitTrackingUiState> = _uiState.asStateFlow()

    private var hasLoadedOnce = false

    init {
        viewModelScope.launch {
            combine(
                habitHistoryRepository.history,
                habitHistoryRepository.lastSyncedAtEpochMillis,
            ) { history, syncedAt -> Pair(history, syncedAt) }
                .collect { (history, syncedAt) ->
                    _uiState.update {
                        it.copy(
                            historyDayCount = history.size,
                            lastSyncedAtEpochMillis = syncedAt,
                        )
                    }
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val uri = obsidianRepository.journalDirUri.first()?.toString()
            val format = obsidianRepository.filenameFormat.first()
            loadHabits(uri, format, showLoading = !hasLoadedOnce)
            hasLoadedOnce = true
        }
    }

    fun onSyncHistory() {
        viewModelScope.launch {
            val uri = obsidianRepository.journalDirUri.first()?.toString() ?: return@launch
            val format = obsidianRepository.filenameFormat.first()
            _uiState.update { it.copy(isSyncingHistory = true) }
            val today = LocalDate.now()
            val dates = (0 until 182).map { today.minusDays(it.toLong()) }
            val result = syncHabitHistoryUseCase(uri, format, dates)
            _uiState.update {
                it.copy(
                    isSyncingHistory = false,
                    errorMessage = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun onHabitToggle(habit: Habit) {
        viewModelScope.launch {
            val uri = obsidianRepository.journalDirUri.first()?.toString() ?: return@launch
            val format = obsidianRepository.filenameFormat.first()
            val date = _uiState.value.date
            // 楽観更新
            _uiState.update { state ->
                state.copy(
                    habits = state.habits.map {
                        if (it.name == habit.name && it.frequency == habit.frequency) {
                            it.copy(isCompleted = !it.isCompleted)
                        } else it
                    }
                )
            }
            val result = toggleHabitUseCase(uri, format, date, habit)
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
                loadHabits(uri, format, showLoading = false)
            }
        }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private suspend fun loadHabits(
        journalDirUri: String?,
        filenameFormat: String,
        showLoading: Boolean,
    ) {
        if (journalDirUri == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    journalConfigured = false,
                    habits = emptyList(),
                )
            }
            return
        }
        if (showLoading) {
            _uiState.update { it.copy(isLoading = true, journalConfigured = true) }
        }
        val date = LocalDate.now()
        val habits = runCatching {
            getTodayHabitsUseCase(journalDirUri, filenameFormat, date)
        }.getOrDefault(emptyList())
        _uiState.update {
            it.copy(
                date = date,
                habits = habits,
                isLoading = false,
                journalConfigured = true,
                journalExists = true,
            )
        }
    }
}
