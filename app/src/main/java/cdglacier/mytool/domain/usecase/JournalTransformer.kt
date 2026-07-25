package cdglacier.mytool.domain.usecase

import java.time.Duration

object JournalTransformer {
    private val TODO_ITEM_DONE = Regex("""^-\s+\[x]\s+.+$""")
    private val TODO_ITEM_ANY = Regex("""^-\s+\[( |x)]\s+.+$""")
    private val HABIT_HEADING = Regex("""^#\s+Habit\s*$""")
    private val POSITION_HEADING = Regex("""^#\s+Position Tracking\s*$""")
    private val RECIPE_HEADING = Regex("""^#\s+\[\[Recipe]]\s*$""")
    private val HOUSEHOLD_HEADING = Regex("""^#\s+Household\s*$""")
    private val HEALTH_HEADING = Regex("""^#\s+Health\s*$""")
    private val TODO_HEADING = Regex("""^#\s+TODO\s*$""")
    private val TOP_HEADING = Regex("""^#\s+.+$""")
    private val CHECKED_MARK = Regex("""\[x]""")

    fun transform(markdown: String, healthData: HealthData? = null): String {
        val lines = markdown.lines()
        val out = mutableListOf<String>()
        var i = 0
        var healthInjected = false
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            if (POSITION_HEADING.matches(trimmed) || RECIPE_HEADING.matches(trimmed) ||
                HOUSEHOLD_HEADING.matches(trimmed) || HEALTH_HEADING.matches(trimmed)
            ) {
                i++
                while (i < lines.size && !TOP_HEADING.matches(lines[i].trim())) i++
                continue
            }

            if (trimmed == "---" &&
                i + 1 < lines.size && TODO_HEADING.matches(lines[i + 1].trim())
            ) {
                if (!healthInjected && healthData != null && healthData.hasAny) {
                    out.addAll(buildHealthSection(healthData))
                    healthInjected = true
                }
                val openIdx = i
                val headIdx = i + 1
                var j = i + 2
                val kept = mutableListOf<String>()
                while (j < lines.size && lines[j].trim() != "---") {
                    if (!TODO_ITEM_DONE.matches(lines[j].trim())) {
                        kept.add(lines[j])
                    }
                    j++
                }
                val hasClose = j < lines.size
                val hasAnyItem = kept.any { TODO_ITEM_ANY.matches(it.trim()) }
                if (hasAnyItem) {
                    out.add(lines[openIdx])
                    out.add(lines[headIdx])
                    out.addAll(kept)
                    if (hasClose) out.add(lines[j])
                }
                i = if (hasClose) j + 1 else j
                continue
            }

            if (HABIT_HEADING.matches(trimmed)) {
                out.add(line)
                i++
                while (i < lines.size && !TOP_HEADING.matches(lines[i].trim())) {
                    val l = lines[i]
                    if (TODO_ITEM_DONE.matches(l.trim())) {
                        out.add(l.replaceFirst(CHECKED_MARK, "[ ]"))
                    } else {
                        out.add(l)
                    }
                    i++
                }
                continue
            }

            out.add(line)
            i++
        }

        if (!healthInjected && healthData != null && healthData.hasAny) {
            if (out.isNotEmpty() && out.last().isNotBlank()) out.add("")
            out.addAll(buildHealthSection(healthData))
        }

        return out.joinToString("\n")
    }

    private fun buildHealthSection(data: HealthData): List<String> {
        val section = mutableListOf("# Health", "")
        data.sleep?.let { section.add("- 睡眠: ${formatSleep(it)}") }
        data.steps?.takeIf { it > 0 }?.let { section.add("- 歩数: $it steps") }
        section.add("")
        return section
    }

    private fun formatSleep(duration: Duration): String {
        val hours = duration.toHours()
        val minutes = (duration.toMinutes() % 60)
        return "${hours}h${minutes}m"
    }
}
