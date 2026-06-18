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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
fun PositionTrackingScreen(
    viewModel: PositionTrackingViewModel = viewModel(),
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = { GlacierTopBar(title = "POSITION_TRACKING", onBack = onBack) },
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
                        onCheckedChange = viewModel::onToggleTracking,
                        enabled = uiState.permissionsReady,
                    )
                }
            }

            DateNavRow(
                dateText = uiState.date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                recordCount = uiState.points.size,
                onPrev = { viewModel.onDateChange(-1) },
                onNext = { viewModel.onDateChange(1) },
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(GlacierSurface),
            ) {
                OsmMapView(
                    points = uiState.points,
                    modifier = Modifier.fillMaxSize(),
                )
                if (uiState.points.isEmpty()) {
                    Text(
                        text = "NO_RECORDS",
                        color = GlacierMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Center),
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
