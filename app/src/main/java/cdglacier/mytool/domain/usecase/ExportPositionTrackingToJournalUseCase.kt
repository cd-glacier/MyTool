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

        val section = buildSection(records)
        val current = journalRepository.readContent(journalDirUri, date, filenameFormat).orEmpty()
        val updated = replaceOrAppendSection(current, section)

        journalRepository.writeContent(journalDirUri, date, filenameFormat, updated).getOrThrow()
        records.size
    }

    private fun buildSection(records: List<LocationRecordEntity>): String {
        val timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss")
        val zone = ZoneId.systemDefault()
        val header = """
            $SECTION_HEADING

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
        return "$header\n$rows\n"
    }

    private fun replaceOrAppendSection(content: String, section: String): String {
        val lines = content.lines()
        val startIdx = lines.indexOfFirst { it.trimEnd() == SECTION_HEADING }
        if (startIdx < 0) {
            val sep = if (content.isEmpty() || content.endsWith("\n\n")) "" else if (content.endsWith("\n")) "\n" else "\n\n"
            return content + sep + section
        }
        val endIdx = (startIdx + 1 until lines.size)
            .firstOrNull { lines[it].startsWith("# ") }
            ?: lines.size
        val before = lines.subList(0, startIdx).joinToString("\n")
        val after = lines.subList(endIdx, lines.size).joinToString("\n")
        return buildString {
            if (before.isNotEmpty()) {
                append(before)
                if (!before.endsWith("\n")) append("\n")
            }
            append(section)
            if (after.isNotEmpty()) {
                if (!section.endsWith("\n")) append("\n")
                append(after)
            }
        }
    }

    companion object {
        private const val SECTION_HEADING = "# Position Tracking"
    }
}
