package cdglacier.mytool.widget

import kotlinx.serialization.Serializable

@Serializable
data class TodoItem(val text: String, val isDone: Boolean)

object TodoParser {
    private val TODO_ITEM_REGEX = Regex("""^-\s+\[( |x)]\s+(.+)$""")

    fun parse(markdown: String): List<TodoItem> {
        val lines = markdown.lines()
        val result = mutableListOf<TodoItem>()

        var i = 0
        while (i < lines.size) {
            // Look for "---" separator
            if (lines[i].trim() == "---") {
                // Check if next non-empty line is "# TODO"
                val nextIdx = i + 1
                if (nextIdx < lines.size && lines[nextIdx].trim() == "# TODO") {
                    // We're in a TODO block, parse until next "---"
                    i = nextIdx + 1
                    while (i < lines.size && lines[i].trim() != "---") {
                        val match = TODO_ITEM_REGEX.matchEntire(lines[i].trim())
                        if (match != null) {
                            val isDone = match.groupValues[1] == "x"
                            val text = match.groupValues[2]
                            result.add(TodoItem(text, isDone))
                        }
                        i++
                    }
                    // Skip the closing "---"
                    if (i < lines.size) i++
                    continue
                }
            }
            i++
        }

        return result
    }
}
