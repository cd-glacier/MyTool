package cdglacier.mytool.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.data.repository.WidgetConfigRepository
import cdglacier.mytool.ui.component.GlacierButton
import cdglacier.mytool.ui.component.GlacierSectionCard
import cdglacier.mytool.ui.component.GlacierTopBar
import cdglacier.mytool.ui.component.NoticeCard
import cdglacier.mytool.ui.theme.GlacierAmber
import cdglacier.mytool.ui.theme.GlacierBg
import cdglacier.mytool.ui.theme.GlacierMuted
import cdglacier.mytool.ui.theme.GlacierOnSurface
import cdglacier.mytool.ui.theme.GlacierSurface
import cdglacier.mytool.ui.theme.GlacierSurfaceHigh
import cdglacier.mytool.ui.theme.GlacierTeal
import cdglacier.mytool.ui.theme.MyToolTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class JournalTodoWidgetConfigActivity : ComponentActivity() {

    @Inject
    lateinit var obsidianRepository: ObsidianRepository

    @Inject
    lateinit var widgetConfigRepository: WidgetConfigRepository

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                val scope = rememberCoroutineScope()

                var vaultDirUri by remember { mutableStateOf<Uri?>(null) }
                var journalDirUri by remember { mutableStateOf<Uri?>(null) }
                var filenameFormat by remember { mutableStateOf(WidgetConfigRepository.DEFAULT_FILENAME_FORMAT) }
                var backgroundOpacity by remember { mutableStateOf(80f) }

                val widgetId = appWidgetId

                LaunchedEffect(widgetId) {
                    vaultDirUri = obsidianRepository.vaultUri.first()
                    journalDirUri = obsidianRepository.journalDirUri.first()
                }

                LaunchedEffect(widgetId) {
                    val widgetVal = widgetConfigRepository.filenameFormat(widgetId).first()
                    val repoVal = obsidianRepository.filenameFormat.first()
                    filenameFormat = if (widgetVal != WidgetConfigRepository.DEFAULT_FILENAME_FORMAT) widgetVal else repoVal
                }

                LaunchedEffect(widgetId) {
                    widgetConfigRepository.backgroundOpacity(widgetId)
                        .collect { opacity -> backgroundOpacity = opacity.toFloat() }
                }

                val directoriesConfigured = vaultDirUri != null && journalDirUri != null

                Scaffold(
                    topBar = { GlacierTopBar(title = "JOURNAL_WIDGET_CFG", onBack = { finish() }) },
                    containerColor = GlacierBg,
                    contentWindowInsets = WindowInsets(0),
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (!directoriesConfigured) {
                            NoticeCard(
                                title = "DIRS: NOT_SET",
                                body = "アプリの SYS_SETTINGS で Vault と Journal フォルダを設定してください。",
                            )
                        }

                        GlacierSectionCard(title = "FILENAME_FMT") {
                            BasicTextField(
                                value = filenameFormat,
                                onValueChange = { filenameFormat = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = GlacierOnSurface,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp,
                                ),
                                cursorBrush = SolidColor(GlacierAmber),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(GlacierSurface)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "例: yyyy-MM-dd",
                                color = GlacierMuted,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                            )
                        }

                        GlacierSectionCard(title = "BG_OPACITY") {
                            Text(
                                text = "OPACITY: ${backgroundOpacity.toInt()}%",
                                color = GlacierOnSurface,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                            )
                            Slider(
                                value = backgroundOpacity,
                                onValueChange = { backgroundOpacity = it },
                                valueRange = 0f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = GlacierAmber,
                                    activeTrackColor = GlacierTeal,
                                    inactiveTrackColor = GlacierSurfaceHigh,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        GlacierButton(
                            label = "保存",
                            onClick = {
                                val vUri = vaultDirUri ?: return@GlacierButton
                                val jUri = journalDirUri ?: return@GlacierButton
                                val currentFilenameFormat = filenameFormat
                                val currentOpacity = backgroundOpacity.toInt()

                                scope.launch {
                                    widgetConfigRepository.setVaultDirUri(widgetId, vUri)
                                    widgetConfigRepository.setJournalDirUri(widgetId, jUri)
                                    widgetConfigRepository.setFilenameFormat(widgetId, currentFilenameFormat)
                                    widgetConfigRepository.setBackgroundOpacity(widgetId, currentOpacity)

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
                            enabled = directoriesConfigured,
                        )
                    }
                }
            }
        }
    }
}
