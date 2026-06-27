package cdglacier.mytool.ui.screen.money.chart

import java.time.YearMonth

enum class MoneySection {
    INCOMES, CARD, BUDGET, SAVINGS, SERVICES;

    companion object {
        fun fromKey(key: String): MoneySection = valueOf(key)
    }
}

data class MoneyChartUiState(
    val isLoading: Boolean = true,
    val section: MoneySection = MoneySection.INCOMES,
    val group: String? = null,
    val months: List<YearMonth> = emptyList(),
    val series: List<ChartSeries> = emptyList(),
) {
    val title: String
        get() = buildString {
            append(section.name)
            if (group != null) append(" / ").append(group)
        }
}

data class ChartSeries(
    val name: String,
    val values: List<Long>,
)
