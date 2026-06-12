package cdglacier.mytool.domain.usecase

import cdglacier.mytool.data.repository.JournalRepository
import java.time.LocalDate
import javax.inject.Inject

class CheckJournalTargetUseCase @Inject constructor(
    private val journalRepository: JournalRepository,
) {
    suspend operator fun invoke(
        journalDirUri: String,
        targetDate: LocalDate,
        filenameFormat: String,
    ): Boolean = journalRepository.exists(journalDirUri, targetDate, filenameFormat)
}
