package cdglacier.mytool.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class CalendarWidgetUpdateWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        GlanceAppWidgetManager(applicationContext)
            .getGlanceIds(GoogleCalendarWidget::class.java)
            .forEach { updateCalendarWidgetContent(applicationContext, it) }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "GoogleCalendarWidgetUpdate"

        fun schedulePeriodicUpdate(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<CalendarWidgetUpdateWorker>(15, TimeUnit.MINUTES).build(),
            )
        }

        fun runOnce(context: Context) {
            WorkManager.getInstance(context).enqueue(
                OneTimeWorkRequestBuilder<CalendarWidgetUpdateWorker>().build(),
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
