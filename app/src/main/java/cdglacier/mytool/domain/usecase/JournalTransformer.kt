package cdglacier.mytool.domain.usecase

object JournalTransformer {
    private val TODO_ITEM_DONE = Regex("""^-\s+\[x]\s+.+$""")
    private val TODO_ITEM_ANY = Regex("""^-\s+\[( |x)]\s+.+$""")
    private val HABIT_HEADING = Regex("""^#\s+Habit\s*$""")
    private val POSITION_HEADING = Regex("""^#\s+Position Tracking\s*$""")
    private val TODO_HEADING = Regex("""^#\s+TODO\s*$""")
    private val TOP_HEADING = Regex("""^#\s+.+$""")
    private val CHECKED_MARK = Regex("""\[x]""")

    fun transform(markdown: String): String {
        val lines = markdown.lines()
        val out = mutableListOf<String>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            if (POSITION_HEADING.matches(trimmed)) {
                i++
                while (i < lines.size && !TOP_HEADING.matches(lines[i].trim())) i++
                continue
            }

            if (trimmed == "---" &&
                i + 1 < lines.size && TODO_HEADING.matches(lines[i + 1].trim())
            ) {
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
        return out.joinToString("\n")
    }
}
