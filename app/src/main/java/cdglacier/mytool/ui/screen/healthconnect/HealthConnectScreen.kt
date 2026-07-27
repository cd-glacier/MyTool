package cdglacier.mytool.ui.screen.healthconnect

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
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
        onSourceDateChange = viewModel::onSourceDateChange,
        onTargetDateChange = viewModel::onTargetDateChange,
        onWriteToJournal = viewModel::onWriteToJournal,
        onBack = onBack,
    )
}

@Composable
fun HealthConnectScreen(
    uiState: HealthConnectUiState,
    snackbarHostState: SnackbarHostState,
    onRequestPermission: () -> Unit,
    onSourceDateChange: (Long) -> Unit,
    onTargetDateChange: (Long) -> Unit,
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
            GlacierSectionCard(title = "SOURCE_DATE") {
                DateNavRow(
                    dateText = uiState.sourceDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    caption = "取得元の日付",
                    onPrev = { onSourceDateChange(-1) },
                    onNext = { onSourceDateChange(1) },
                )
            }

            GlacierSectionCard(title = "DATA") {
                when {
                    !uiState.healthConnectAvailable -> Text(
                        text = "! Health Connect が利用できません",
                        color = GlacierAmber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    )
                    !uiState.permissionsGranted -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "! Health Connect の権限が未許可です",
                            color = GlacierAmber,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                        )
                        GlacierButton(
                            label = "権限をリクエスト",
                            onClick = onRequestPermission,
                        )
                    }
                    uiState.isLoading -> Text(
                        text = "LOADING...",
                        color = GlacierMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    )
                    !uiState.hasAnyData -> Text(
                        text = "NO_DATA",
                        color = GlacierMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    )
                    else -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DataRow(label = "SLEEP", value = uiState.sleep?.let(::formatSleep) ?: "--")
                        DataRow(label = "STEPS", value = uiState.steps?.let { "$it steps" } ?: "--")
                    }
                }
            }

            GlacierSectionCard(title = "TARGET_JOURNAL") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateNavRow(
                        dateText = uiState.targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        caption = "挿入先の Journal 日付",
                        onPrev = { onTargetDateChange(-1) },
                        onNext = { onTargetDateChange(1) },
                    )
                    Text(
                        text = if (uiState.journalDirUri == null)
                            "! SETTINGS で Journal フォルダを設定してください"
                        else
                            "選択中の日付の Journal に # Health セクションを挿入します",
                        color = if (uiState.journalDirUri == null) GlacierAmber else GlacierMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                    )
                    GlacierButton(
                        label = "JOURNALへ挿入",
                        onClick = onWriteToJournal,
                        enabled = uiState.canWrite,
                        loading = uiState.isWriting,
                        loadingLabel = "WRITING...",
                    )
                }
            }
        }
    }
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
    caption: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArrowButton(label = "<", onClick = onPrev)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = dateText,
                color = GlacierOnSurface,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            Text(
                text = caption,
                color = GlacierMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
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
