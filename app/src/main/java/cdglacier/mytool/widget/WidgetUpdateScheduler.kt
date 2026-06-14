package cdglacier.mytool.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WidgetUpdateScheduler {
    inline fun <reified W : CoroutineWorker> schedulePeriodic(context: Context, workName: String) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            workName,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<W>(15, TimeUnit.MINUTES).build(),
        )
    }

    inline fun <reified W : CoroutineWorker> runOnce(context: Context) {
        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequestBuilder<W>().build(),
        )
    }

    fun cancel(context: Context, workName: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName)
    }
}
