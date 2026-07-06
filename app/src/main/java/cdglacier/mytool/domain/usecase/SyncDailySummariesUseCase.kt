package cdglacier.mytool.domain.usecase

import android.location.Location
import cdglacier.mytool.data.db.LocationRecordEntity
import cdglacier.mytool.data.repository.DailySummaryRepository
import cdglacier.mytool.data.repository.JournalRepository
import cdglacier.mytool.data.repository.LocationRecordRepository
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.domain.model.Assignee
import cdglacier.mytool.domain.model.DailySummary
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * 指定範囲の日次サマリを Room キャッシュに揃える。
 * - デフォルトでは Room に存在しない日付のみを取得。
 * - 当日は常に再計算して上書き。
 * - forceAll=true の場合、範囲内すべてを再取得。
 */
class SyncDailySummariesUseCase @Inject constructor(
    private val obsidianRepository: ObsidianRepository,
    private val journalRepository: JournalRepository,
    private val locationRecordRepository: LocationRecordRepository,
    private val getHouseholdPointsUseCase: GetHouseholdPointsUseCase,
    private val dailySummaryRepository: DailySummaryRepository,
) {
    suspend operator fun invoke(
        dateRange: ClosedRange<LocalDate>,
        forceAll: Boolean = false,
    ): Result<Int> = runCatching {
        val journalUri = obsidianRepository.journalDirUri.first()?.toString() ?: return@runCatching 0
        val format = obsidianRepository.filenameFormat.first()
        val today = LocalDate.now()

        val allDates = generateSequence(dateRange.start) { d ->
            val next = d.plusDays(1); if (next > dateRange.endInclusive) null else next
        }.toList()
        val existing = if (forceAll) emptySet()
        else dailySummaryRepository.getExistingDates(allDates)
        val targets = allDates.filter { forceAll || it !in existing || it == today }
        if (targets.isEmpty()) return@runCatching 0

        val distances = distancesByDate(dateRange.start, dateRange.endInclusive)
        val pointsMap = runCatching { getHouseholdPointsUseCase().associate { it.name to it.points } }
            .getOrDefault(emptyMap())

        val summaries = targets.map { date ->
            val content = journalRepository.readContent(journalUri, date, format).orEmpty()
            val habitRate = if (content.isEmpty()) null else {
                val applicable = HabitParser.parse(content).filter { it.appliesTo(date.dayOfWeek) }
                if (applicable.isEmpty()) null
                else applicable.count { it.isCompleted }.toFloat() / applicable.size
            }
            val household = if (content.isEmpty()) 0 else HouseholdJournalParser.parse(content)
                .filter { it.assignee == Assignee.HUSBAND }
                .sumOf { (pointsMap[it.name] ?: 0) * it.count + it.adjustment }
            DailySummary(
                date = date,
                habitRate = habitRate,
                distanceMeters = distances[date] ?: 0.0,
                householdHusbandPoints = household,
            )
        }
        dailySummaryRepository.upsertAll(summaries, System.currentTimeMillis())
        summaries.size
    }

    private suspend fun distancesByDate(from: LocalDate, toInclusive: LocalDate): Map<LocalDate, Double> {
        val records = locationRecordRepository.getBetweenDates(from, toInclusive)
        if (records.isEmpty()) return emptyMap()
        val zone = ZoneId.systemDefault()
        val byDate = records.groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
        return byDate.mapValues { (_, list) -> totalDistanceMeters(list) }
    }

    private fun totalDistanceMeters(records: List<LocationRecordEntity>): Double {
        if (records.size < 2) return 0.0
        var total = 0.0
        val buffer = FloatArray(1)
        for (i in 1 until records.size) {
            Location.distanceBetween(
                records[i - 1].latitude, records[i - 1].longitude,
                records[i].latitude, records[i].longitude, buffer,
            )
            total += buffer[0]
        }
        return total
    }
}
