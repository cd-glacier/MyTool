package cdglacier.mytool.domain.usecase

import cdglacier.mytool.data.db.DiaryEntity
import cdglacier.mytool.data.repository.DiaryRepository
import cdglacier.mytool.data.repository.JournalRepository
import javax.inject.Inject

class ScanDiariesUseCase @Inject constructor(
    private val journalRepository: JournalRepository,
    private val diaryRepository: DiaryRepository,
) {
    suspend operator fun invoke(journalDirUri: String, filenameFormat: String) {
        val dates = runCatching {
            journalRepository.listJournalDates(journalDirUri, filenameFormat)
        }.getOrDefault(emptyList()).sortedDescending()

        for (date in dates) {
            runCatching {
                val content = journalRepository.readContent(journalDirUri, date, filenameFormat)
                    ?: return@runCatching
                val parsed = DiaryParser.parse(content)
                val existing = diaryRepository.getByDate(date)
                if (!sameContent(existing, parsed)) {
                    diaryRepository.replaceForDate(date, parsed)
                }
            }
        }
    }

    private fun sameContent(
        existing: List<DiaryEntity>,
        parsed: List<cdglacier.mytool.domain.model.Diary>,
    ): Boolean {
        if (existing.size != parsed.size) return false
        val sortedExisting = existing.sortedBy { it.position }
        return sortedExisting.zip(parsed).all { (e, p) ->
            e.timestamp == p.timestamp && e.content == p.content
        }
    }
}
