package cdglacier.mytool.domain.usecase

import cdglacier.mytool.domain.model.HouseholdPoint

object HouseholdPointParser {
    private const val HEADER = "| 家事 | ポイント |"
    private const val SEPARATOR = "|---|---|"

    fun parse(markdown: String): List<HouseholdPoint> {
        val result = mutableListOf<HouseholdPoint>()
        for (raw in markdown.lines()) {
            val line = raw.trim()
            if (!line.startsWith("|") || !line.endsWith("|")) continue
            val cells = line.trim('|').split("|").map { it.trim() }
            if (cells.size != 2) continue
            val name = cells[0]
            val pointsCell = cells[1]
            if (name == "家事" || name.isEmpty()) continue
            if (pointsCell.all { it == '-' || it == ':' } && pointsCell.isNotEmpty()) continue
            val points = pointsCell.toIntOrNull() ?: continue
            result.add(HouseholdPoint(name, points))
        }
        return result
    }

    fun serialize(points: List<HouseholdPoint>): String = buildString {
        appendLine(HEADER)
        appendLine(SEPARATOR)
        for (p in points) {
            appendLine("| ${p.name} | ${p.points} |")
        }
    }
}
