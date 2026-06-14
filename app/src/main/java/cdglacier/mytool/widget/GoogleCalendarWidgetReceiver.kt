package cdglacier.mytool.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GoogleCalendarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GoogleCalendarWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        CalendarWidgetUpdateWorker.schedulePeriodicUpdate(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val widgetConfig = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java,
        ).widgetConfigRepository()
        CoroutineScope(Dispatchers.IO).launch {
            appWidgetIds.forEach { widgetConfig.delete(it) }
            val remaining = GlanceAppWidgetManager(context).getGlanceIds(GoogleCalendarWidget::class.java)
            if (remaining.isEmpty()) CalendarWidgetUpdateWorker.cancel(context)
        }
    }
}
