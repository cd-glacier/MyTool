package cdglacier.mytool.domain.usecase

import cdglacier.mytool.data.db.JournalLocationEntity
import cdglacier.mytool.data.repository.JournalLocationRepository
import cdglacier.mytool.data.repository.JournalRepository
import javax.inject.Inject

class ScanJournalLocationsUseCase @Inject constructor(
    private val journalRepository: JournalRepository,
    private val journalLocationRepository: JournalLocationRepository,
) {
    /**
     * journal を新しい日付順に走査し、Position Tracking セクションを DB に保存する。
     * 個別ファイル読み取りの失敗は無視する。
     */
    suspend operator fun invoke(journalDirUri: String, filenameFormat: String) {
        val dates = runCatching {
            journalRepository.listJournalDates(journalDirUri, filenameFormat)
        }.getOrDefault(emptyList()).sortedDescending()

        for (date in dates) {
            runCatching {
                val content = journalRepository.readContent(journalDirUri, date, filenameFormat)
                    ?: return@runCatching
                val parsed = PositionTrackingParser.parse(date, content)
                val existing = journalLocationRepository.getByDate(date)
                if (!sameContent(existing, parsed)) {
                    journalLocationRepository.replaceForDate(date, parsed)
                }
            }
        }
    }

    private fun sameContent(a: List<JournalLocationEntity>, b: List<JournalLocationEntity>): Boolean {
        if (a.size != b.size) return false
        val sortedA = a.sortedBy { it.timestamp }
        val sortedB = b.sortedBy { it.timestamp }
        return sortedA.zip(sortedB).all { (x, y) ->
            x.timestamp == y.timestamp &&
                x.latitude == y.latitude &&
                x.longitude == y.longitude
        }
    }
}
