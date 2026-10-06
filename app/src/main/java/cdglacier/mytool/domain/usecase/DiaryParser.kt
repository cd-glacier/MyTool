package cdglacier.mytool.domain.usecase

import cdglacier.mytool.domain.model.Diary

object DiaryParser {
    private val DIARY_HEADING = Regex("""^#{1,6}\s+\[\[Diary]]\s*$""")
    private val ANY_HEADING = Regex("""^#{1,6}\s+.*$""")
    private val ENTRY = Regex("""^-\s+(\d{2}:\d{2})\s+(.*)$""")
    private val CONTINUATION = Regex("""^ {2}(.*)$""")

    fun parse(markdown: String): List<Diary> {
        val lines = markdown.lines()
        val result = mutableListOf<Diary>()

        var i = 0
        while (i < lines.size) {
            if (DIARY_HEADING.matches(lines[i].trim())) {
                i++
                var current: Pair<String, StringBuilder>? = null
                while (i < lines.size) {
                    val raw = lines[i]
                    if (ANY_HEADING.matches(raw.trim())) break
                    val entry = ENTRY.matchEntire(raw)
                    if (entry != null) {
                        current?.let { result.add(Diary(it.first, it.second.toString())) }
                        current = entry.groupValues[1] to StringBuilder(entry.groupValues[2])
                    } else {
                        val cont = CONTINUATION.matchEntire(raw)
                        if (cont != null && current != null) {
                            current.second.append('\n').append(cont.groupValues[1])
                        }
                    }
                    i++
                }
                current?.let { result.add(Diary(it.first, it.second.toString())) }
                continue
            }
            i++
        }
        return result
    }
}
