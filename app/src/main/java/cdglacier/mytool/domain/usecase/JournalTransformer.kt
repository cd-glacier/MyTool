package cdglacier.mytool.domain.usecase

object JournalTransformer {
    private val TODO_ITEM_DONE = Regex("""^-\s+\[x]\s+.+$""")
    private val TODO_ITEM_ANY = Regex("""^-\s+\[( |x)]\s+.+$""")
    private val CHECKED_MARK = Regex("""\[x]""")

    fun transform(markdown: String): String {
        val lines = markdown.lines()
        val out = mutableListOf<String>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()
            val section = JournalSection.fromHeadingLine(line)

            // セクションごと削除する対象
            if (section != null && section in SECTIONS_TO_DROP) {
                i++
                while (i < lines.size && !JournalSection.isAnyTopHeading(lines[i])) i++
                continue
            }

            // TODO セクション（`---` で囲まれていない前提の旧形式に対応）
            if (trimmed == "---" &&
                i + 1 < lines.size && JournalSection.TODO.matchesHeading(lines[i + 1])
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
                    out.add(JournalSection.TODO.heading)
                    out.addAll(kept)
                    if (hasClose) out.add(lines[j])
                }
                i = if (hasClose) j + 1 else j
                continue
            }

            // HabitTracking: チェック解除して引き継ぎ
            if (section == JournalSection.HABIT_TRACKING) {
                out.add(JournalSection.HABIT_TRACKING.heading)
                i++
                while (i < lines.size && !JournalSection.isAnyTopHeading(lines[i])) {
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

    private val SECTIONS_TO_DROP = setOf(
        JournalSection.RECIPE,
        JournalSection.DIARY,
        JournalSection.POSITION_TRACKING,
        JournalSection.HOUSEHOLD,
        JournalSection.HEALTH,
    )
}
