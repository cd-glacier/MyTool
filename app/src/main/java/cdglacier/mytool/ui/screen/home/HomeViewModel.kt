package cdglacier.mytool.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cdglacier.mytool.data.repository.DailySummaryRepository
import cdglacier.mytool.domain.usecase.DailyActivity
import cdglacier.mytool.domain.usecase.GetActivityRatesUseCase
import cdglacier.mytool.domain.usecase.SyncDailySummariesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dailySummaryRepository: DailySummaryRepository,
    private val syncDailySummariesUseCase: SyncDailySummariesUseCase,
    private val getActivityRatesUseCase: GetActivityRatesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var hasLoadedOnce = false

    init {
        viewModelScope.launch {
            dailySummaryRepository.summaries.collect { summaries ->
                val activities = getActivityRatesUseCase(summaries)
                val todayRate = summaries[LocalDate.now()]?.habitRate
                _uiState.update {
                    it.copy(dailyActivities = activities, todayCompletionRate = todayRate)
                }
            }
        }
    }

    fun onSelectDate(date: LocalDate) {
        if (date.isAfter(LocalDate.now())) return
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = !hasLoadedOnce) }
            val today = LocalDate.now()
            val from = today.minusDays(GRAPH_RANGE_DAYS)
            syncDailySummariesUseCase(from..today)
            _uiState.update { it.copy(isLoading = false) }
            hasLoadedOnce = true
        }
    }

    companion object {
        private const val GRAPH_RANGE_DAYS: Long = 400
    }
}
