package cdglacier.mytool.ui.screen.healthconnect.sleepstage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cdglacier.mytool.data.repository.SleepPageRepository
import cdglacier.mytool.domain.model.SleepBook
import cdglacier.mytool.domain.model.SleepDay
import cdglacier.mytool.domain.model.SleepStageType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class SleepStageChartViewModel @Inject constructor(
    private val sleepPageRepository: SleepPageRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SleepStageChartUiState())
    val uiState: StateFlow<SleepStageChartUiState> = _uiState.asStateFlow()

    private var book: SleepBook = SleepBook()
    private var collecting = false

    fun setDate(date: LocalDate) {
        _uiState.update { it.copy(date = date) }
        if (!collecting) {
            collecting = true
            viewModelScope.launch {
                sleepPageRepository.observeBook().collect { b ->
                    book = b
                    recompute()
                }
            }
        } else {
            recompute()
        }
    }

    fun onDateChange(delta: Long) {
        val next = _uiState.value.date.plusDays(delta)
        setDate(next)
    }

    private fun recompute() {
        val date = _uiState.value.date
        val day = book.days[date] ?: SleepDay(date)
        val bands = day.sessions.flatMap { s ->
            s.stages.map { SleepStageBandUiModel(it.type, it.start, it.end) }
        }.sortedBy { it.start }

        val windowStart = bands.minByOrNull { it.start }?.start
            ?: day.sessions.minByOrNull { it.start }?.start
        val windowEnd = bands.maxByOrNull { it.end }?.end
            ?: day.sessions.maxByOrNull { it.end }?.end

        val totals = bands.groupBy { it.type }
            .mapValues { (_, list) -> list.sumOf { Duration.between(it.start, it.end).toMinutes() } }
        val totalMin = totals.values.sum()
        val awakeMin = AWAKE_STAGE_TYPES.sumOf { totals[it] ?: 0L }

        _uiState.update {
            it.copy(
                bands = bands,
                windowStart = windowStart,
                windowEnd = windowEnd,
                stageTotals = totals,
                totalMinutes = totalMin,
                awakeMinutes = awakeMin,
            )
        }
    }

    private companion object {
        val AWAKE_STAGE_TYPES = listOf(
            SleepStageType.AWAKE,
            SleepStageType.AWAKE_IN_BED,
            SleepStageType.OUT_OF_BED,
        )
    }
}
