package cdglacier.mytool.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cdglacier.mytool.data.repository.HabitHistoryRepository
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.data.repository.TrackingStateRepository
import cdglacier.mytool.domain.usecase.GetActivityRatesUseCase
import cdglacier.mytool.domain.usecase.GetTodayHabitCompletionRateUseCase
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
class HomeViewModel @Inject constructor(
    private val obsidianRepository: ObsidianRepository,
    private val habitHistoryRepository: HabitHistoryRepository,
    private val getTodayHabitCompletionRateUseCase: GetTodayHabitCompletionRateUseCase,
    private val getActivityRatesUseCase: GetActivityRatesUseCase,
    private val trackingStateRepository: TrackingStateRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var hasLoadedOnce = false

    init {
        viewModelScope.launch {
            habitHistoryRepository.history.collect { history ->
                val today = _uiState.value.todayCompletionRate
                val rates = computeActivityRates(history, today)
                _uiState.update { it.copy(activityRates = rates) }
            }
        }
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
    }

    fun refresh() {
        viewModelScope.launch {
            val uri = obsidianRepository.journalDirUri.first()
            _uiState.update {
                it.copy(
                    journalDirUri = uri,
                    isLoading = !hasLoadedOnce && uri != null,
                )
            }
            if (uri == null) return@launch
            val format = obsidianRepository.filenameFormat.first()
            val today = LocalDate.now()
            val todayRate = runCatching {
                getTodayHabitCompletionRateUseCase(uri.toString(), format, today)
            }.getOrNull()
            val history = habitHistoryRepository.history.first()
            val rates = computeActivityRates(history, todayRate)
            _uiState.update {
                it.copy(
                    todayCompletionRate = todayRate,
                    activityRates = rates,
                    isLoading = false,
                )
            }
            hasLoadedOnce = true
        }
    }

    private suspend fun computeActivityRates(
        history: Map<LocalDate, Float>,
        todayRate: Float?,
    ): Map<LocalDate, Float?> {
        val today = LocalDate.now()
        val habits: MutableMap<LocalDate, Float?> = mutableMapOf<LocalDate, Float?>().apply { putAll(history) }
        habits.remove(today)
        if (todayRate != null) habits[today] = todayRate
        val from = today.minusDays(GRAPH_RANGE_DAYS)
        return getActivityRatesUseCase(habits, from, today)
    }

    companion object {
        // Graph shows ~ 1 year of weeks; over-fetch to cover any width.
        private const val GRAPH_RANGE_DAYS: Long = 400
    }
}
