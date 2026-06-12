package cdglacier.mytool.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.work.WorkerParameters

class CalendarWidgetUpdateWorker(
    context: Context,
    params: WorkerParameters,
) : BaseWidgetUpdateWorker(context, params) {

    override val widgetClass: Class<out GlanceAppWidget> = GoogleCalendarWidget::class.java

    override suspend fun updateContent(context: Context, glanceId: GlanceId) {
        updateCalendarWidgetContent(context, glanceId)
    }

    companion object {
        private const val WORK_NAME = "GoogleCalendarWidgetUpdate"

        fun schedulePeriodicUpdate(context: Context) =
            WidgetUpdateScheduler.schedulePeriodic<CalendarWidgetUpdateWorker>(context, WORK_NAME)

        fun runOnce(context: Context) =
            WidgetUpdateScheduler.runOnce<CalendarWidgetUpdateWorker>(context)

        fun cancel(context: Context) =
            WidgetUpdateScheduler.cancel(context, WORK_NAME)
    }
}
