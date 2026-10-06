package cdglacier.mytool.domain.usecase

import cdglacier.mytool.data.db.LocationRecordEntity
import cdglacier.mytool.data.repository.JournalRepository
import cdglacier.mytool.data.repository.LocationRecordRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ExportPositionTrackingToJournalUseCase @Inject constructor(
    private val locationRecordRepository: LocationRecordRepository,
    private val journalRepository: JournalRepository,
) {
    suspend operator fun invoke(
        journalDirUri: String,
        date: LocalDate,
        filenameFormat: String,
    ): Result<Int> = runCatching {
        val records = locationRecordRepository.observeByDate(date).first()
        if (records.isEmpty()) error("対象日の位置情報がありません")

        val body = buildSectionBody(records)
        val current = journalRepository.readContent(journalDirUri, date, filenameFormat).orEmpty()
        val updated = JournalSectionWriter.write(current, JournalSection.POSITION_TRACKING, body)

        journalRepository.writeContent(journalDirUri, date, filenameFormat, updated).getOrThrow()
        records.size
    }

    private fun buildSectionBody(records: List<LocationRecordEntity>): String {
        val timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss")
        val zone = ZoneId.systemDefault()
        val header = """
            |時刻|緯度|経度|精度|Stay Count|電池残量|
            |:--:|:--:|:--:|:--:|:--:|:--:|
        """.trimIndent()
        val rows = records.joinToString("\n") { r ->
            val time = Instant.ofEpochMilli(r.timestamp).atZone(zone).toLocalTime().format(timeFmt)
            val lat = "%.6f".format(r.latitude)
            val lon = "%.6f".format(r.longitude)
            val acc = "%.1f m".format(r.accuracy)
            "|$time|$lat|$lon|$acc|${r.sameLocationCount}|${r.batteryLevel}%|"
        }
        return "$header\n$rows"
    }
}
