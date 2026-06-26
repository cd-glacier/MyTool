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
 * pages/Money.md の Markdown テーブル形式シリアライザ/パーサ。
 *
 * フォーマット:
 *
 * # Money
 *
 * ## Services
 * | Name | Annual | Start | End |
 *
 * ## Archived
 * | Category | Name |
 *
 * ## Incomes
 * | Month | Name | Amount |
 *
 * ## Card
 * | Month | Name | Amount |
 *
 * ## Budget
 * | Month | Name | Amount | Tag |
 *
 * ## Savings
 * | Month | Name | Amount | Category | ToLifeAccount |
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

        appendLine("## Incomes")
        appendLine("| Month | Name | Amount |")
        appendLine("|---|---|---|")
        for (ym in sortedMonths) {
            val m = book.months[ym] ?: continue
            for (i in m.incomes) appendLine("| ${ym.format(MONTH_FMT)} | ${escape(i.name)} | ${i.amount} |")
        }
        appendLine()

        appendLine("## Card")
        appendLine("| Month | Name | Amount |")
        appendLine("|---|---|---|")
        for (ym in sortedMonths) {
            val m = book.months[ym] ?: continue
            for (i in m.cardExpenses) appendLine("| ${ym.format(MONTH_FMT)} | ${escape(i.name)} | ${i.amount} |")
        }
        appendLine()

        appendLine("## Budget")
        appendLine("| Month | Name | Amount | Tag |")
        appendLine("|---|---|---|---|")
        for (ym in sortedMonths) {
            val m = book.months[ym] ?: continue
            for (i in m.budgets) appendLine("| ${ym.format(MONTH_FMT)} | ${escape(i.name)} | ${i.amount} | ${escape(i.tag)} |")
        }
        appendLine()

        appendLine("## Savings")
        appendLine("| Month | Name | Amount | Category | ToLifeAccount |")
        appendLine("|---|---|---|---|---|")
        for (ym in sortedMonths) {
            val m = book.months[ym] ?: continue
            for (s in m.savings) {
                appendLine("| ${ym.format(MONTH_FMT)} | ${escape(s.name)} | ${s.amount} | ${escape(s.category)} | ${s.toLifeAccount} |")
            }
        }
        appendLine()
    }

    fun parse(text: String): MoneyBook {
        val lines = text.lines()
        val services = mutableListOf<AnnualService>()
        val archived = mutableMapOf<MoneyCategory, MutableSet<String>>()
        val incomesByMonth = mutableMapOf<YearMonth, MutableList<MoneyItem>>()
        val cardByMonth = mutableMapOf<YearMonth, MutableList<MoneyItem>>()
        val budgetByMonth = mutableMapOf<YearMonth, MutableList<MoneyItem>>()
        val savingsByMonth = mutableMapOf<YearMonth, MutableList<SavingsItem>>()

        var section: String? = null

        for (raw in lines) {
            val line = raw.trim()
            when {
                line.startsWith("## ") -> {
                    val title = line.removePrefix("## ").trim().lowercase()
                    section = when (title) {
                        "services" -> "services"
                        "archived" -> "archived"
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
                        "incomes" -> if (cells.size >= 3) {
                            val ym = parseMonth(cells[0]) ?: continue
                            incomesByMonth.getOrPut(ym) { mutableListOf() } +=
                                MoneyItem(cells[1], cells[2].toLongOrNull() ?: 0L)
                        }
                        "card" -> if (cells.size >= 3) {
                            val ym = parseMonth(cells[0]) ?: continue
                            cardByMonth.getOrPut(ym) { mutableListOf() } +=
                                MoneyItem(cells[1], cells[2].toLongOrNull() ?: 0L)
                        }
                        "budget" -> if (cells.size >= 3) {
                            val ym = parseMonth(cells[0]) ?: continue
                            budgetByMonth.getOrPut(ym) { mutableListOf() } += MoneyItem(
                                name = cells[1],
                                amount = cells[2].toLongOrNull() ?: 0L,
                                tag = cells.getOrNull(3).orEmpty(),
                            )
                        }
                        "savings" -> if (cells.size >= 5) {
                            val ym = parseMonth(cells[0]) ?: continue
                            savingsByMonth.getOrPut(ym) { mutableListOf() } += SavingsItem(
                                name = cells[1],
                                amount = cells[2].toLongOrNull() ?: 0L,
                                category = cells[3],
                                toLifeAccount = cells[4].equals("true", ignoreCase = true),
                            )
                        }
                    }
                }
            }
        }

        val allMonths = (incomesByMonth.keys + cardByMonth.keys + budgetByMonth.keys + savingsByMonth.keys)
        val months = allMonths.associateWith { ym ->
            MonthlyMoney(
                month = ym,
                incomes = incomesByMonth[ym]?.toList().orEmpty(),
                cardExpenses = cardByMonth[ym]?.toList().orEmpty(),
                budgets = budgetByMonth[ym]?.toList().orEmpty(),
                savings = savingsByMonth[ym]?.toList().orEmpty(),
            )
        }

        return MoneyBook(
            months = months,
            services = services,
            archived = archived.mapValues { it.value.toSet() },
        )
    }

    private fun parseMonth(s: String): YearMonth? =
        runCatching { YearMonth.parse(s, MONTH_FMT) }.getOrNull()

    private fun isHeaderRow(line: String): Boolean {
        val first = parseRow(line).firstOrNull()?.lowercase()
        return first in setOf("name", "category", "month")
    }

    private fun parseRow(line: String): List<String> =
        line.trim().trim('|').split("|").map { it.trim() }

    private fun escape(s: String): String = s.replace("|", "\\|").replace("\n", " ")
}
