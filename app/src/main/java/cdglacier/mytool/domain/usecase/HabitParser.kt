package cdglacier.mytool.domain.usecase

import cdglacier.mytool.domain.model.Habit
import cdglacier.mytool.domain.model.HabitFrequency
import java.time.DayOfWeek

object HabitParser {
    private val HABIT_HEADING = Regex("""^#\s+Habit\s*$""")
    private val SUB_HEADING = Regex("""^##\s+(.+?)\s*$""")
    private val ANY_HEADING = Regex("""^#{1,6}\s+.*$""")
    private val ITEM = Regex("""^-\s+\[( |x)]\s+(.+)$""")

    private val DAY_OF_WEEK = mapOf(
        "monday" to DayOfWeek.MONDAY,
        "tuesday" to DayOfWeek.TUESDAY,
        "wednesday" to DayOfWeek.WEDNESDAY,
        "thursday" to DayOfWeek.THURSDAY,
        "friday" to DayOfWeek.FRIDAY,
        "saturday" to DayOfWeek.SATURDAY,
        "sunday" to DayOfWeek.SUNDAY,
    )

    fun parse(markdown: String): List<Habit> {
        val lines = markdown.lines()
        val result = mutableListOf<Habit>()

        var i = 0
        while (i < lines.size) {
            if (HABIT_HEADING.matches(lines[i].trim())) {
                i++
                var currentFrequency: HabitFrequency? = null
                while (i < lines.size) {
                    val raw = lines[i]
                    val trimmed = raw.trim()
                    if (trimmed.startsWith("# ") && !trimmed.startsWith("##")) break
                    val sub = SUB_HEADING.matchEntire(trimmed)
                    if (sub != null) {
                        currentFrequency = parseFrequency(sub.groupValues[1])
                        i++
                        continue
                    }
                    val item = ITEM.matchEntire(trimmed)
                    if (item != null && currentFrequency != null) {
                        result.add(
                            Habit(
                                name = item.groupValues[2].trim(),
                                frequency = currentFrequency,
                                isCompleted = item.groupValues[1] == "x",
                            )
                        )
                    }
                    i++
                }
                continue
            }
            i++
        }
        return result
    }

    private fun parseFrequency(text: String): HabitFrequency? {
        val normalized = text.trim().lowercase()
        if (normalized == "daily") return HabitFrequency.Daily
        val everyMatch = Regex("""^every\s+(\w+)$""").matchEntire(normalized) ?: return null
        val dow = DAY_OF_WEEK[everyMatch.groupValues[1]] ?: return null
        return HabitFrequency.Weekly(dow)
    }

    /**
     * 指定された習慣行のチェック状態をトグルした Markdown を返す。
     * 該当行が無ければ元の Markdown をそのまま返す。
     */
    fun toggle(markdown: String, habit: Habit): String {
        val lines = markdown.lines().toMutableList()
        var inHabit = false
        var currentFrequency: HabitFrequency? = null
        for (i in lines.indices) {
            val trimmed = lines[i].trim()
            if (HABIT_HEADING.matches(trimmed)) {
                inHabit = true
                currentFrequency = null
                continue
            }
            if (!inHabit) continue
            if (trimmed.startsWith("# ") && !trimmed.startsWith("##")) {
                inHabit = false
                continue
            }
            val sub = SUB_HEADING.matchEntire(trimmed)
            if (sub != null) {
                currentFrequency = parseFrequency(sub.groupValues[1])
                continue
            }
            val item = ITEM.matchEntire(trimmed)
            if (item != null && currentFrequency == habit.frequency &&
                item.groupValues[2].trim() == habit.name
            ) {
                val isDone = item.groupValues[1] == "x"
                val newMark = if (isDone) " " else "x"
                lines[i] = lines[i].replaceFirst(
                    Regex("""\[( |x)]"""),
                    "[$newMark]"
                )
                break
            }
        }
        return lines.joinToString("\n")
    }
}
