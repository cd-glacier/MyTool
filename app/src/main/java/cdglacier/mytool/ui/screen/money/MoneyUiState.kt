package cdglacier.mytool.ui.screen.money

import cdglacier.mytool.domain.model.AnnualService
import cdglacier.mytool.domain.model.MoneyBook
import cdglacier.mytool.domain.model.MoneyCategory
import cdglacier.mytool.domain.model.MonthlyMoney
import java.time.YearMonth

data class MoneyUiState(
    val isLoading: Boolean = true,
    val book: MoneyBook = MoneyBook(),
    val displayedMonth: YearMonth = YearMonth.now(),
    val isVaultConfigured: Boolean = false,
    val saveError: String? = null,
) {
    val currentMonth: MonthlyMoney
        get() = book.monthOrEmpty(displayedMonth)

    val services: List<AnnualService> get() = book.services

    val incomeTotal: Long get() = currentMonth.incomeTotal
    val cardTotal: Long get() = currentMonth.cardTotal
    val budgetTotal: Long get() = currentMonth.budgetTotal
    val savingsTotal: Long get() = currentMonth.savingsTotal
    val servicesMonthlyTotal: Long get() = book.servicesMonthlyTotal(displayedMonth)

    val difference: Long
        get() = incomeTotal - cardTotal - budgetTotal - savingsTotal - servicesMonthlyTotal

    val lifeAccountTransfer: Long
        get() = budgetTotal + currentMonth.savingsToLifeAccount

    fun isArchived(category: MoneyCategory, name: String): Boolean =
        book.archived[category]?.contains(name) == true
}
