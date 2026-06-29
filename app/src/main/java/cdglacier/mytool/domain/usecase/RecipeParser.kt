package cdglacier.mytool.domain.usecase

import cdglacier.mytool.domain.model.Recipe

object RecipeParser {
    private val RECIPE_HEADING = Regex("""^\[\[Recipe]]\s*$""")
    private val OTHER_WIKILINK = Regex("""^\[\[[^\[\]]+]]\s*$""")
    private val ITEM = Regex("""^-\s+\[(.+?)]\((https?://\S+?)\)\s*$""")

    fun parse(markdown: String): List<Recipe> {
        val lines = markdown.lines()
        val result = mutableListOf<Recipe>()

        var i = 0
        while (i < lines.size) {
            if (RECIPE_HEADING.matches(lines[i].trim())) {
                i++
                while (i < lines.size) {
                    val trimmed = lines[i].trim()
                    if (trimmed.startsWith("#")) break
                    if (OTHER_WIKILINK.matches(trimmed)) break
                    val item = ITEM.matchEntire(trimmed)
                    if (item != null) {
                        result.add(
                            Recipe(
                                title = item.groupValues[1].trim(),
                                url = item.groupValues[2].trim(),
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
}
