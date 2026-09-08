package cdglacier.mytool.data.repository

import cdglacier.mytool.domain.model.SleepBook
import cdglacier.mytool.domain.model.SleepDay
import cdglacier.mytool.domain.model.SleepSession
import cdglacier.mytool.domain.model.SleepStage
import cdglacier.mytool.domain.model.SleepStageType
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * pages/SleepStages.md の Markdown シリアライザ/パーサ。
 * 1 行 = 1 ステージ。同一セッション/日でグルーピングして復元する。
 */
object SleepMarkdown {

    private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val HEADER = listOf("Date", "SessionStart", "SessionEnd", "Stage", "StageStart", "StageEnd")

    fun serialize(book: SleepBook): String = buildString {
        appendLine("# Sleep Stages")
        appendLine()
        appendLine("## Stages")
        appendLine("| ${HEADER.joinToString(" | ")} |")
        appendLine("|" + "---|".repeat(HEADER.size))
        val sorted = book.days.keys.sortedDescending()
        for (date in sorted) {
            val day = book.days[date] ?: continue
            for (session in day.sessions) {
                for (stage in session.stages) {
                    appendLine(
                        "| ${date.format(DATE_FMT)} | ${session.start} | ${session.end} | " +
                            "${stage.type.key} | ${stage.start} | ${stage.end} |"
                    )
                }
            }
        }
    }

    fun parse(text: String): SleepBook {
        val rows = mutableListOf<Row>()
        var inStages = false
        var order: Map<String, Int> = emptyMap()
        var awaitingHeader = false

        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.startsWith("## ")) {
                inStages = line.removePrefix("## ").trim().equals("Stages", ignoreCase = true)
                order = emptyMap()
                awaitingHeader = inStages
                continue
            }
            if (!inStages) continue
            if (!line.startsWith("|") || line.startsWith("|---")) continue
            val cells = line.trim('|').split("|").map { it.trim() }
            if (awaitingHeader) {
                order = cells.mapIndexedNotNull { i, h ->
                    HEADER.firstOrNull { it.equals(h, ignoreCase = true) }?.let { it to i }
                }.toMap()
                awaitingHeader = false
                continue
            }
            val row = parseRow(cells, order) ?: continue
            rows += row
        }

        val days = rows.groupBy { it.date }.mapValues { (date, dayRows) ->
            val sessions = dayRows.groupBy { it.sessionStart to it.sessionEnd }
                .map { (key, stageRows) ->
                    SleepSession(
                        start = key.first,
                        end = key.second,
                        stages = stageRows.map { SleepStage(it.stageType, it.stageStart, it.stageEnd) }
                            .sortedBy { it.start },
                    )
                }
                .sortedBy { it.start }
            SleepDay(date = date, sessions = sessions)
        }
        return SleepBook(days = days)
    }

    private fun parseRow(cells: List<String>, order: Map<String, Int>): Row? {
        fun get(name: String): String? = order[name]?.let { cells.getOrNull(it) }
        val date = runCatching { LocalDate.parse(get("Date"), DATE_FMT) }.getOrNull() ?: return null
        val sessionStart = runCatching { Instant.parse(get("SessionStart")) }.getOrNull() ?: return null
        val sessionEnd = runCatching { Instant.parse(get("SessionEnd")) }.getOrNull() ?: return null
        val stageType = SleepStageType.fromKey(get("Stage").orEmpty())
        val stageStart = runCatching { Instant.parse(get("StageStart")) }.getOrNull() ?: return null
        val stageEnd = runCatching { Instant.parse(get("StageEnd")) }.getOrNull() ?: return null
        return Row(date, sessionStart, sessionEnd, stageType, stageStart, stageEnd)
    }

    private data class Row(
        val date: LocalDate,
        val sessionStart: Instant,
        val sessionEnd: Instant,
        val stageType: SleepStageType,
        val stageStart: Instant,
        val stageEnd: Instant,
    )
}
