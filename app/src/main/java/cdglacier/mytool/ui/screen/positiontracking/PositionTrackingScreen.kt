package cdglacier.mytool.ui.screen.positiontracking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import cdglacier.mytool.data.repository.TrackingMode
import cdglacier.mytool.ui.component.GlacierButton
import cdglacier.mytool.ui.component.GlacierSectionCard
import cdglacier.mytool.ui.component.GlacierSwitch
import cdglacier.mytool.ui.component.GlacierTopBar
import cdglacier.mytool.ui.component.OsmMapView
import cdglacier.mytool.ui.theme.GlacierAmber
import cdglacier.mytool.ui.theme.GlacierBg
import cdglacier.mytool.ui.theme.GlacierMuted
import cdglacier.mytool.ui.theme.GlacierOnSurface
import cdglacier.mytool.ui.theme.GlacierSurface
import cdglacier.mytool.ui.theme.GlacierTeal
import java.time.format.DateTimeFormatter

@Composable
fun PositionTrackingRoute(
    onBack: () -> Unit,
    viewModel: PositionTrackingViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermissions()
        onPauseOrDispose { }
    }
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onSnackbarShown()
        }
    }

    PositionTrackingScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onToggleTracking = viewModel::onToggleTracking,
        onExportToJournal = viewModel::onExportToJournal,
        onDateChange = viewModel::onDateChange,
        onBack = onBack,
    )
}

@Composable
fun PositionTrackingScreen(
    uiState: PositionTrackingUiState,
    snackbarHostState: SnackbarHostState,
    onToggleTracking: (Boolean) -> Unit,
    onExportToJournal: () -> Unit,
    onDateChange: (Long) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = { GlacierTopBar(title = "POSITION_TRACKING", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = GlacierBg,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlacierSectionCard(title = "BG_TRACKING") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uiState.trackingEnabled) "RECORDING" else "STOPPED",
                            color = if (uiState.trackingEnabled) GlacierTeal else GlacierMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                        )
                        val (precisionLabel, precisionDetail) = when {
                            !uiState.trackingEnabled -> "OFF" to "記録は停止しています"
                            uiState.trackingMode == TrackingMode.MOVING -> "HIGH" to "GPS高精度 / 5秒間隔"
                            else -> "BALANCED" to "省電力 / 30秒間隔"
                        }
                        val precisionColor = if (uiState.trackingEnabled) GlacierTeal else GlacierMuted
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "PRECISION: ",
                                color = GlacierMuted,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                            )
                            Text(
                                text = precisionLabel,
                                color = precisionColor,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = precisionDetail,
                            color = GlacierMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                        )
                        if (!uiState.permissionsReady) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "! SYS_SETTINGS で位置情報権限を許可してください",
                                color = GlacierAmber,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                            )
                        }
                    }
                    GlacierSwitch(
                        checked = uiState.trackingEnabled,
                        onCheckedChange = onToggleTracking,
                        enabled = uiState.permissionsReady,
                    )
                }
            }

            GlacierSectionCard(title = "EXPORT") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (uiState.journalDirUri == null)
                            "! SETTINGS で Journal フォルダを設定してください"
                        else
                            "選択中の日付の位置情報を JOURNAL に出力します",
                        color = if (uiState.journalDirUri == null) GlacierAmber else GlacierMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                    )
                    GlacierButton(
                        label = "JOURNALへ出力",
                        onClick = onExportToJournal,
                        enabled = uiState.canExport,
                        loading = uiState.isExporting,
                        loadingLabel = "EXPORTING...",
                    )
                }
            }

            DateNavRow(
                dateText = uiState.date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                recordCount = uiState.points.size,
                onPrev = { onDateChange(-1) },
                onNext = { onDateChange(1) },
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(GlacierSurface),
            ) {
                if (uiState.points.isEmpty()) {
                    Text(
                        text = "NO_RECORDS",
                        color = GlacierMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    OsmMapView(
                        points = uiState.points,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun DateNavRow(
    dateText: String,
    recordCount: Int,
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
            Text(
                text = "POINTS: $recordCount",
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
