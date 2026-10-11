package cdglacier.mytool.domain.usecase

import cdglacier.mytool.data.repository.JournalRepository
import java.time.LocalDate
import javax.inject.Inject

class IsJournalCopiedUseCase @Inject constructor(
    private val journalRepository: JournalRepository,
) {
    suspend operator fun invoke(
        journalDirUri: String,
        targetDate: LocalDate,
        filenameFormat: String,
    ): Boolean {
        val content = journalRepository.readContent(journalDirUri, targetDate, filenameFormat)
            ?: return false
        return content.lines().any { JournalSection.HABIT_TRACKING.matchesHeading(it) }
    }
}
