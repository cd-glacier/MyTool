package cdglacier.mytool.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cdglacier.mytool.data.repository.HabitHistoryRepository
import cdglacier.mytool.data.repository.ObsidianRepository
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var hasLoadedOnce = false

    init {
        // キャッシュは Flow で常に追従(SYNC_HISTORYボタン押下後に自動反映)
        viewModelScope.launch {
            habitHistoryRepository.history.collect { history ->
                _uiState.update { state ->
                    val today = state.todayCompletionRate
                    state.copy(habitCompletionRates = mergeWithToday(history, today))
                }
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
            _uiState.update {
                it.copy(
                    todayCompletionRate = todayRate,
                    habitCompletionRates = mergeWithToday(history, todayRate),
                    isLoading = false,
                )
            }
            hasLoadedOnce = true
        }
    }

    private fun mergeWithToday(
        history: Map<LocalDate, Float>,
        todayRate: Float?,
    ): Map<LocalDate, Float?> {
        val today = LocalDate.now()
        val merged = history.toMutableMap<LocalDate, Float>()
        // 過去日(今日以外)はキャッシュ、当日はライブ値で上書き
        merged.remove(today)
        val result: MutableMap<LocalDate, Float?> = merged.toMutableMap()
        if (todayRate != null) result[today] = todayRate
        return result
    }
}
