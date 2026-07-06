package cdglacier.mytool.domain.usecase

import cdglacier.mytool.domain.model.DailySummary
import java.time.LocalDate
import javax.inject.Inject

data class DailyActivity(
    val habitRate: Float?,
    val distanceMeters: Double,
    val householdHusbandPoints: Int,
    val activityRate: Float?,
)

class GetActivityRatesUseCase @Inject constructor() {
    operator fun invoke(summaries: Map<LocalDate, DailySummary>): Map<LocalDate, DailyActivity> {
        return summaries.mapValues { (_, s) ->
            val distanceRatio = (s.distanceMeters / OSAKA_TOKYO_DISTANCE_METERS)
                .coerceIn(0.0, 1.0)
                .toFloat()
            val householdRatio = (s.householdHusbandPoints.toFloat() / HOUSEHOLD_MAX_POINTS)
                .coerceIn(0f, 1f)
            val activity =
                if (s.habitRate == null && s.distanceMeters <= 0.0 && s.householdHusbandPoints == 0) null
                else (s.habitRate ?: 0f) * HABIT_WEIGHT +
                    distanceRatio * DISTANCE_WEIGHT +
                    householdRatio * HOUSEHOLD_WEIGHT
            DailyActivity(s.habitRate, s.distanceMeters, s.householdHusbandPoints, activity)
        }
    }

    companion object {
        const val OSAKA_TOKYO_DISTANCE_METERS = 400_000.0
        const val HOUSEHOLD_MAX_POINTS = 50f
        private const val HABIT_WEIGHT = 1f / 4f
        private const val DISTANCE_WEIGHT = 2f / 4f
        private const val HOUSEHOLD_WEIGHT = 1f / 4f
    }
}
