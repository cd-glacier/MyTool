package cdglacier.mytool.domain.usecase

import cdglacier.mytool.data.repository.JournalRepository
import java.time.LocalDate
import javax.inject.Inject

class AddRecipeToJournalUseCase @Inject constructor(
    private val journalRepository: JournalRepository,
) {
    private val recipeHeading = Regex("""^#{1,6}\s+\[\[Recipe]]\s*$""")
    private val anyHeading = Regex("""^#{1,6}\s+.*$""")

    suspend operator fun invoke(
        journalDirUri: String,
        date: LocalDate,
        filenameFormat: String,
        title: String,
        url: String,
    ): Result<Unit> = runCatching {
        val original = journalRepository.readContent(journalDirUri, date, filenameFormat) ?: ""
        val item = "- [${title.trim()}]($url)"
        val updated = insertOrAppend(original, item)
        journalRepository.writeContent(journalDirUri, date, filenameFormat, updated).getOrThrow()
    }

    private fun insertOrAppend(content: String, item: String): String {
        val lines = content.lines().toMutableList()
        val headingIndex = lines.indexOfFirst { recipeHeading.matches(it.trim()) }
        if (headingIndex < 0) {
            val needsBlank = content.isNotEmpty() && !content.endsWith("\n\n")
            val prefix = if (content.isEmpty()) "" else if (content.endsWith("\n")) "\n" else "\n\n"
            return content + prefix + "# [[Recipe]]\n" + item + "\n"
        }
        var insertAt = lines.size
        for (i in (headingIndex + 1) until lines.size) {
            if (anyHeading.matches(lines[i].trim())) {
                insertAt = i
                break
            }
        }
        while (insertAt > headingIndex + 1 && lines[insertAt - 1].isBlank()) insertAt--
        lines.add(insertAt, item)
        return lines.joinToString("\n")
    }
}
