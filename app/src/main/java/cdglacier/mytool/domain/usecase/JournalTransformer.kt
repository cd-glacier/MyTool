package cdglacier.mytool.domain.usecase

/**
 * 翌日分の Journal を生成する際、管理ブロック `---` ... `---` の内側だけを
 * [JournalSection] ごとのルールで加工する。フリー領域は非破壊。
 */
object JournalTransformer {
    private val TODO_ITEM_DONE = Regex("""^-\s+\[x]\s+.+$""")
    private val TODO_ITEM_ANY = Regex("""^-\s+\[( |x)]\s+.+$""")
    private val CHECKED_MARK = Regex("""\[x]""")

    private val SECTIONS_TO_DROP = setOf(
        JournalSection.RECIPE,
        JournalSection.DIARY,
        JournalSection.POSITION_TRACKING,
        JournalSection.HOUSEHOLD,
        JournalSection.HEALTH,
    )

    fun transform(markdown: String): String {
        val lines = markdown.lines()
        val firstSep = lines.indexOfFirst { it.trim() == "---" }
        if (firstSep < 0) return markdown
        val secondSep = (firstSep + 1 until lines.size).firstOrNull { lines[it].trim() == "---" }
            ?: return markdown

        val before = lines.subList(0, firstSep)
        val managed = lines.subList(firstSep + 1, secondSep)
        val after = lines.subList(secondSep + 1, lines.size)

        val processed = processManagedBlock(managed)

        val sb = StringBuilder()
        if (before.isNotEmpty()) {
            sb.append(before.joinToString("\n")).append("\n")
        }
        sb.append("---\n")
        if (processed.isNotEmpty()) {
            sb.append(processed.joinToString("\n").trim('\n')).append("\n")
        }
        sb.append("---")
        if (after.isNotEmpty()) {
            sb.append("\n").append(after.joinToString("\n"))
        }
        return sb.toString()
    }

    private fun processManagedBlock(managed: List<String>): List<String> {
        val out = mutableListOf<String>()
        var i = 0
        while (i < managed.size) {
            val section = JournalSection.fromHeadingLine(managed[i])
            if (section == null) {
                out.add(managed[i])
                i++
                continue
            }
            val end = (i + 1 until managed.size)
                .firstOrNull { JournalSection.isAnyTopHeading(managed[it]) }
                ?: managed.size
            val body = managed.subList(i + 1, end)
            when {
                section in SECTIONS_TO_DROP -> {
                    // セクションごと削除。前の空行があれば一緒に落とす。
                    while (out.isNotEmpty() && out.last().isBlank()) out.removeAt(out.size - 1)
                }
                section == JournalSection.TODO -> {
                    val kept = body.filterNot { TODO_ITEM_DONE.matches(it.trim()) }
                    val hasAnyItem = kept.any { TODO_ITEM_ANY.matches(it.trim()) }
                    if (hasAnyItem) {
                        out.add(JournalSection.TODO.heading)
                        out.addAll(kept)
                    } else {
                        while (out.isNotEmpty() && out.last().isBlank()) out.removeAt(out.size - 1)
                    }
                }
                section == JournalSection.HABIT_TRACKING -> {
                    out.add(JournalSection.HABIT_TRACKING.heading)
                    body.forEach { l ->
                        out.add(
                            if (TODO_ITEM_DONE.matches(l.trim())) l.replaceFirst(CHECKED_MARK, "[ ]")
                            else l
                        )
                    }
                }
                else -> {
                    out.add(section.heading)
                    out.addAll(body)
                }
            }
            i = end
        }
        return out
    }
}
