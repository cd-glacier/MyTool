package cdglacier.mytool.domain.model

import java.time.LocalDate
import java.time.YearMonth

data class MoneyItem(
    val name: String,
    val amount: Long,
    val tag: String = "",
)

data class SavingsItem(
    val name: String,
    val amount: Long,
    val category: String,
    val toLifeAccount: Boolean,
)

data class AnnualService(
    val name: String,
    val annualAmount: Long,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val archived: Boolean = false,
) {
    fun monthlyAmountFor(month: YearMonth): Long {
        if (archived) return 0L
        val start = YearMonth.from(startDate)
        val end = YearMonth.from(endDate)
        if (month < start || month > end) return 0L
        return annualAmount / 12
    }
}

data class MonthlyMoney(
    val month: YearMonth,
    val incomes: List<MoneyItem> = emptyList(),
    val cardExpenses: List<MoneyItem> = emptyList(),
    val budgets: List<MoneyItem> = emptyList(),
    val savings: List<SavingsItem> = emptyList(),
) {
    val incomeTotal: Long get() = incomes.sumOf { it.amount }
    val cardTotal: Long get() = cardExpenses.sumOf { it.amount }
    val budgetTotal: Long get() = budgets.sumOf { it.amount }
    val savingsTotal: Long get() = savings.sumOf { it.amount }
    val savingsToLifeAccount: Long get() = savings.filter { it.toLifeAccount }.sumOf { it.amount }
}

enum class MoneyCategory { INCOME, CARD, BUDGET, SAVINGS }

data class MoneyBook(
    val months: Map<YearMonth, MonthlyMoney> = emptyMap(),
    val services: List<AnnualService> = emptyList(),
    /** カテゴリごとにアーカイブされた項目名 */
    val archived: Map<MoneyCategory, Set<String>> = emptyMap(),
) {
    fun monthOrEmpty(month: YearMonth): MonthlyMoney =
        months[month] ?: MonthlyMoney(month)

    fun servicesMonthlyTotal(month: YearMonth): Long =
        services.sumOf { it.monthlyAmountFor(month) }
}
