package cdglacier.mytool.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

abstract class BaseWidgetUpdateWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    protected abstract val widgetClass: Class<out GlanceAppWidget>
    protected abstract suspend fun updateContent(context: Context, glanceId: GlanceId)

    override suspend fun doWork(): Result {
        GlanceAppWidgetManager(applicationContext)
            .getGlanceIds(widgetClass)
            .forEach { updateContent(applicationContext, it) }
        return Result.success()
    }
}
