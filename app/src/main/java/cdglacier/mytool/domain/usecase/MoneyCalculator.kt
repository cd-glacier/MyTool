package cdglacier.mytool.domain.usecase

import cdglacier.mytool.domain.model.MoneyBook
import cdglacier.mytool.domain.model.MoneyCategory
import cdglacier.mytool.domain.model.MoneyItem
import cdglacier.mytool.domain.model.MonthlyMoney
import cdglacier.mytool.domain.model.SavingsItem
import java.time.YearMonth

object MoneyCalculator {

    /**
     * 差額 = 手取り - 2ヶ月前カード - 当月予算 - 貯蓄 - 年額サービス月割
     */
    fun difference(book: MoneyBook, month: YearMonth): Long {
        val m = book.monthOrEmpty(month)
        return m.incomeTotal - m.cardTotal - m.budgetTotal - m.savingsTotal - book.servicesMonthlyTotal(month)
    }

    /** 生活用口座への振込内訳: 予算方式生活費合計 + 「生活用口座行き」貯蓄 */
    fun lifeAccountTransfer(book: MoneyBook, month: YearMonth): Long {
        val m = book.monthOrEmpty(month)
        return m.budgetTotal + m.savingsToLifeAccount
    }

    /** 前月の項目名のみを引き継いで初期化された当月データを返す（アーカイブ済みは除外） */
    fun carryForwardNames(book: MoneyBook, month: YearMonth): MonthlyMoney {
        val existing = book.months[month]
        if (existing != null) return existing
        val previous = book.months[month.minusMonths(1)] ?: return MonthlyMoney(month)
        fun MoneyCategory.archivedSet() = book.archived[this] ?: emptySet()
        return MonthlyMoney(
            month = month,
            incomes = previous.incomes
                .filterNot { it.name in MoneyCategory.INCOME.archivedSet() }
                .map { MoneyItem(it.name, 0L) },
            cardExpenses = previous.cardExpenses
                .filterNot { it.name in MoneyCategory.CARD.archivedSet() }
                .map { MoneyItem(it.name, 0L) },
            budgets = previous.budgets
                .filterNot { it.name in MoneyCategory.BUDGET.archivedSet() }
                .map { MoneyItem(it.name, 0L) },
            savings = previous.savings
                .filterNot { it.name in MoneyCategory.SAVINGS.archivedSet() }
                .map { SavingsItem(it.name, 0L, it.category, it.toLifeAccount) },
        )
    }
}
