package cdglacier.mytool.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.ui.theme.MyToolTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class JournalTodoWidgetConfigActivity : ComponentActivity() {

    @Inject
    lateinit var obsidianRepository: ObsidianRepository

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Default result is CANCELED
        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            MyToolTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val scope = rememberCoroutineScope()

                    var vaultDirUri by remember { mutableStateOf<Uri?>(null) }
                    var journalDirUri by remember { mutableStateOf<Uri?>(null) }
                    var filenameFormat by remember { mutableStateOf(WidgetPreferences.DEFAULT_FILENAME_FORMAT) }
                    var backgroundOpacity by remember { mutableStateOf(80f) }

                    val widgetId = appWidgetId

                    // Load vault/journal URIs from ObsidianRepository (managed in app Settings)
                    LaunchedEffect(widgetId) {
                        vaultDirUri = obsidianRepository.vaultUri.first()
                        journalDirUri = obsidianRepository.journalDirUri.first()
                    }

                    // Load filename format: prefer existing widget setting, fall back to repository
                    LaunchedEffect(widgetId) {
                        val widgetVal = WidgetPreferences.getFilenameFormatFlow(this@JournalTodoWidgetConfigActivity, widgetId).first()
                        val repoVal = obsidianRepository.filenameFormat.first()
                        filenameFormat = if (widgetVal != WidgetPreferences.DEFAULT_FILENAME_FORMAT) widgetVal else repoVal
                    }

                    // Load background opacity from existing widget setting
                    LaunchedEffect(widgetId) {
                        WidgetPreferences.getBackgroundOpacityFlow(this@JournalTodoWidgetConfigActivity, widgetId)
                            .collect { opacity -> backgroundOpacity = opacity.toFloat() }
                    }

                    val directoriesConfigured = vaultDirUri != null && journalDirUri != null

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "ウィジェット設定",
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (!directoriesConfigured) {
                            Text(
                                text = "Obsidianフォルダが設定されていません。\nアプリのSettingsページでVaultとJournalフォルダを設定してください。",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .padding(12.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        OutlinedTextField(
                            value = filenameFormat,
                            onValueChange = { filenameFormat = it },
                            label = { Text("ファイル名フォーマット") },
                            placeholder = { Text("yyyy-MM-dd") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(text = "背景の不透明度: ${backgroundOpacity.toInt()}%")
                        Slider(
                            value = backgroundOpacity,
                            onValueChange = { backgroundOpacity = it },
                            valueRange = 0f..100f,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                val vUri = vaultDirUri ?: return@Button
                                val jUri = journalDirUri ?: return@Button
                                val currentFilenameFormat = filenameFormat
                                val currentOpacity = backgroundOpacity.toInt()

                                scope.launch {
                                    WidgetPreferences.setVaultDirUri(
                                        this@JournalTodoWidgetConfigActivity,
                                        widgetId,
                                        vUri
                                    )
                                    WidgetPreferences.setJournalDirUri(
                                        this@JournalTodoWidgetConfigActivity,
                                        widgetId,
                                        jUri
                                    )
                                    WidgetPreferences.setFilenameFormat(
                                        this@JournalTodoWidgetConfigActivity,
                                        widgetId,
                                        currentFilenameFormat
                                    )
                                    WidgetPreferences.setBackgroundOpacity(
                                        this@JournalTodoWidgetConfigActivity,
                                        widgetId,
                                        currentOpacity
                                    )

                                    val glanceId = GlanceAppWidgetManager(this@JournalTodoWidgetConfigActivity)
                                        .getGlanceIdBy(widgetId)
                                    updateWidgetContent(this@JournalTodoWidgetConfigActivity, glanceId)

                                    WidgetUpdateWorker.schedulePeriodicUpdate(this@JournalTodoWidgetConfigActivity)

                                    val resultValue = Intent().apply {
                                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                                    }
                                    setResult(Activity.RESULT_OK, resultValue)
                                    finish()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = directoriesConfigured
                        ) {
                            Text("保存")
                        }
                    }
                }
            }
        }
    }
}
