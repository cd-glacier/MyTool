package cdglacier.mytool.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import cdglacier.mytool.ui.theme.MyToolTheme
import kotlinx.coroutines.launch

class JournalTodoWidgetConfigActivity : ComponentActivity() {

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

                    // Load existing settings
                    val widgetId = appWidgetId
                    androidx.compose.runtime.LaunchedEffect(widgetId) {
                        WidgetPreferences.getVaultDirUriFlow(this@JournalTodoWidgetConfigActivity, widgetId)
                            .collect { uri -> if (uri != null) vaultDirUri = uri }
                    }
                    androidx.compose.runtime.LaunchedEffect(widgetId) {
                        WidgetPreferences.getJournalDirUriFlow(this@JournalTodoWidgetConfigActivity, widgetId)
                            .collect { uri -> if (uri != null) journalDirUri = uri }
                    }
                    androidx.compose.runtime.LaunchedEffect(widgetId) {
                        WidgetPreferences.getFilenameFormatFlow(this@JournalTodoWidgetConfigActivity, widgetId)
                            .collect { format -> filenameFormat = format }
                    }
                    androidx.compose.runtime.LaunchedEffect(widgetId) {
                        WidgetPreferences.getBackgroundOpacityFlow(this@JournalTodoWidgetConfigActivity, widgetId)
                            .collect { opacity -> backgroundOpacity = opacity.toFloat() }
                    }

                    val vaultDirPicker = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocumentTree()
                    ) { uri ->
                        if (uri != null) {
                            contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                            vaultDirUri = uri
                        }
                    }

                    val journalDirPicker = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocumentTree()
                    ) { uri ->
                        if (uri != null) {
                            contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                            journalDirUri = uri
                        }
                    }

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

                        Text(text = "Obsidian Vault フォルダ")
                        Button(
                            onClick = { vaultDirPicker.launch(vaultDirUri) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (vaultDirUri != null)
                                    WidgetPreferences.getVaultName(this@JournalTodoWidgetConfigActivity, vaultDirUri!!) ?: vaultDirUri.toString()
                                else
                                    "フォルダを選択"
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(text = "ジャーナルフォルダ")
                        Button(
                            onClick = { journalDirPicker.launch(journalDirUri) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (journalDirUri != null)
                                    journalDirUri.toString()
                                else
                                    "フォルダを選択"
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

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
                                val vUri = vaultDirUri
                                val jUri = journalDirUri
                                if (vUri == null || jUri == null) return@Button
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
                            enabled = vaultDirUri != null && journalDirUri != null
                        ) {
                            Text("保存")
                        }
                    }
                }
            }
        }
    }
}
