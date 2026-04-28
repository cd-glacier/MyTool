package cdglacier.mytool.widget

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.glance.GlanceComposable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import cdglacier.mytool.R
import cdglacier.mytool.data.repository.CalendarEvent
import cdglacier.mytool.data.repository.GoogleCalendarRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val CALENDAR_EVENTS_KEY = stringPreferencesKey("calendar_events_json")
val CALENDAR_TOMORROW_EVENTS_KEY = stringPreferencesKey("calendar_tomorrow_events_json")
val CALENDAR_PERMISSION_KEY = booleanPreferencesKey("calendar_permission_granted")

class GoogleCalendarWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
            val hasPermission = prefs[CALENDAR_PERMISSION_KEY] ?: true

            val todayEvents: List<CalendarEvent> = prefs[CALENDAR_EVENTS_KEY]?.let {
                try { Json.decodeFromString(it) } catch (e: Exception) { emptyList() }
            } ?: emptyList()

            val tomorrowEvents: List<CalendarEvent> = prefs[CALENDAR_TOMORROW_EVENTS_KEY]?.let {
                try { Json.decodeFromString(it) } catch (e: Exception) { emptyList() }
            } ?: emptyList()

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(Color.Transparent)
                    .clickable(actionRunCallback<UpdateCalendarWidgetCallback>()),
                contentAlignment = if (hasPermission) Alignment.TopStart else Alignment.Center,
            ) {
                if (!hasPermission) {
                    Text(
                        text = "カレンダーへのアクセスを許可してください",
                        style = TextStyle(color = ColorProvider(Color(0xFFFB4934))),
                    )
                } else {
                    CalendarSections(todayEvents, tomorrowEvents)
                }
            }
        }
    }
}

@GlanceComposable
@Composable
private fun CalendarSections(
    todayEvents: List<CalendarEvent>,
    tomorrowEvents: List<CalendarEvent>,
) {
    val textColor = ColorProvider(R.color.widget_text)
    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
        item {
            Text(
                text = "Today",
                style = TextStyle(color = textColor, fontWeight = FontWeight.Bold, fontSize = 12.sp),
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp)
                    .clickable(actionRunCallback<UpdateCalendarWidgetCallback>()),
            )
        }
        if (todayEvents.isEmpty()) {
            item {
                Text(
                    text = "予定なし",
                    style = TextStyle(color = textColor),
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, bottom = 4.dp)
                        .clickable(actionRunCallback<UpdateCalendarWidgetCallback>()),
                )
            }
        } else {
            items(todayEvents) { event -> EventRow(event) }
        }

        item {
            Text(
                text = "Tomorrow",
                style = TextStyle(color = textColor, fontWeight = FontWeight.Bold, fontSize = 12.sp),
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 2.dp)
                    .clickable(actionRunCallback<UpdateCalendarWidgetCallback>()),
            )
        }
        if (tomorrowEvents.isEmpty()) {
            item {
                Text(
                    text = "予定なし",
                    style = TextStyle(color = textColor),
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(start = 4.dp)
                        .clickable(actionRunCallback<UpdateCalendarWidgetCallback>()),
                )
            }
        } else {
            items(tomorrowEvents) { event -> EventRow(event) }
        }
    }
}

@GlanceComposable
@Composable
private fun EventRow(event: CalendarEvent) {
    val textColor = ColorProvider(R.color.widget_text)
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
            .padding(vertical = 1.dp)
            .clickable(actionRunCallback<UpdateCalendarWidgetCallback>()),
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
            style = TextStyle(color = textColor),
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

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("content://com.android.calendar/time")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

suspend fun updateCalendarWidgetContent(context: Context, glanceId: GlanceId) {
    val hasPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_CALENDAR
    ) == PackageManager.PERMISSION_GRANTED

    val (todayEvents, tomorrowEvents) = if (!hasPermission) {
        emptyList<CalendarEvent>() to emptyList()
    } else {
        val appWidgetId = try {
            GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        } catch (e: Exception) {
            -1
        }
        // null = キー未存在 → 後方互換で全カレンダー表示
        val selectedIds = if (appWidgetId != -1) {
            WidgetPreferences.getSelectedCalendarIdsFlow(context, appWidgetId).first()
        } else {
            null
        }

        val repo = GoogleCalendarRepositoryImpl(context)
        val today = try { repo.getEventsForDate(LocalDate.now(), selectedIds) } catch (e: Exception) { emptyList() }
        val tomorrow = try { repo.getEventsForDate(LocalDate.now().plusDays(1), selectedIds) } catch (e: Exception) { emptyList() }
        today to tomorrow
    }

    updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
        prefs.toMutablePreferences().apply {
            this[CALENDAR_EVENTS_KEY] = Json.encodeToString(todayEvents)
            this[CALENDAR_TOMORROW_EVENTS_KEY] = Json.encodeToString(tomorrowEvents)
            this[CALENDAR_PERMISSION_KEY] = hasPermission
        }
    }
    GoogleCalendarWidget().update(context, glanceId)
}
