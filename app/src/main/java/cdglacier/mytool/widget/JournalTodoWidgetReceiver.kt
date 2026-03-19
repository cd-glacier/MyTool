package cdglacier.mytool.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import cdglacier.mytool.di.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class JournalTodoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = JournalTodoWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetUpdateWorker.schedulePeriodicUpdate(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val widgetPreferences = EntryPointAccessors
            .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
            .widgetPreferences()
        CoroutineScope(Dispatchers.IO).launch {
            appWidgetIds.forEach { widgetPreferences.deleteWidgetPrefs(it) }
            val remaining = GlanceAppWidgetManager(context).getGlanceIds(JournalTodoWidget::class.java)
            if (remaining.isEmpty()) WidgetUpdateWorker.cancel(context)
        }
    }
}
