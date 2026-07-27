package cdglacier.mytool.domain.usecase

import cdglacier.mytool.data.db.JournalLocationEntity
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object PositionTrackingParser {
    private val SECTION_HEADING = Regex("""^#{1,6}\s+Position\s+Tracking\s*$""")
    private val ANY_HEADING = Regex("""^#{1,6}\s+.*$""")
    private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    fun parse(date: LocalDate, markdown: String): List<JournalLocationEntity> {
        val zone = ZoneId.systemDefault()
        val lines = markdown.lines()
        val result = mutableListOf<JournalLocationEntity>()

        var i = 0
        while (i < lines.size) {
            if (SECTION_HEADING.matches(lines[i].trim())) {
                i++
                while (i < lines.size) {
                    val trimmed = lines[i].trim()
                    if (ANY_HEADING.matches(trimmed)) break
                    parseRow(trimmed)?.let { row ->
                        val time = runCatching { LocalTime.parse(row.time, TIME_FMT) }.getOrNull()
                        if (time != null) {
                            val timestamp = date.atTime(time).atZone(zone).toInstant().toEpochMilli()
                            result.add(
                                JournalLocationEntity(
                                    date = date.toString(),
                                    timestamp = timestamp,
                                    latitude = row.lat,
                                    longitude = row.lon,
                                    accuracy = row.accuracy,
                                    batteryLevel = row.battery,
                                    sameLocationCount = row.stayCount,
                                )
                            )
                        }
                    }
                    i++
                }
                continue
            }
            i++
        }
        return result
    }

    private data class Row(
        val time: String,
        val lat: Double,
        val lon: Double,
        val accuracy: Float,
        val stayCount: Int,
        val battery: Int,
    )

    private fun parseRow(line: String): Row? {
        if (!line.startsWith("|") || !line.endsWith("|")) return null
        val cells = line.trim('|').split("|").map { it.trim() }
        if (cells.size < 6) return null
        if (cells[0].contains(":--") || cells[0] == "時刻") return null
        val lat = cells[1].toDoubleOrNull() ?: return null
        val lon = cells[2].toDoubleOrNull() ?: return null
        val accuracy = cells[3].removeSuffix("m").trim().toFloatOrNull() ?: return null
        val stayCount = cells[4].toIntOrNull() ?: return null
        val battery = cells[5].removeSuffix("%").trim().toIntOrNull() ?: return null
        return Row(
            time = cells[0],
            lat = lat,
            lon = lon,
            accuracy = accuracy,
            stayCount = stayCount,
            battery = battery,
        )
    }
}
