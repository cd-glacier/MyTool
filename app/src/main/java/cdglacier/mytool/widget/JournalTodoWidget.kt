package cdglacier.mytool.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.unit.dp
import cdglacier.mytool.R
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val TODO_ITEMS_KEY = stringPreferencesKey("todo_items_json")

class JournalTodoWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
            val todosJson = prefs[TODO_ITEMS_KEY]

            val todos: List<TodoItem> = if (todosJson != null) {
                try {
                    Json.decodeFromString(todosJson)
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(ColorProvider(R.color.widget_background))
                    .clickable(actionRunCallback<OpenObsidianCallback>()),
                contentAlignment = Alignment.TopStart
            ) {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    val textColor = ColorProvider(R.color.widget_text)
                    if (todos.isEmpty()) {
                        Text(text = "TODOなし", style = TextStyle(color = textColor))
                    } else {
                        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                            items(todos) { todo ->
                                Row(
                                    modifier = GlanceModifier.fillMaxWidth()
                                        .padding(2.dp)
                                        .clickable(actionRunCallback<OpenObsidianCallback>()),
                                    verticalAlignment = Alignment.Vertical.CenterVertically
                                ) {
                                    Text(
                                        text = if (todo.isDone) "☑" else "☐",
                                        style = TextStyle(color = textColor),
                                        modifier = GlanceModifier.padding(end = 4.dp)
                                    )
                                    Text(
                                        text = todo.text,
                                        style = if (todo.isDone) {
                                            TextStyle(color = textColor, textDecoration = TextDecoration.LineThrough)
                                        } else {
                                            TextStyle(color = textColor)
                                        },
                                        modifier = GlanceModifier.defaultWeight()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

suspend fun updateWidgetContent(context: Context, glanceId: GlanceId) {
    val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
    val journalDirUri = WidgetPreferences.getJournalDirUriFlow(context, appWidgetId).first()
    val format = WidgetPreferences.getFilenameFormatFlow(context, appWidgetId).first()

    val markdown = if (journalDirUri != null)
        JournalReader.readTodayJournal(context, journalDirUri, format) else null
    val todos = if (markdown != null) TodoParser.parse(markdown) else emptyList()

    updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
        prefs.toMutablePreferences().apply {
            this[TODO_ITEMS_KEY] = Json.encodeToString(todos)
        }
    }
    JournalTodoWidget().update(context, glanceId)
}

class OpenObsidianCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        val vaultDirUri = WidgetPreferences.getVaultDirUriFlow(context, appWidgetId).first()
        val vaultName = vaultDirUri?.let { WidgetPreferences.getVaultName(context, it) } ?: ""
        val format = WidgetPreferences.getFilenameFormatFlow(context, appWidgetId).first()
        val filename = LocalDate.now().format(DateTimeFormatter.ofPattern(format))

        val obsidianUri = Uri.parse(
            "obsidian://open?vault=${Uri.encode(vaultName)}&file=${Uri.encode(filename)}"
        )
        context.startActivity(Intent(Intent.ACTION_VIEW, obsidianUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
        WidgetUpdateWorker.runOnce(context)
    }
}
