package cdglacier.mytool.domain.usecase

import android.location.Location
import cdglacier.mytool.data.db.LocationRecordEntity
import cdglacier.mytool.data.repository.LocationRecordRepository
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class DailyActivity(
    val habitRate: Float?,
    val distanceMeters: Double,
    val activityRate: Float?,
)

class GetActivityRatesUseCase @Inject constructor(
    private val locationRecordRepository: LocationRecordRepository,
) {
    suspend operator fun invoke(
        history: Map<LocalDate, Float>,
        todayRate: Float?,
        householdHusbandPoints: Map<LocalDate, Int>,
        from: LocalDate,
        toInclusive: LocalDate,
    ): Map<LocalDate, DailyActivity> {
        val habitCompletionRates: Map<LocalDate, Float?> = buildMap {
            putAll(history)
            remove(toInclusive)
            if (todayRate != null) put(toInclusive, todayRate)
        }
        val distances = dailyDistancesMeters(from, toInclusive)
        val keys = habitCompletionRates.keys + distances.keys + householdHusbandPoints.keys
        return keys.associateWith { date ->
            val habit = habitCompletionRates[date]
            val distance = distances[date] ?: 0.0
            val distanceRatio = (distance / OSAKA_TOKYO_DISTANCE_METERS)
                .coerceIn(0.0, 1.0)
                .toFloat()
            val household = householdHusbandPoints[date]
            val householdRatio = ((household ?: 0).toFloat() / HOUSEHOLD_MAX_POINTS)
                .coerceIn(0f, 1f)
            val activity = if (habit == null && distance <= 0.0 && household == null) null
            else (habit ?: 0f) * HABIT_WEIGHT +
                distanceRatio * DISTANCE_WEIGHT +
                householdRatio * HOUSEHOLD_WEIGHT
            DailyActivity(habit, distance, activity)
        }
    }

    private suspend fun dailyDistancesMeters(
        from: LocalDate,
        toInclusive: LocalDate,
    ): Map<LocalDate, Double> {
        val records = locationRecordRepository.getBetweenDates(from, toInclusive)
        if (records.isEmpty()) return emptyMap()
        val zone = ZoneId.systemDefault()
        val byDate = records.groupBy { record ->
            java.time.Instant.ofEpochMilli(record.timestamp).atZone(zone).toLocalDate()
        }
        return byDate.mapValues { (_, list) -> totalDistanceMeters(list) }
    }

    private fun totalDistanceMeters(records: List<LocationRecordEntity>): Double {
        if (records.size < 2) return 0.0
        var total = 0.0
        val buffer = FloatArray(1)
        for (i in 1 until records.size) {
            val a = records[i - 1]
            val b = records[i]
            Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, buffer)
            total += buffer[0]
        }
        return total
    }

    companion object {
        const val OSAKA_TOKYO_DISTANCE_METERS = 400_000.0
        const val HOUSEHOLD_MAX_POINTS = 50f
        private const val HABIT_WEIGHT = 1f / 4f
        private const val DISTANCE_WEIGHT = 2f / 4f
        private const val HOUSEHOLD_WEIGHT = 1f / 4f
    }
}
