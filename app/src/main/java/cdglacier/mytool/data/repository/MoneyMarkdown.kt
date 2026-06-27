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
 * pages/Money.md の Markdown シリアライザ/パーサ。
 *
 * - 1 テーブル = カテゴリ (Budget は tag、Savings は category でさらに分割)
 * - 1 行 = 1 月、1 列 = 1 項目 (Month 列を除く)
 * - 列ヘッダの接頭記号: `#` = archived, `*` = toLifeAccount (Savings のみ)
 *   両方該当時は `*#name`
 * - 該当月に項目が無い場合のセルは空文字列
 */
object MoneyMarkdown {

    private val MONTH_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
    private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private const val MARK_ARCHIVED = "#"
    private const val MARK_LIFE = "*"

    fun serialize(book: MoneyBook): String = buildString {
        appendLine("# Money")
        appendLine()
        appendLine("## Services")
        appendLine("| Name | Annual | Start | End |")
        appendLine("|---|---|---|---|")
        for (s in book.services) {
            val name = (if (s.archived) MARK_ARCHIVED else "") + escape(s.name)
            appendLine("| $name | ${s.annualAmount} | ${s.startDate.format(DATE_FMT)} | ${s.endDate.format(DATE_FMT)} |")
        }
        appendLine()

        val months = book.months.keys.sortedDescending()
        val archivedIncome = book.archived[MoneyCategory.INCOME].orEmpty()
        val archivedCard = book.archived[MoneyCategory.CARD].orEmpty()
        val archivedBudget = book.archived[MoneyCategory.BUDGET].orEmpty()
        val archivedSavings = book.archived[MoneyCategory.SAVINGS].orEmpty()

        appendItemSection(
            heading = "Incomes",
            months = months,
            itemsOf = { ym -> book.months[ym]?.incomes.orEmpty() },
            archived = archivedIncome,
        )
        appendItemSection(
            heading = "Card",
            months = months,
            itemsOf = { ym -> book.months[ym]?.cardExpenses.orEmpty() },
            archived = archivedCard,
        )

        val budgetTags = months
            .flatMap { ym -> book.months[ym]?.budgets.orEmpty().map { it.tag } }
            .distinct()
        for (tag in budgetTags) {
            appendItemSection(
                heading = "Budget: $tag",
                months = months,
                itemsOf = { ym -> book.months[ym]?.budgets.orEmpty().filter { it.tag == tag } },
                archived = archivedBudget,
            )
        }

        val savingsCategories = months
            .flatMap { ym -> book.months[ym]?.savings.orEmpty().map { it.category } }
            .distinct()
        for (category in savingsCategories) {
            appendSavingsSection(
                category = category,
                months = months,
                book = book,
                archived = archivedSavings,
            )
        }
    }

    private fun StringBuilder.appendItemSection(
        heading: String,
        months: List<YearMonth>,
        itemsOf: (YearMonth) -> List<MoneyItem>,
        archived: Set<String>,
    ) {
        val names = collectNames(months) { ym -> itemsOf(ym).map { it.name } }
        appendLine("## $heading")
        val headers = names.map { columnHeader(it, archived = it in archived, life = false) }
        appendLine("| Month | ${headers.joinToString(" | ")} |")
        appendLine("|" + "---|".repeat(names.size + 1))
        for (ym in months) {
            val byName = itemsOf(ym).associateBy { it.name }
            val cells = names.map { byName[it]?.amount?.toString().orEmpty() }
            appendLine("| ${ym.format(MONTH_FMT)} | ${cells.joinToString(" | ")} |")
        }
        appendLine()
    }

    private fun StringBuilder.appendSavingsSection(
        category: String,
        months: List<YearMonth>,
        book: MoneyBook,
        archived: Set<String>,
    ) {
        fun savingsOf(ym: YearMonth) = book.months[ym]?.savings.orEmpty().filter { it.category == category }
        val names = collectNames(months) { ym -> savingsOf(ym).map { it.name } }
        // 各項目の toLifeAccount は全月で一貫している前提。最新月の値を採用する
        val lifeFlagByName: Map<String, Boolean> = names.associateWith { name ->
            months.firstNotNullOfOrNull { ym -> savingsOf(ym).firstOrNull { it.name == name }?.toLifeAccount } ?: false
        }
        appendLine("## Savings: $category")
        val headers = names.map { columnHeader(it, archived = it in archived, life = lifeFlagByName[it] == true) }
        appendLine("| Month | ${headers.joinToString(" | ")} |")
        appendLine("|" + "---|".repeat(names.size + 1))
        for (ym in months) {
            val byName = savingsOf(ym).associateBy { it.name }
            val cells = names.map { byName[it]?.amount?.toString().orEmpty() }
            appendLine("| ${ym.format(MONTH_FMT)} | ${cells.joinToString(" | ")} |")
        }
        appendLine()
    }

    private fun collectNames(months: List<YearMonth>, namesOf: (YearMonth) -> List<String>): List<String> {
        val seen = LinkedHashSet<String>()
        for (ym in months) seen.addAll(namesOf(ym))
        return seen.toList()
    }

    private fun columnHeader(name: String, archived: Boolean, life: Boolean): String {
        val prefix = buildString {
            if (life) append(MARK_LIFE)
            if (archived) append(MARK_ARCHIVED)
        }
        return prefix + escape(name)
    }

