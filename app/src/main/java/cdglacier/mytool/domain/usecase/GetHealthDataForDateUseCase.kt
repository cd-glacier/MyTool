package cdglacier.mytool.domain.usecase

import cdglacier.mytool.data.repository.HealthRepository
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class HealthData(
    val sleep: Duration? = null,
    val steps: Long? = null,
) {
    val hasAny: Boolean
        get() = sleep != null || (steps != null && steps > 0)
}

class GetHealthDataForDateUseCase @Inject constructor(
    private val healthRepository: HealthRepository,
) {
    suspend operator fun invoke(date: LocalDate): HealthData {
        if (!healthRepository.isAvailable) return HealthData()
        healthRepository.refreshPermissions()
        if (!healthRepository.permissionsGranted.value) return HealthData()

        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()

        val steps = healthRepository.getStepsBetween(start, end)
        val sleep = healthRepository.getSleepDurationBetween(start, end)
        return HealthData(sleep = sleep, steps = steps)
    }
}
