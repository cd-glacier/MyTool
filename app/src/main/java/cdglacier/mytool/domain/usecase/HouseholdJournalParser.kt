package cdglacier.mytool.domain.usecase

import cdglacier.mytool.domain.model.Assignee
import cdglacier.mytool.domain.model.HouseholdEntry

object HouseholdJournalParser {
    const val SECTION_HEADING = "# Household"
    private const val TABLE_HEADER = "| 家事 | 担当 | 回数 | 増減 |"
    private const val TABLE_SEPARATOR = "|---|---|---|---|"

    fun parse(markdown: String): List<HouseholdEntry> {
        val lines = markdown.lines()
        val start = lines.indexOfFirst { it.trimEnd() == SECTION_HEADING }
        if (start < 0) return emptyList()
        val end = (start + 1 until lines.size).firstOrNull { lines[it].startsWith("# ") } ?: lines.size

        val result = mutableListOf<HouseholdEntry>()
        for (i in start + 1 until end) {
            val line = lines[i].trim()
            if (!line.startsWith("|") || !line.endsWith("|")) continue
            val cells = line.trim('|').split("|").map { it.trim() }
            if (cells.size != 4) continue
            val name = cells[0]
            if (name == "家事" || name.isEmpty()) continue
            if (cells.all { it.all { c -> c == '-' || c == ':' } && it.isNotEmpty() }) continue
            val assignee = Assignee.fromKey(cells[1]) ?: continue
            val count = cells[2].toIntOrNull() ?: continue
            val adjustment = cells[3].removePrefix("+").toIntOrNull() ?: 0
            result.add(HouseholdEntry(name, assignee, count, adjustment))
        }
        return result
    }

    fun buildSection(entries: List<HouseholdEntry>): String = buildString {
        appendLine(SECTION_HEADING)
        appendLine()
        appendLine(TABLE_HEADER)
        appendLine(TABLE_SEPARATOR)
        for (e in entries) {
            val adj = if (e.adjustment > 0) "+${e.adjustment}" else e.adjustment.toString()
            appendLine("| ${e.name} | ${e.assignee.key} | ${e.count} | $adj |")
        }
    }
}