    private data class ColumnMeta(val name: String, val archived: Boolean, val life: Boolean)

    private fun parseColumn(header: String): ColumnMeta {
        var s = header
        var life = false
        var archived = false
        if (s.startsWith(MARK_LIFE)) { life = true; s = s.drop(1) }
        if (s.startsWith(MARK_ARCHIVED)) { archived = true; s = s.drop(1) }
        return ColumnMeta(s.trim(), archived, life)
    }

    fun parse(text: String): MoneyBook {
        val lines = text.lines()
        val services = mutableListOf<AnnualService>()
        val archived = mutableMapOf<MoneyCategory, MutableSet<String>>()
        val incomesByMonth = mutableMapOf<YearMonth, MutableList<MoneyItem>>()
        val cardByMonth = mutableMapOf<YearMonth, MutableList<MoneyItem>>()
        val budgetByMonth = mutableMapOf<YearMonth, MutableList<MoneyItem>>()
        val savingsByMonth = mutableMapOf<YearMonth, MutableList<SavingsItem>>()

        // current section state
        var sectionKind: String? = null // "services" | "incomes" | "card" | "budget" | "savings"
        var sectionGrouping: String = "" // tag or category
        var columns: List<ColumnMeta> = emptyList()
        var awaitingHeader = false

        fun resetSection(kind: String?, grouping: String = "") {
            sectionKind = kind
            sectionGrouping = grouping
            columns = emptyList()
            awaitingHeader = kind in setOf("incomes", "card", "budget", "savings")
        }

        for (raw in lines) {
            val line = raw.trim()
            if (line.startsWith("## ")) {
                val title = line.removePrefix("## ").trim()
                when {
                    title.equals("Services", ignoreCase = true) -> resetSection("services")
                    title.equals("Incomes", ignoreCase = true) -> resetSection("incomes")
                    title.equals("Card", ignoreCase = true) -> resetSection("card")
                    title.startsWith("Budget:", ignoreCase = true) ->
                        resetSection("budget", title.removePrefix("Budget:").trim())
                    title.startsWith("Savings:", ignoreCase = true) ->
                        resetSection("savings", title.removePrefix("Savings:").trim())
                    else -> resetSection(null)
                }
                continue
            }
            if (!line.startsWith("|") || line.startsWith("|---")) continue
            val cells = parseRow(line)

            when (sectionKind) {
                "services" -> {
                    if (isHeaderRow(cells)) continue
                    if (cells.size >= 4) {
                        val nameRaw = cells[0]
                        val archivedFlag = nameRaw.startsWith(MARK_ARCHIVED)
                        val name = if (archivedFlag) nameRaw.drop(1).trim() else nameRaw
                        val annual = cells[1].toLongOrNull() ?: 0L
                        val start = runCatching { LocalDate.parse(cells[2], DATE_FMT) }.getOrNull()
                        val end = runCatching { LocalDate.parse(cells[3], DATE_FMT) }.getOrNull()
                        if (start != null && end != null) {
                            services += AnnualService(name, annual, start, end, archivedFlag)
                        }
                    }
                }
                "incomes", "card", "budget", "savings" -> {
                    if (awaitingHeader) {
                        // 最初の table 行はヘッダ: | Month | name1 | name2 |
                        columns = cells.drop(1).map { parseColumn(it) }
                        // archived 記録
                        val cat = when (sectionKind) {
                            "incomes" -> MoneyCategory.INCOME
                            "card" -> MoneyCategory.CARD
                            "budget" -> MoneyCategory.BUDGET
                            "savings" -> MoneyCategory.SAVINGS
                            else -> null
                        }
                        if (cat != null) {
                            val set = archived.getOrPut(cat) { mutableSetOf() }
                            columns.filter { it.archived }.forEach { set += it.name }
                        }
                        awaitingHeader = false
                    } else {
                        val ym = parseMonth(cells.getOrNull(0).orEmpty()) ?: continue
                        val values = cells.drop(1)
                        columns.forEachIndexed { idx, col ->
                            val raw = values.getOrNull(idx).orEmpty()
                            if (raw.isBlank()) return@forEachIndexed
                            val amount = raw.toLongOrNull() ?: return@forEachIndexed
                            when (sectionKind) {
                                "incomes" -> incomesByMonth.getOrPut(ym) { mutableListOf() } += MoneyItem(col.name, amount)
                                "card" -> cardByMonth.getOrPut(ym) { mutableListOf() } += MoneyItem(col.name, amount)
                                "budget" -> budgetByMonth.getOrPut(ym) { mutableListOf() } += MoneyItem(col.name, amount, sectionGrouping)
                                "savings" -> savingsByMonth.getOrPut(ym) { mutableListOf() } += SavingsItem(col.name, amount, sectionGrouping, col.life)
                            }
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

    private fun isHeaderRow(cells: List<String>): Boolean {
        val first = cells.firstOrNull()?.lowercase()
        return first in setOf("name", "month")
    }

    private fun parseMonth(s: String): YearMonth? =
        runCatching { YearMonth.parse(s, MONTH_FMT) }.getOrNull()

    private fun parseRow(line: String): List<String> =
        line.trim().trim('|').split("|").map { it.trim() }

    private fun escape(s: String): String = s.replace("|", "\\|").replace("\n", " ")
}
