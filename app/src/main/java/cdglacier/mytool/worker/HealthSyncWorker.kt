package cdglacier.mytool.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import cdglacier.mytool.widget.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * 毎日 06:00 に前日分の Health データを HealthConnect から取得し Health.md に追記する。
 * 起動時 runOnce でも直近数日を補完する。
 */
class HealthSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            WidgetEntryPoint::class.java,
        )
        val syncHealthDay = entryPoint.syncHealthDayUseCase()
        val today = LocalDate.now()
        var anyFailure = false
        for (offset in 1L..BACKFILL_DAYS) {
            val date = today.minusDays(offset)
            val result = syncHealthDay(date)
            if (result.isFailure) anyFailure = true
        }
        return if (anyFailure) Result.retry() else Result.success()
    }

    companion object {
        private const val WORK_NAME = "HealthSync"
        private const val BACKFILL_DAYS = 3L

        fun schedule(context: Context) {
            val initialDelay = initialDelayUntil(LocalTime.of(6, 0))
            val request = PeriodicWorkRequestBuilder<HealthSyncWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(initialDelay.toMinutes(), TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun runOnce(context: Context) {
            WorkManager.getInstance(context).enqueue(
                OneTimeWorkRequestBuilder<HealthSyncWorker>().build()
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        private fun initialDelayUntil(target: LocalTime): Duration {
            val now = LocalDateTime.now()
            var next = now.toLocalDate().atTime(target)
            if (!next.isAfter(now)) next = next.plusDays(1)
            return Duration.between(now, next)
        }
    }
}
