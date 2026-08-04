package cdglacier.mytool.domain.usecase

import cdglacier.mytool.data.repository.JournalLocationRepository
import cdglacier.mytool.data.repository.LocationRecordRepository
import java.time.LocalDate
import javax.inject.Inject

class AutoExportPositionTrackingIfNeededUseCase @Inject constructor(
    private val locationRecordRepository: LocationRecordRepository,
    private val journalLocationRepository: JournalLocationRepository,
    private val exportPositionTrackingToJournalUseCase: ExportPositionTrackingToJournalUseCase,
) {
    suspend operator fun invoke(
        journalDirUri: String,
        date: LocalDate,
        filenameFormat: String,
    ): Result<Int?> = runCatching {
        val roomLatest = locationRecordRepository.getLatestOfDate(date)?.timestamp ?: return@runCatching null
        val journalLatest = journalLocationRepository.getByDate(date).maxOfOrNull { it.timestamp } ?: 0L
        if (roomLatest <= journalLatest) return@runCatching null
        exportPositionTrackingToJournalUseCase(journalDirUri, date, filenameFormat).getOrThrow()
    }
}
