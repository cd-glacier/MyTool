package cdglacier.mytool.domain.usecase

import cdglacier.mytool.data.repository.HealthPageRepository
import cdglacier.mytool.data.repository.HealthRepository
import cdglacier.mytool.data.repository.SleepPageRepository
import cdglacier.mytool.domain.model.DailyHealth
import java.time.LocalDate
import javax.inject.Inject

/**
 * HealthConnect から指定日のデータを取得し、Health.md / SleepStages.md に upsert する。
 * Worker と UI 両方から使う。
 */
class SyncHealthDayUseCase @Inject constructor(
    private val healthRepository: HealthRepository,
    private val healthPageRepository: HealthPageRepository,
    private val sleepPageRepository: SleepPageRepository,
) {
    suspend operator fun invoke(date: LocalDate, overwrite: Boolean = false): Result<DailyHealth> = runCatching {
        if (!healthRepository.isAvailable) error("HealthConnect が利用できません")
        healthRepository.refreshPermissions()
        if (!healthRepository.permissionsGranted.value) error("HealthConnect の権限がありません")

        val existing = healthPageRepository.load().days[date]
        if (existing != null && existing.hasAny() && !overwrite) return@runCatching existing

        val fetched = healthRepository.readDay(date)
        if (!fetched.hasAny()) return@runCatching fetched
        healthPageRepository.upsertDay(fetched).getOrThrow()

        val sleepDay = healthRepository.readSleepDay(date)
        if (sleepDay.hasAny()) {
            sleepPageRepository.upsertDay(sleepDay).getOrThrow()
        }
        fetched
    }
}
