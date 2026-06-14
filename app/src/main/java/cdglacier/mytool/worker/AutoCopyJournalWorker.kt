package cdglacier.mytool.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import cdglacier.mytool.data.repository.ObsidianRepositoryImpl
import cdglacier.mytool.domain.usecase.CheckJournalTargetUseCase
import cdglacier.mytool.domain.usecase.CopyJournalUseCase
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class AutoCopyJournalWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val repo = ObsidianRepositoryImpl(ctx)
        val journalDirUri = repo.journalDirUri.first() ?: return Result.success()
        val format = repo.filenameFormat.first()
        val today = LocalDate.now()

        val alreadyExists = CheckJournalTargetUseCase(ctx)(journalDirUri, today, format)
        if (alreadyExists) return Result.success()

        val copyResult = CopyJournalUseCase(ctx)(
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
