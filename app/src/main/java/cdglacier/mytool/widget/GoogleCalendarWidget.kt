package cdglacier.mytool.widget

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.glance.GlanceComposable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import cdglacier.mytool.R
import cdglacier.mytool.data.repository.CalendarEvent
import cdglacier.mytool.data.repository.GoogleCalendarRepositoryImpl
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val CALENDAR_EVENTS_KEY = stringPreferencesKey("calendar_events_json")
val CALENDAR_PERMISSION_KEY = booleanPreferencesKey("calendar_permission_granted")

class GoogleCalendarWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
            val eventsJson = prefs[CALENDAR_EVENTS_KEY]
            val hasPermission = prefs[CALENDAR_PERMISSION_KEY] ?: true

            val events: List<CalendarEvent> = eventsJson?.let {
                try {
                    Json.decodeFromString(it)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .background(Color.Transparent)
                    .clickable(actionRunCallback<UpdateCalendarWidgetCallback>()),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    !hasPermission -> Text(
                        text = "カレンダーへのアクセスを許可してください",
                        style = TextStyle(color = ColorProvider(Color(0xFFFB4934))),
                    )
                    events.isEmpty() -> Text(
                        text = "今日の予定はありません",
                        style = TextStyle(color = ColorProvider(R.color.widget_text)),
                    )
                    else -> Column(modifier = GlanceModifier.fillMaxSize()) {
                        events.forEach { event ->
                            EventRow(event)
                        }
                    }
                }
            }
        }
    }
}

@GlanceComposable
@Composable
private fun EventRow(event: CalendarEvent) {
    val textColor = ColorProvider(R.color.widget_text)
    val timeColor = ColorProvider(Color(0xFFAC8983))
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val zone = ZoneId.systemDefault()

    val timeText = if (event.isAllDay) {
        "終日"
    } else {
        val start = Instant.ofEpochMilli(event.dtStart).atZone(zone).toLocalTime().format(timeFormatter)
        val end = Instant.ofEpochMilli(event.dtEnd).atZone(zone).toLocalTime().format(timeFormatter)
        "$start〜$end"
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .width(4.dp)
                .height(14.dp)
                .background(Color(event.calendarColor)),
        ) {}
        Text(
            text = timeText,
            style = TextStyle(color = timeColor),
            modifier = GlanceModifier.padding(start = 6.dp, end = 4.dp),
        )
        Text(
            text = event.title,
            maxLines = 1,
            style = TextStyle(color = textColor),
            modifier = GlanceModifier.defaultWeight(),
        )
    }
}

class UpdateCalendarWidgetCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        CalendarWidgetUpdateWorker.runOnce(context)
        updateCalendarWidgetContent(context, glanceId)
    }
}

suspend fun updateCalendarWidgetContent(context: Context, glanceId: GlanceId) {
    val hasPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_CALENDAR
    ) == PackageManager.PERMISSION_GRANTED

    val events = if (hasPermission) {
        try {
            GoogleCalendarRepositoryImpl(context).getTodayEvents()
        } catch (e: Exception) {
            emptyList()
        }
    } else {
        emptyList()
    }

    updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
        prefs.toMutablePreferences().apply {
            this[CALENDAR_EVENTS_KEY] = Json.encodeToString(events)
            this[CALENDAR_PERMISSION_KEY] = hasPermission
        }
    }
    GoogleCalendarWidget().update(context, glanceId)
}
