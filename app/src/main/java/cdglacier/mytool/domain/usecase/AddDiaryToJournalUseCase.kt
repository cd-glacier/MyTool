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
    suspend operator fun invoke(
        journalDirUri: String,
        date: LocalDate,
        filenameFormat: String,
        timestamp: String,
        content: String,
    ): Result<Unit> = runCatching {
        val original = journalRepository.readContent(journalDirUri, date, filenameFormat) ?: ""
        val entryBlock = buildEntryBlock(timestamp, content).joinToString("\n")
        val updated = JournalSectionWriter.write(original, JournalSection.DIARY, entryBlock)
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
}
