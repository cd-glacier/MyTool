package cdglacier.mytool.ui.screen.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.domain.model.Habit
import cdglacier.mytool.domain.usecase.GetTodayHabitsUseCase
import cdglacier.mytool.domain.usecase.ToggleHabitUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HabitTrackingViewModel @Inject constructor(
    private val obsidianRepository: ObsidianRepository,
    private val getTodayHabitsUseCase: GetTodayHabitsUseCase,
    private val toggleHabitUseCase: ToggleHabitUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitTrackingUiState())
    val uiState: StateFlow<HabitTrackingUiState> = _uiState.asStateFlow()

    private var hasLoadedOnce = false

    fun refresh() {
        viewModelScope.launch {
            val uri = obsidianRepository.journalDirUri.first()?.toString()
            val format = obsidianRepository.filenameFormat.first()
            loadHabits(uri, format, _uiState.value.date, showLoading = !hasLoadedOnce)
            hasLoadedOnce = true
        }
    }

    fun onDateChange(delta: Long) {
        val today = LocalDate.now()
        val next = _uiState.value.date.plusDays(delta).let {
            if (it.isAfter(today)) today else it
        }
        if (next == _uiState.value.date) return
        _uiState.update { it.copy(date = next, habits = emptyList(), isLoading = true) }
        viewModelScope.launch {
            val uri = obsidianRepository.journalDirUri.first()?.toString()
            val format = obsidianRepository.filenameFormat.first()
            loadHabits(uri, format, next, showLoading = true)
        }
    }

    fun onHabitToggle(habit: Habit) {
        viewModelScope.launch {
            val uri = obsidianRepository.journalDirUri.first()?.toString() ?: return@launch
            val format = obsidianRepository.filenameFormat.first()
            val date = _uiState.value.date
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
                loadHabits(uri, format, date, showLoading = false)
            }
        }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private suspend fun loadHabits(
        journalDirUri: String?,
        filenameFormat: String,
        date: LocalDate,
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
