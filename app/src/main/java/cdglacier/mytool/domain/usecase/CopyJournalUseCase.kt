package cdglacier.mytool.domain.usecase

import cdglacier.mytool.data.repository.JournalRepository
import java.time.LocalDate
import javax.inject.Inject

class CopyJournalUseCase @Inject constructor(
    private val journalRepository: JournalRepository,
) {
    suspend operator fun invoke(
        journalDirUri: String,
        sourceDate: LocalDate,
        targetDate: LocalDate,
        filenameFormat: String,
    ): Result<Unit> =
        journalRepository.copy(journalDirUri, sourceDate, targetDate, filenameFormat)
}
