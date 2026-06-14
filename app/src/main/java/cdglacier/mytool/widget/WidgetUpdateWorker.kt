package cdglacier.mytool.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.work.WorkerParameters

class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters,
) : BaseWidgetUpdateWorker(context, params) {

    override val widgetClass: Class<out GlanceAppWidget> = JournalTodoWidget::class.java

    override suspend fun updateContent(context: Context, glanceId: GlanceId) {
        updateWidgetContent(context, glanceId)
    }

    companion object {
        private const val WORK_NAME = "JournalTodoWidgetUpdate"

        fun schedulePeriodicUpdate(context: Context) =
            WidgetUpdateScheduler.schedulePeriodic<WidgetUpdateWorker>(context, WORK_NAME)

        fun runOnce(context: Context) =
            WidgetUpdateScheduler.runOnce<WidgetUpdateWorker>(context)

        fun cancel(context: Context) =
            WidgetUpdateScheduler.cancel(context, WORK_NAME)
    }
}
