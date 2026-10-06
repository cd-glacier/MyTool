package cdglacier.mytool.domain.usecase

import cdglacier.mytool.data.repository.JournalRepository
import cdglacier.mytool.data.repository.DiaryRepository
import cdglacier.mytool.domain.model.Diary
import java.time.LocalDate
import javax.inject.Inject

class AddDiaryToJournalUseCase @Inject constructor(
    private val journalRepository: JournalRepository,
    private val diaryRepository: DiaryRepository,
) {
    private val diaryHeading = Regex("""^#{1,6}\s+\[\[Diary]]\s*$""")
    private val anyHeading = Regex("""^#{1,6}\s+.*$""")

    suspend operator fun invoke(
        journalDirUri: String,
        date: LocalDate,
        filenameFormat: String,
        timestamp: String,
        content: String,
    ): Result<Unit> = runCatching {
        val original = journalRepository.readContent(journalDirUri, date, filenameFormat) ?: ""
        val entryBlock = buildEntryBlock(timestamp, content)
        val updated = insertOrAppend(original, entryBlock)
        journalRepository.writeContent(journalDirUri, date, filenameFormat, updated).getOrThrow()
        val existing = diaryRepository.getByDate(date)
            .map { Diary(timestamp = it.timestamp, content = it.content) }
        diaryRepository.replaceForDate(date, existing + Diary(timestamp = timestamp, content = content))
    }

    private fun buildEntryBlock(timestamp: String, content: String): List<String> {
        val lines = content.lines()
        val head = "- $timestamp ${lines.firstOrNull().orEmpty()}"
        val tail = lines.drop(1).map { "  $it" }
        return listOf(head) + tail
    }

    private fun insertOrAppend(content: String, entryLines: List<String>): String {
        val lines = content.lines().toMutableList()
        val headingIndex = lines.indexOfFirst { diaryHeading.matches(it.trim()) }
        if (headingIndex < 0) {
            val prefix = if (content.isEmpty()) "" else if (content.endsWith("\n")) "\n" else "\n\n"
            return content + prefix + "# [[Diary]]\n" + entryLines.joinToString("\n") + "\n"
        }
        var insertAt = lines.size
        for (i in (headingIndex + 1) until lines.size) {
            if (anyHeading.matches(lines[i].trim())) {
                insertAt = i
                break
            }
        }
        while (insertAt > headingIndex + 1 && lines[insertAt - 1].isBlank()) insertAt--
        lines.addAll(insertAt, entryLines)
        return lines.joinToString("\n")
    }
}
