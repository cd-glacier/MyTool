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
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class AutoCopyJournalWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            WidgetEntryPoint::class.java,
        )
        val obsidianRepository = entryPoint.obsidianRepository()
        val journalDirUri = obsidianRepository.journalDirUri.first()?.toString()
            ?: return Result.success()
        val format = obsidianRepository.filenameFormat.first()
        val today = LocalDate.now()

        if (entryPoint.checkJournalTargetUseCase()(journalDirUri, today, format)) {
            return Result.success()
        }

        val copyResult = entryPoint.copyJournalUseCase()(
            journalDirUri = journalDirUri,
            sourceDate = today.minusDays(1),
            targetDate = today,
            filenameFormat = format,
        )
        return if (copyResult.isSuccess) Result.success() else Result.retry()
    }

    companion object {
        private const val WORK_NAME = "AutoCopyJournal"

        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<AutoCopyJournalWorker>(1, TimeUnit.HOURS).build()
            )
        }

        fun runOnce(context: Context) {
            WorkManager.getInstance(context).enqueue(
                OneTimeWorkRequestBuilder<AutoCopyJournalWorker>().build()
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
