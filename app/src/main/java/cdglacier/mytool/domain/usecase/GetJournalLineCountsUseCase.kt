package cdglacier.mytool.domain.usecase

import android.net.Uri
import cdglacier.mytool.data.repository.JournalRepository
import java.time.LocalDate
import javax.inject.Inject

class GetJournalLineCountsUseCase @Inject constructor(
    private val journalRepository: JournalRepository,
) {
    suspend operator fun invoke(
        journalDirUri: Uri,
        filenameFormat: String,
        dates: List<LocalDate>,
    ): Map<LocalDate, Int> =
        journalRepository.getLineCounts(journalDirUri, dates, filenameFormat)
}
