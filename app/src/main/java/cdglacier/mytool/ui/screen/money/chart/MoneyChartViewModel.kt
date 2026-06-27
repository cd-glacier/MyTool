package cdglacier.mytool.ui.screen.money.chart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cdglacier.mytool.data.repository.MoneyRepository
import cdglacier.mytool.domain.model.MoneyBook
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class MoneyChartViewModel @Inject constructor(
    private val moneyRepository: MoneyRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoneyChartUiState())
    val uiState: StateFlow<MoneyChartUiState> = _uiState.asStateFlow()

    private var book: MoneyBook = MoneyBook()
    private var initialized = false

    fun setTarget(section: MoneySection, group: String?) {
        if (initialized && _uiState.value.section == section && _uiState.value.group == group) return
        initialized = true
        _uiState.update { it.copy(section = section, group = group) }
        viewModelScope.launch {
            moneyRepository.observeBook().collect { b ->
                book = b
                recompute()
            }
        }
    }

    private fun recompute() {
        val state = _uiState.value
        val months = monthRange(book)
        val series = buildSeries(book, state.section, state.group, months)
        _uiState.update { it.copy(isLoading = false, months = months, series = series) }
    }

    private fun monthRange(book: MoneyBook): List<YearMonth> {
        val keys = book.months.keys
        if (keys.isEmpty()) {
            val now = YearMonth.now()
            return (0L until 6L).map { now.minusMonths(5 - it) }
        }
        val min = keys.min()
        val max = keys.max()
        val list = mutableListOf<YearMonth>()
        var cur = min
        while (cur <= max) {
            list.add(cur)
            cur = cur.plusMonths(1)
        }
        return list
    }

    private fun buildSeries(
        book: MoneyBook,
        section: MoneySection,
        group: String?,
        months: List<YearMonth>,
    ): List<ChartSeries> {
        return when (section) {
            MoneySection.INCOMES -> seriesForItems(months) { m -> book.monthOrEmpty(m).incomes.map { it.name to it.amount } }
            MoneySection.CARD -> seriesForItems(months) { m -> book.monthOrEmpty(m).cardExpenses.map { it.name to it.amount } }
            MoneySection.BUDGET -> seriesForItems(months) { m ->
                book.monthOrEmpty(m).budgets
                    .filter { group == null || it.tag == group }
                    .map { it.name to it.amount }
            }
            MoneySection.SAVINGS -> seriesForItems(months) { m ->
                book.monthOrEmpty(m).savings
                    .filter { group == null || it.category == group }
                    .map { it.name to it.amount }
            }
            MoneySection.SERVICES -> {
                val names = book.services.filterNot { it.archived }.map { it.name }.distinct()
                names.map { name ->
                    val svc = book.services.firstOrNull { it.name == name && !it.archived }
                    val values = months.map { ym -> svc?.monthlyAmountFor(ym) ?: 0L }
                    ChartSeries(name, values)
                }
            }
        }
    }

    private inline fun seriesForItems(
        months: List<YearMonth>,
        crossinline itemsFor: (YearMonth) -> List<Pair<String, Long>>,
    ): List<ChartSeries> {
        val names = linkedSetOf<String>()
        val perMonth = months.map { m ->
            val map = mutableMapOf<String, Long>()
            for ((name, amount) in itemsFor(m)) {
                names.add(name)
                map[name] = (map[name] ?: 0L) + amount
            }
            map
        }
        return names.map { name ->
            ChartSeries(name, perMonth.map { it[name] ?: 0L })
        }
    }
}
