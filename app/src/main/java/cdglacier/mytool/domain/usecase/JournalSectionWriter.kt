package cdglacier.mytool.domain.usecase

/**
 * 管理ブロック `---` ... `---` の内側に対して、[JournalSection] を正規順序で挿入／更新する。
 * フォーマット仕様は `.claude/rules/journal-format.md` を参照。
 */
object JournalSectionWriter {

    fun write(content: String, section: JournalSection, sectionBody: String): String {
        val normalizedBody = sectionBody.trimEnd('\n')
        val parts = splitByManagedBlock(content)
        val managedLines = parts.managed.toMutableList()

        val range = findSectionRange(managedLines, section)
        if (range != null) {
            applyToExisting(managedLines, section, normalizedBody, range)
        } else {
            insertNewSection(managedLines, section, normalizedBody)
        }

        return assemble(parts.before, managedLines, parts.after)
    }

    private data class SplitResult(
        val before: List<String>,
        val managed: List<String>,
        val after: List<String>,
    )

    private fun splitByManagedBlock(content: String): SplitResult {
        val lines = if (content.isEmpty()) emptyList() else content.lines()
        val firstSep = lines.indexOfFirst { it.trim() == "---" }
        if (firstSep < 0) {
            return SplitResult(before = lines, managed = emptyList(), after = emptyList())
        }
        val secondSep = (firstSep + 1 until lines.size).firstOrNull { lines[it].trim() == "---" }
        if (secondSep == null) {
            return SplitResult(
                before = lines.subList(0, firstSep),
                managed = lines.subList(firstSep + 1, lines.size),
                after = emptyList(),
            )
        }
        return SplitResult(
            before = lines.subList(0, firstSep),
            managed = lines.subList(firstSep + 1, secondSep),
            after = lines.subList(secondSep + 1, lines.size),
        )
    }

    private data class SectionRange(val headIdx: Int, val bodyEndExclusive: Int)

    private fun findSectionRange(managed: List<String>, section: JournalSection): SectionRange? {
        val headIdx = managed.indexOfFirst { section.matchesHeading(it) }
        if (headIdx < 0) return null
        val end = (headIdx + 1 until managed.size)
            .firstOrNull { JournalSection.isAnyTopHeading(managed[it]) }
            ?: managed.size
        return SectionRange(headIdx, end)
    }

    private fun applyToExisting(
        managed: MutableList<String>,
        section: JournalSection,
        sectionBody: String,
        range: SectionRange,
    ) {
        when (section.strategy) {
            JournalSection.WriteStrategy.REPLACE, JournalSection.WriteStrategy.CLEANUP_ONLY -> {
                val replacement = buildSectionLines(section, sectionBody)
                val removeCount = range.bodyEndExclusive - range.headIdx
                repeat(removeCount) { managed.removeAt(range.headIdx) }
                managed.addAll(range.headIdx, replacement)
            }
            JournalSection.WriteStrategy.APPEND -> {
                managed[range.headIdx] = section.heading
                val bodyLines = sectionBody.lines()
                val currentBody = managed.subList(range.headIdx + 1, range.bodyEndExclusive)
                val alreadyPresent = currentBody.joinToString("\n").contains(sectionBody)
                if (alreadyPresent) return
                var insertAt = range.bodyEndExclusive
                while (insertAt > range.headIdx + 1 && managed[insertAt - 1].isBlank()) insertAt--
                managed.addAll(insertAt, bodyLines)
            }
        }
    }

    private fun insertNewSection(
        managed: MutableList<String>,
        section: JournalSection,
        sectionBody: String,
    ) {
        val insertAt = findInsertPosition(managed, section)
        val block = buildSectionLines(section, sectionBody)
        val padded = buildList {
            if (insertAt > 0 && managed[insertAt - 1].isNotBlank()) add("")
            addAll(block)
            if (insertAt < managed.size && managed[insertAt].isNotBlank()) add("")
        }
        managed.addAll(insertAt, padded)
    }

    private fun findInsertPosition(managed: List<String>, section: JournalSection): Int {
        for (i in managed.indices) {
            val found = JournalSection.fromHeadingLine(managed[i]) ?: continue
            if (found.order > section.order) {
                var pos = i
                while (pos > 0 && managed[pos - 1].isBlank()) pos--
                return pos
            }
        }
        var pos = managed.size
        while (pos > 0 && managed[pos - 1].isBlank()) pos--
        return pos
    }

    private fun buildSectionLines(section: JournalSection, sectionBody: String): List<String> {
        val result = mutableListOf(section.heading, "")
        result.addAll(sectionBody.lines())
        return result
    }

    private fun assemble(before: List<String>, managed: List<String>, after: List<String>): String {
        val sb = StringBuilder()
        if (before.isNotEmpty()) {
            sb.append(before.joinToString("\n"))
            sb.append("\n")
        }
        sb.append("---\n")
        if (managed.isNotEmpty()) {
            sb.append(managed.joinToString("\n").trim('\n'))
            sb.append("\n")
        }
        sb.append("---")
        if (after.isNotEmpty()) {
            sb.append("\n")
            sb.append(after.joinToString("\n"))
        }
        return sb.toString()
    }
}
