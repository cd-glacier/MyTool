package cdglacier.mytool.domain.usecase

import android.location.Location
import cdglacier.mytool.data.db.LocationRecordEntity
import cdglacier.mytool.data.repository.LocationRecordRepository
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class GetActivityRatesUseCase @Inject constructor(
    private val locationRecordRepository: LocationRecordRepository,
) {
    suspend operator fun invoke(
        habitCompletionRates: Map<LocalDate, Float?>,
        from: LocalDate,
        toInclusive: LocalDate,
    ): Map<LocalDate, Float?> {
        val distances = dailyDistancesMeters(from, toInclusive)
        val keys = habitCompletionRates.keys + distances.keys
        return keys.associateWith { date ->
            val habit = habitCompletionRates[date]
            val distance = distances[date] ?: 0.0
            val distanceRatio = (distance / OSAKA_TOKYO_DISTANCE_METERS)
                .coerceIn(0.0, 1.0)
                .toFloat()
            if (habit == null && distance <= 0.0) null
            else (habit ?: 0f) * HABIT_WEIGHT + distanceRatio * DISTANCE_WEIGHT
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
        private const val HABIT_WEIGHT = 1f / 3f
        private const val DISTANCE_WEIGHT = 2f / 3f
    }
}
