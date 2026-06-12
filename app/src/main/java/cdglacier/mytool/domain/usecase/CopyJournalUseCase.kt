package cdglacier.mytool.domain.usecase

import android.net.Uri
import cdglacier.mytool.data.repository.JournalRepository
import java.time.LocalDate
import javax.inject.Inject

class CopyJournalUseCase @Inject constructor(
    private val journalRepository: JournalRepository,
) {
    suspend operator fun invoke(
        journalDirUri: Uri,
        sourceDate: LocalDate,
        targetDate: LocalDate,
        filenameFormat: String,
    ): Result<Unit> =
        journalRepository.copy(journalDirUri, sourceDate, targetDate, filenameFormat)
}
