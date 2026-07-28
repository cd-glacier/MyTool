package cdglacier.mytool.ui.screen.healthconnect

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cdglacier.mytool.data.repository.HealthPermissions
import cdglacier.mytool.ui.component.GlacierButton
import cdglacier.mytool.ui.component.GlacierSectionCard
import cdglacier.mytool.ui.component.GlacierTopBar
import cdglacier.mytool.ui.theme.GlacierAmber
import cdglacier.mytool.ui.theme.GlacierBg
import cdglacier.mytool.ui.theme.GlacierMuted
import cdglacier.mytool.ui.theme.GlacierOnSurface
import cdglacier.mytool.ui.theme.GlacierSurface
import cdglacier.mytool.ui.theme.GlacierTeal
import java.time.Duration
import java.time.format.DateTimeFormatter

@Composable
fun HealthConnectRoute(
    onBack: () -> Unit,
    viewModel: HealthConnectViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val healthPermissionLauncher = rememberLauncherForActivityResult(
        HealthPermissions.createRequestPermissionResultContract()
    ) { _ ->
        viewModel.refresh()
    }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onSnackbarShown()
        }
    }

    HealthConnectScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onRequestPermission = {
            healthPermissionLauncher.launch(viewModel.healthRequiredPermissions)
        },
        onDateChange = viewModel::onDateChange,
        onWriteToJournal = viewModel::onWriteToJournal,
        onBack = onBack,
    )
}

@Composable
fun HealthConnectScreen(
    uiState: HealthConnectUiState,
    snackbarHostState: SnackbarHostState,
    onRequestPermission: () -> Unit,
    onDateChange: (Long) -> Unit,
    onWriteToJournal: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = { GlacierTopBar(title = "HEALTH_CONNECT", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = GlacierBg,
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DateNavRow(
                dateText = uiState.date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                onPrev = { onDateChange(-1) },
                onNext = { onDateChange(1) },
            )

            GlacierSectionCard(title = "DATA") {
                when {
                    !uiState.healthConnectAvailable -> StatusText(
                        text = "! HEALTH_CONNECT UNAVAILABLE",
                        color = GlacierAmber,
                    )
                    !uiState.permissionsGranted -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusText(
                            text = "! PERMISSION_DENIED",
                            color = GlacierAmber,
                        )
                        GlacierButton(
                            label = "REQUEST_PERMISSION",
                            onClick = onRequestPermission,
                        )
                    }
                    else -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DataRow(
                            label = "SLEEP",
                            value = when {
                                uiState.sleep != null -> formatSleep(uiState.sleep)
                                uiState.isLoading -> "..."
                                else -> "--"
                            },
                        )
                        DataRow(
                            label = "STEPS",
                            value = when {
                                uiState.steps != null -> "${uiState.steps}"
                                uiState.isLoading -> "..."
                                else -> "--"
                            },
                        )
                    }
                }
            }

            GlacierSectionCard(title = "EXPORT") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (uiState.journalDirUri == null)
                            "! SETTINGS で Journal フォルダを設定してください"
                        else
                            "選択中の日付の Health を JOURNAL に出力します",
                        color = if (uiState.journalDirUri == null) GlacierAmber else GlacierMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                    )
                    GlacierButton(
                        label = "JOURNALへ出力",
                        onClick = onWriteToJournal,
                        enabled = uiState.canWrite,
                        loading = uiState.isWriting,
                        loadingLabel = "EXPORTING...",
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusText(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = text,
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
    )
}

@Composable
private fun DataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$label:",
            color = GlacierMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = value,
            color = GlacierTeal,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun DateNavRow(
    dateText: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlacierSurface)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArrowButton(label = "<", onClick = onPrev)
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = dateText,
                color = GlacierOnSurface,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }
        ArrowButton(label = ">", onClick = onNext)
    }
}

@Composable
private fun ArrowButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(GlacierBg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = GlacierAmber,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
    }
}

private fun formatSleep(duration: Duration): String {
    val hours = duration.toHours()
    val minutes = duration.toMinutes() % 60
    return "${hours}h${minutes}m"
}
