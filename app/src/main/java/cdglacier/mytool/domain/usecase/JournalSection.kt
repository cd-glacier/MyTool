package cdglacier.mytool.domain.usecase

enum class JournalSection(
    val heading: String,
    val legacyHeadings: List<String>,
    val strategy: WriteStrategy,
    val order: Int,
) {
    TODO("# TODO", emptyList(), WriteStrategy.CLEANUP_ONLY, 1),
    HABIT_TRACKING("# HabitTracking", listOf("# Habit"), WriteStrategy.CLEANUP_ONLY, 2),
    RECIPE("# Recipe", listOf("# [[Recipe]]"), WriteStrategy.APPEND, 3),
    DIARY("# Diary", listOf("# [[Diary]]"), WriteStrategy.APPEND, 4),
    POSITION_TRACKING("# PositionTracking", listOf("# Position Tracking"), WriteStrategy.REPLACE, 5),
    HOUSEHOLD("# Household", emptyList(), WriteStrategy.REPLACE, 6),
    HEALTH("# Health", emptyList(), WriteStrategy.REPLACE, 7);

    enum class WriteStrategy { CLEANUP_ONLY, APPEND, REPLACE }

    fun matchesHeading(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed == heading) return true
        return legacyHeadings.any { it == trimmed }
    }

    companion object {
        private val TOP_HEADING = Regex("""^#\s+.+$""")

        fun fromHeadingLine(line: String): JournalSection? {
            val trimmed = line.trim()
            return values().firstOrNull { it.heading == trimmed || it.legacyHeadings.contains(trimmed) }
        }

        fun isAnyTopHeading(line: String): Boolean = TOP_HEADING.matches(line.trim())
    }
}
