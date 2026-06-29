package cdglacier.mytool.widget

import kotlinx.serialization.Serializable

@Serializable
data class TodoItem(val text: String, val isDone: Boolean)

object TodoParser {
    private val TODO_ITEM_REGEX = Regex("""^-\s+\[( |x)]\s+(.+)$""")
    private val TOP_HEADING = Regex("""^#\s+.+$""")

    fun parse(markdown: String): List<TodoItem> {
        val lines = markdown.lines()
        val result = mutableListOf<TodoItem>()

        var i = 0
        while (i < lines.size) {
            if (lines[i].trim() == "# TODO") {
                i++
                while (i < lines.size &&
                    lines[i].trim() != "---" &&
                    !TOP_HEADING.matches(lines[i].trim())
                ) {
                    val match = TODO_ITEM_REGEX.matchEntire(lines[i].trim())
                    if (match != null) {
                        val isDone = match.groupValues[1] == "x"
                        val rawText = match.groupValues[2]
                        val text = rawText.replace(Regex("""\[\[([^\]]+)]]"""), "$1")
                        result.add(TodoItem(text, isDone))
                    }
                    i++
                }
                continue
            }
            i++
        }

        return result
    }
}
