package cdglacier.mytool.data.repository

import cdglacier.mytool.domain.model.AnnualService
import cdglacier.mytool.domain.model.MoneyBook
import cdglacier.mytool.domain.model.MoneyCategory
import cdglacier.mytool.domain.model.MoneyItem
import cdglacier.mytool.domain.model.MonthlyMoney
import cdglacier.mytool.domain.model.SavingsItem
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * pages/money.md の Markdown テーブル形式シリアライザ/パーサ。
 *
 * フォーマット:
 *
 * # Money
 *
 * ## Services
 * | Name | Annual | Start | End |
 * |---|---|---|---|
 *
 * ## Archived
 * | Category | Name |
 * |---|---|
 *
 * ## 2026-06
 * ### Incomes
 * | Name | Amount |
 * ### Card
 * | Name | Amount |
 * ### Budget
 * | Name | Amount |
 * ### Savings
 * | Name | Amount | Category | ToLifeAccount |
 */
object MoneyMarkdown {

    private val MONTH_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
    private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun serialize(book: MoneyBook): String = buildString {
        appendLine("# Money")
        appendLine()
        appendLine("## Services")
        appendLine("| Name | Annual | Start | End |")
        appendLine("|---|---|---|---|")
        for (s in book.services) {
            appendLine("| ${escape(s.name)} | ${s.annualAmount} | ${s.startDate.format(DATE_FMT)} | ${s.endDate.format(DATE_FMT)} |")
        }
        appendLine()
        appendLine("## Archived")
        appendLine("| Category | Name |")
        appendLine("|---|---|")
        for ((cat, names) in book.archived) {
            for (n in names) appendLine("| ${cat.name} | ${escape(n)} |")
        }
        appendLine()
        val sortedMonths = book.months.keys.sortedDescending()
        for (ym in sortedMonths) {
            val m = book.months[ym] ?: continue
            appendLine("## ${ym.format(MONTH_FMT)}")
            appendLine()
            appendLine("### Incomes")
            appendLine("| Name | Amount |")
            appendLine("|---|---|")
            for (i in m.incomes) appendLine("| ${escape(i.name)} | ${i.amount} |")
            appendLine()
            appendLine("### Card")
            appendLine("| Name | Amount |")
            appendLine("|---|---|")
            for (i in m.cardExpenses) appendLine("| ${escape(i.name)} | ${i.amount} |")
            appendLine()
            appendLine("### Budget")
            appendLine("| Name | Amount |")
            appendLine("|---|---|")
            for (i in m.budgets) appendLine("| ${escape(i.name)} | ${i.amount} |")
            appendLine()
            appendLine("### Savings")
            appendLine("| Name | Amount | Category | ToLifeAccount |")
            appendLine("|---|---|---|---|")
            for (s in m.savings) {
                appendLine("| ${escape(s.name)} | ${s.amount} | ${escape(s.category)} | ${s.toLifeAccount} |")
            }
            appendLine()
        }
    }

    fun parse(text: String): MoneyBook {
        val lines = text.lines()
        var i = 0
        val services = mutableListOf<AnnualService>()
        val archived = mutableMapOf<MoneyCategory, MutableSet<String>>()
        val months = mutableMapOf<YearMonth, MonthlyMoney>()

        var currentMonth: YearMonth? = null
        var currentIncomes = mutableListOf<MoneyItem>()
        var currentCard = mutableListOf<MoneyItem>()
        var currentBudget = mutableListOf<MoneyItem>()
        var currentSavings = mutableListOf<SavingsItem>()
        var section: String? = null // "services" | "archived" | "incomes" | "card" | "budget" | "savings"

        fun flushMonth() {
            val m = currentMonth ?: return
            months[m] = MonthlyMoney(
                month = m,
                incomes = currentIncomes.toList(),
                cardExpenses = currentCard.toList(),
                budgets = currentBudget.toList(),
                savings = currentSavings.toList(),
            )
            currentIncomes = mutableListOf()
            currentCard = mutableListOf()
            currentBudget = mutableListOf()
            currentSavings = mutableListOf()
        }

        while (i < lines.size) {
            val line = lines[i].trim()
            when {
                line.startsWith("## ") -> {
                    val title = line.removePrefix("## ").trim()
                    if (title.equals("Services", ignoreCase = true)) {
                        flushMonth(); currentMonth = null; section = "services"
                    } else if (title.equals("Archived", ignoreCase = true)) {
                        flushMonth(); currentMonth = null; section = "archived"
                    } else {
                        flushMonth()
                        currentMonth = runCatching { YearMonth.parse(title, MONTH_FMT) }.getOrNull()
                        section = null
                    }
                }
                line.startsWith("### ") -> {
                    val title = line.removePrefix("### ").trim().lowercase()
                    section = when (title) {
                        "incomes" -> "incomes"
                        "card" -> "card"
                        "budget" -> "budget"
                        "savings" -> "savings"
                        else -> null
                    }
                }
                line.startsWith("|") && !line.startsWith("|---") && !isHeaderRow(line) -> {
                    val cells = parseRow(line)
                    when (section) {
                        "services" -> if (cells.size >= 4) {
                            val annual = cells[1].toLongOrNull() ?: 0L
                            val start = runCatching { LocalDate.parse(cells[2], DATE_FMT) }.getOrNull()
                            val end = runCatching { LocalDate.parse(cells[3], DATE_FMT) }.getOrNull()
                            if (start != null && end != null) {
                                services += AnnualService(cells[0], annual, start, end)
                            }
                        }
                        "archived" -> if (cells.size >= 2) {
                            val cat = runCatching { MoneyCategory.valueOf(cells[0]) }.getOrNull()
                            if (cat != null) archived.getOrPut(cat) { mutableSetOf() } += cells[1]
                        }
                        "incomes" -> if (cells.size >= 2) {
                            currentIncomes += MoneyItem(cells[0], cells[1].toLongOrNull() ?: 0L)
                        }
                        "card" -> if (cells.size >= 2) {
                            currentCard += MoneyItem(cells[0], cells[1].toLongOrNull() ?: 0L)
                        }
                        "budget" -> if (cells.size >= 2) {
                            currentBudget += MoneyItem(cells[0], cells[1].toLongOrNull() ?: 0L)
                        }
                        "savings" -> if (cells.size >= 4) {
                            currentSavings += SavingsItem(
                                name = cells[0],
                                amount = cells[1].toLongOrNull() ?: 0L,
                                category = cells[2],
                                toLifeAccount = cells[3].equals("true", ignoreCase = true),
                            )
                        }
                    }
                }
            }
            i++
        }
        flushMonth()
        return MoneyBook(
            months = months,
            services = services,
            archived = archived.mapValues { it.value.toSet() },
        )
    }

    private fun isHeaderRow(line: String): Boolean {
        val cells = parseRow(line).map { it.lowercase() }
        return cells.firstOrNull() in setOf("name", "category")
    }

    private fun parseRow(line: String): List<String> =
        line.trim().trim('|').split("|").map { it.trim() }

    private fun escape(s: String): String = s.replace("|", "\\|").replace("\n", " ")
}
