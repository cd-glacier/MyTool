package cdglacier.mytool.widget

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.GlanceAppWidgetManager
import cdglacier.mytool.data.repository.GoogleCalendarRepository
import cdglacier.mytool.data.repository.WidgetConfigRepository
import cdglacier.mytool.domain.model.CalendarAccount
import cdglacier.mytool.ui.component.GlacierButton
import cdglacier.mytool.ui.component.GlacierSectionCard
import cdglacier.mytool.ui.component.GlacierTopBar
import cdglacier.mytool.ui.component.NoticeCard
import cdglacier.mytool.ui.theme.GlacierAmber
import cdglacier.mytool.ui.theme.GlacierBg
import cdglacier.mytool.ui.theme.GlacierMuted
import cdglacier.mytool.ui.theme.GlacierOnSurface
import cdglacier.mytool.ui.theme.GlacierSurface
import cdglacier.mytool.ui.theme.GlacierTeal
import cdglacier.mytool.ui.theme.MyToolTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GoogleCalendarWidgetConfigActivity : ComponentActivity() {

    @Inject
    lateinit var calendarRepository: GoogleCalendarRepository

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
                CalendarWidgetConfigScreen(
                    widgetId = appWidgetId,
                    repository = calendarRepository,
                    widgetConfigRepository = widgetConfigRepository,
                    onBack = { finish() },
                    onSaved = {
                        val resultValue = Intent().apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        }
                        setResult(Activity.RESULT_OK, resultValue)
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
private fun CalendarWidgetConfigScreen(
    widgetId: Int,
    repository: GoogleCalendarRepository,
    widgetConfigRepository: WidgetConfigRepository,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_CALENDAR,
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
    }

    var calendars by remember { mutableStateOf<List<CalendarAccount>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    val checkedState = remember { mutableStateMapOf<Long, Boolean>() }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            calendars = try {
                repository.getCalendars()
            } catch (e: Exception) {
                emptyList()
            }
            loaded = true
        }
    }

    val selectedCount = checkedState.values.count { it }

    Scaffold(
        topBar = { GlacierTopBar(title = "CALENDAR_WIDGET_CFG", onBack = onBack) },
        containerColor = GlacierBg,
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                !hasPermission -> {
                    NoticeCard(
                        title = "PERMISSION: NOT_GRANTED",
                        body = "カレンダーへのアクセス権限が必要です。",
                    )
                    GlacierButton(
                        label = "権限を許可",
                        onClick = { permissionLauncher.launch(Manifest.permission.READ_CALENDAR) },
                    )
                }
                loaded && calendars.isEmpty() -> {
                    NoticeCard(
                        title = "NO_CALENDARS",
                        body = "利用可能なカレンダーが見つかりませんでした。",
                    )
                }
            }

            if (hasPermission && calendars.isNotEmpty()) {
                GlacierSectionCard(
                    title = "CALENDARS",
                    modifier = Modifier.weight(1f),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(calendars, key = { it.id }) { account ->
                            val checked = checkedState[account.id] == true
                            CalendarRow(
                                account = account,
                                checked = checked,
                                onToggle = { checkedState[account.id] = !checked },
                            )
                        }
                    }
                }
            }

            Text(
                text = "SELECTED: $selectedCount",
                color = GlacierMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
            )

            GlacierButton(
                label = "保存",
                onClick = {
                    val ids = checkedState.filterValues { it }.keys.toSet()
                    scope.launch {
                        widgetConfigRepository.setSelectedCalendarIds(widgetId, ids)
                        val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(widgetId)
                        updateCalendarWidgetContent(context, glanceId)
                        CalendarWidgetUpdateWorker.schedulePeriodicUpdate(context)
                        onSaved()
                    }
                },
                enabled = hasPermission && selectedCount > 0,
            )
        }
    }
}

@Composable
private fun CalendarRow(
    account: CalendarAccount,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlacierSurface)
            .clickable { onToggle() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color(account.color)),
        )
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.displayName,
                color = GlacierOnSurface,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
            )
            if (account.accountName.isNotBlank() && account.accountName != account.displayName) {
                Text(
                    text = account.accountName,
                    color = GlacierMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                )
            }
        }
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(if (checked) GlacierTeal else GlacierBg)
                .border(1.dp, if (checked) GlacierTeal else GlacierMuted),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Text(
                    text = "x",
                    color = GlacierBg,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
            }
        }
    }
}
