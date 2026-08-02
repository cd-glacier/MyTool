package cdglacier.mytool.data.repository

import cdglacier.mytool.domain.model.DailyHealth
import cdglacier.mytool.domain.model.HealthBook
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * pages/Health.md の Markdown シリアライザ/パーサ。
 * 行=日付、列=指標の 1 テーブル形式。
 */
object HealthMarkdown {

    private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private data class Column(
        val header: String,
        val get: (DailyHealth) -> String,
        val set: (DailyHealth, String) -> DailyHealth,
    )

    private val COLUMNS: List<Column> = listOf(
        col("Steps", { it.steps?.toString().orEmpty() }, { d, v -> d.copy(steps = v.toLongOrNull()) }),
        col("Dist_m", { it.distanceMeters?.let { m -> fmt(m) }.orEmpty() }, { d, v -> d.copy(distanceMeters = v.toDoubleOrNull()) }),
        col("ActMin", { it.activeMinutes?.toString().orEmpty() }, { d, v -> d.copy(activeMinutes = v.toLongOrNull()) }),
        col("ActKcal", { it.activeCalories?.let { c -> fmt(c) }.orEmpty() }, { d, v -> d.copy(activeCalories = v.toDoubleOrNull()) }),
        col("TotalKcal", { it.totalCalories?.let { c -> fmt(c) }.orEmpty() }, { d, v -> d.copy(totalCalories = v.toDoubleOrNull()) }),
        col("HrAvg", { it.heartRateAvg?.toString().orEmpty() }, { d, v -> d.copy(heartRateAvg = v.toLongOrNull()) }),
        col("HrMin", { it.heartRateMin?.toString().orEmpty() }, { d, v -> d.copy(heartRateMin = v.toLongOrNull()) }),
        col("HrMax", { it.heartRateMax?.toString().orEmpty() }, { d, v -> d.copy(heartRateMax = v.toLongOrNull()) }),
        col("RestHr", { it.restingHeartRate?.toString().orEmpty() }, { d, v -> d.copy(restingHeartRate = v.toLongOrNull()) }),
        col("SleepMin", { it.sleepMinutes?.toString().orEmpty() }, { d, v -> d.copy(sleepMinutes = v.toLongOrNull()) }),
        col("Sys", { it.bloodPressureSystolic?.let { x -> fmt(x) }.orEmpty() }, { d, v -> d.copy(bloodPressureSystolic = v.toDoubleOrNull()) }),
        col("Dia", { it.bloodPressureDiastolic?.let { x -> fmt(x) }.orEmpty() }, { d, v -> d.copy(bloodPressureDiastolic = v.toDoubleOrNull()) }),
        col("Glucose", { it.bloodGlucose?.let { x -> fmt(x) }.orEmpty() }, { d, v -> d.copy(bloodGlucose = v.toDoubleOrNull()) }),
        col("SpO2", { it.spo2?.let { x -> fmt(x) }.orEmpty() }, { d, v -> d.copy(spo2 = v.toDoubleOrNull()) }),
        col("Temp", { it.bodyTemperature?.let { x -> fmt(x) }.orEmpty() }, { d, v -> d.copy(bodyTemperature = v.toDoubleOrNull()) }),
        col("RespRate", { it.respiratoryRate?.let { x -> fmt(x) }.orEmpty() }, { d, v -> d.copy(respiratoryRate = v.toDoubleOrNull()) }),
    )

    private fun col(h: String, g: (DailyHealth) -> String, s: (DailyHealth, String) -> DailyHealth) = Column(h, g, s)

    private fun fmt(d: Double): String {
        if (d.isNaN() || d.isInfinite()) return ""
        return if (d == d.toLong().toDouble()) d.toLong().toString() else "%.2f".format(d)
    }

    fun serialize(book: HealthBook): String = buildString {
        appendLine("# Health")
        appendLine()
        appendLine("## Daily")
        appendLine("| Date | ${COLUMNS.joinToString(" | ") { it.header }} |")
        appendLine("|" + "---|".repeat(COLUMNS.size + 1))
        val sorted = book.days.keys.sortedDescending()
        for (date in sorted) {
            val day = book.days[date] ?: continue
            val cells = COLUMNS.map { it.get(day) }
            appendLine("| ${date.format(DATE_FMT)} | ${cells.joinToString(" | ")} |")
        }
    }

    fun parse(text: String): HealthBook {
        val lines = text.lines()
        val days = mutableMapOf<LocalDate, DailyHealth>()
        var inDaily = false
        var order: List<Int> = emptyList() // maps table-column index -> COLUMNS index
        var awaitingHeader = false

        for (raw in lines) {
            val line = raw.trim()
            if (line.startsWith("## ")) {
                inDaily = line.removePrefix("## ").trim().equals("Daily", ignoreCase = true)
                order = emptyList()
                awaitingHeader = inDaily
                continue
            }
            if (!inDaily) continue
            if (!line.startsWith("|") || line.startsWith("|---")) continue
            val cells = line.trim('|').split("|").map { it.trim() }
            if (awaitingHeader) {
                val headerCells = cells.drop(1)
                order = headerCells.map { h ->
                    COLUMNS.indexOfFirst { it.header.equals(h, ignoreCase = true) }
                }
                awaitingHeader = false
                continue
            }
            val date = runCatching { LocalDate.parse(cells[0], DATE_FMT) }.getOrNull() ?: continue
            var day = DailyHealth(date)
            val values = cells.drop(1)
            values.forEachIndexed { idx, v ->
                val colIdx = order.getOrNull(idx) ?: -1
                if (colIdx >= 0 && v.isNotBlank()) {
                    day = COLUMNS[colIdx].set(day, v)
                }
            }
            days[date] = day
        }
        return HealthBook(days = days)
    }
}
