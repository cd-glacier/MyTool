package cdglacier.mytool.widget

import cdglacier.mytool.domain.usecase.JournalSection
import kotlinx.serialization.Serializable

@Serializable
data class TodoItem(val text: String, val isDone: Boolean)

object TodoParser {
    private val TODO_ITEM_REGEX = Regex("""^-\s+\[( |x)]\s+(.+)$""")
    private val WIKI_LINK_REGEX = Regex("""\[\[([^\]]+)]]""")

    fun parse(markdown: String): List<TodoItem> {
        val lines = markdown.lines()
        val headIdx = lines.indexOfFirst { JournalSection.TODO.matchesHeading(it) }
        if (headIdx < 0) return emptyList()

        val result = mutableListOf<TodoItem>()
        var i = headIdx + 1
        while (i < lines.size) {
            val trimmed = lines[i].trim()
            if (trimmed == "---" || JournalSection.isAnyTopHeading(lines[i])) break
            val match = TODO_ITEM_REGEX.matchEntire(trimmed)
            if (match != null) {
                val isDone = match.groupValues[1] == "x"
                val text = WIKI_LINK_REGEX.replace(match.groupValues[2], "$1")
                result.add(TodoItem(text, isDone))
            }
            i++
        }
        return result
    }
}
