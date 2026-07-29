package cdglacier.mytool.ui.screen.healthconnect.chart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cdglacier.mytool.data.repository.HealthPageRepository
import cdglacier.mytool.domain.model.HealthBook
import cdglacier.mytool.domain.model.HealthMetric
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HealthChartViewModel @Inject constructor(
    private val healthPageRepository: HealthPageRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HealthChartUiState())
    val uiState: StateFlow<HealthChartUiState> = _uiState.asStateFlow()

    private var book: HealthBook = HealthBook()
    private var collecting = false

    fun setMetric(key: String) {
        val metric = HealthMetric.fromKey(key) ?: return
        if (_uiState.value.metric == metric) return
        _uiState.update { it.copy(metric = metric) }
        if (!collecting) {
            collecting = true
            viewModelScope.launch {
                healthPageRepository.observeBook().collect { b ->
                    book = b
                    recompute()
                }
            }
        } else {
            recompute()
        }
    }

    fun onModeChange(mode: HealthChartMode) {
        if (_uiState.value.mode == mode) return
        _uiState.update { it.copy(mode = mode) }
        recompute()
    }

    private fun recompute() {
        val metric = _uiState.value.metric ?: return
        val mode = _uiState.value.mode
        val points = when (mode) {
            HealthChartMode.DAY -> buildDailyPoints(metric)
            HealthChartMode.WEEK -> buildWeeklyPoints(metric)
        }
        _uiState.update { it.copy(points = points) }
    }

    private fun buildDailyPoints(metric: HealthMetric): List<HealthChartPoint> {
        val today = LocalDate.now()
        val days = (0L until 90L).map { today.minusDays(89 - it) }
        return days.map { d ->
            val v = book.days[d]?.let(metric::valueOf)
            HealthChartPoint(label = d.toString().takeLast(5), value = v) // MM-dd
        }
    }

    private fun buildWeeklyPoints(metric: HealthMetric): List<HealthChartPoint> {
        val today = LocalDate.now()
        val mondayThisWeek = today.minusDays(((today.dayOfWeek.value + 6) % 7).toLong())
        val weekStarts = (0L until 26L).map { mondayThisWeek.minusWeeks(25 - it) }
        return weekStarts.map { monday ->
            val vs = (0L..6L).mapNotNull { book.days[monday.plusDays(it)]?.let(metric::valueOf) }
            val avg = if (vs.isEmpty()) null else vs.average()
            HealthChartPoint(label = monday.toString().takeLast(5), value = avg)
        }
    }
}
