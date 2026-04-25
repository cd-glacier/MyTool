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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.GlanceAppWidgetManager
import cdglacier.mytool.data.repository.CalendarAccount
import cdglacier.mytool.data.repository.GoogleCalendarRepository
import cdglacier.mytool.ui.theme.MyToolTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GoogleCalendarWidgetConfigActivity : ComponentActivity() {

    @Inject
    lateinit var calendarRepository: GoogleCalendarRepository

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                    color = MaterialTheme.colorScheme.background,
                ) {
                    CalendarWidgetConfigScreen(
                        widgetId = appWidgetId,
                        repository = calendarRepository,
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
}

@Composable
private fun CalendarWidgetConfigScreen(
    widgetId: Int,
    repository: GoogleCalendarRepository,
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "カレンダーWidget設定",
            style = MaterialTheme.typography.headlineSmall,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Widgetに表示するカレンダーを選択してください。",
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (!hasPermission) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(12.dp),
            ) {
                Text(
                    text = "カレンダーへのアクセス権限が必要です。",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.READ_CALENDAR) },
                ) {
                    Text("権限を許可")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        } else if (loaded && calendars.isEmpty()) {
            Text(
                text = "利用可能なカレンダーが見つかりませんでした。",
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(12.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            items(calendars, key = { it.id }) { account ->
                val checked = checkedState[account.id] == true
                ListItem(
                    headlineContent = { Text(account.displayName) },
                    supportingContent = {
                        if (account.accountName.isNotBlank() && account.accountName != account.displayName) {
                            Text(account.accountName)
                        }
                    },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(account.color)),
                        )
                    },
                    trailingContent = {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { checkedState[account.id] = it },
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { checkedState[account.id] = !checked },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "選択中: $selectedCount 件",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                val ids = checkedState.filterValues { it }.keys.toSet()
                scope.launch {
                    WidgetPreferences.setSelectedCalendarIds(context, widgetId, ids)
                    val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(widgetId)
                    updateCalendarWidgetContent(context, glanceId)
                    CalendarWidgetUpdateWorker.schedulePeriodicUpdate(context)
                    onSaved()
                }
            },
            enabled = hasPermission && selectedCount > 0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("保存")
        }
    }
}

