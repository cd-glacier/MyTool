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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cdglacier.mytool.data.repository.HealthPermissions
import cdglacier.mytool.domain.model.HealthCategory
import cdglacier.mytool.domain.model.HealthMetric
import cdglacier.mytool.ui.component.GlacierButton
import cdglacier.mytool.ui.component.GlacierSectionCard
import cdglacier.mytool.ui.component.GlacierTopBar
import cdglacier.mytool.ui.theme.GlacierAmber
import cdglacier.mytool.ui.theme.GlacierBg
import cdglacier.mytool.ui.theme.GlacierCyan
import cdglacier.mytool.ui.theme.GlacierMuted
import cdglacier.mytool.ui.theme.GlacierOnSurface
import cdglacier.mytool.ui.theme.GlacierSurface
import cdglacier.mytool.ui.theme.GlacierSurfaceLow
import cdglacier.mytool.ui.theme.GlacierTeal
import cdglacier.mytool.ui.theme.SpaceGroteskFamily

@Composable
fun HealthConnectRoute(
    onBack: () -> Unit,
    onNavigateChart: (String) -> Unit = {},
    onNavigateSleepStage: (String) -> Unit = {},
    viewModel: HealthConnectViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        HealthPermissions.createRequestPermissionResultContract()
    ) { _ ->
        viewModel.refreshPermissions()
    }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermissions()
        onPauseOrDispose { }
    }

    HealthConnectScreen(
        uiState = uiState,
        onBack = onBack,
        onDateChange = viewModel::onDateChange,
        onViewModeChange = viewModel::onViewModeChange,
        onRequestPermission = {
            permissionLauncher.launch(viewModel.healthRequiredPermissions)
        },
        onBackfill = viewModel::onBackfill,
        onSnackbarShown = viewModel::onSnackbarShown,
        onNavigateChart = onNavigateChart,
        onNavigateSleepStage = onNavigateSleepStage,
    )
}

@Composable
fun HealthConnectScreen(
    uiState: HealthConnectUiState,
    onBack: () -> Unit,
    onDateChange: (Long) -> Unit,
    onViewModeChange: (HealthViewMode) -> Unit,
    onRequestPermission: () -> Unit,
    onBackfill: (Int) -> Unit,
    onSnackbarShown: () -> Unit,
    onNavigateChart: (String) -> Unit,
    onNavigateSleepStage: (String) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            onSnackbarShown()
        }
    }
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!uiState.isPagesDirConfigured) {
                WarningCard("PAGES_DIR が未設定です。Settings から設定してください。")
            }
            when {
                !uiState.healthConnectAvailable -> WarningCard("HealthConnect が利用できません")
                !uiState.permissionsGranted -> PermissionRequestCard(onRequestPermission)
            }

            ViewModeSelector(mode = uiState.viewMode, onChange = onViewModeChange)
            AnchorHeader(
                label = when (uiState.viewMode) {
                    HealthViewMode.DAY -> uiState.anchorDate.toString()
                    HealthViewMode.WEEK -> uiState.weekLabel
                },
                isSyncing = uiState.isSyncing,
                onPrev = { onDateChange(-1) },
                onNext = { onDateChange(1) },
            )

            SummaryCard(uiState = uiState)

            for (category in HealthCategory.values()) {
                CategorySection(
                    category = category,
                    uiState = uiState,
                    onNavigateChart = onNavigateChart,
                    onNavigateSleepStage = onNavigateSleepStage,
                )
            }

            BackfillCard(
                progress = uiState.backfillProgress,
                enabled = uiState.permissionsGranted && uiState.healthConnectAvailable,
                onBackfill = onBackfill,
            )
        }
    }
}

@Composable
private fun ViewModeSelector(mode: HealthViewMode, onChange: (HealthViewMode) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().background(GlacierSurface)) {
        HealthViewMode.values().forEach { m ->
            val selected = m == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (selected) GlacierAmber else GlacierSurface)
                    .clickable { onChange(m) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = m.name,
                    color = if (selected) GlacierBg else GlacierMuted,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun AnchorHeader(
    label: String,
    isSyncing: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArrowButton("<", onPrev)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = GlacierCyan,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
            )
            if (isSyncing) {
                Text(
                    text = "SYNCING...",
                    color = GlacierAmber,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                )
            }
        }
        ArrowButton(">", onNext)
    }
}

@Composable
private fun ArrowButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(32.dp)
            .height(32.dp)
            .background(GlacierSurface)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = GlacierAmber, fontFamily = FontFamily.Monospace, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SummaryCard(uiState: HealthConnectUiState) {
    GlacierSectionCard(title = "SUMMARY") {
        when (uiState.viewMode) {
            HealthViewMode.DAY -> {
                SummaryRow("STEPS", uiState.currentDay.steps?.toString() ?: "--")
                SummaryRow("SLEEP", uiState.currentDay.sleepMinutes?.let(::formatSleepMinutes) ?: "--")
                SummaryRow("HR_AVG", uiState.currentDay.heartRateAvg?.let { "$it bpm" } ?: "--")
                SummaryRow("ACT_KCAL", uiState.currentDay.activeCalories?.let { "%.0f kcal".format(it) } ?: "--")
            }
            HealthViewMode.WEEK -> {
                val week = uiState.currentWeek
                SummaryRow("STEPS_AVG", avgLong(week.mapNotNull { it.steps })?.toString() ?: "--")
                SummaryRow("SLEEP_AVG", avgLong(week.mapNotNull { it.sleepMinutes })?.let(::formatSleepMinutes) ?: "--")
                SummaryRow("HR_AVG", avgLong(week.mapNotNull { it.heartRateAvg })?.let { "$it bpm" } ?: "--")
                SummaryRow("ACT_KCAL_SUM", week.mapNotNull { it.activeCalories }.sum().takeIf { it > 0 }?.let { "%.0f kcal".format(it) } ?: "--")
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = GlacierMuted, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = GlacierTeal, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun CategorySection(
    category: HealthCategory,
    uiState: HealthConnectUiState,
    onNavigateChart: (String) -> Unit,
    onNavigateSleepStage: (String) -> Unit,
) {
    GlacierSectionCard(title = category.label) {
        category.metrics.forEach { metric ->
            MetricRow(
                metric = metric,
                uiState = uiState,
                onClick = {
                    if (metric == HealthMetric.SLEEP_MINUTES) {
                        onNavigateSleepStage(uiState.anchorDate.toString())
                    } else {
                        onNavigateChart(metric.key)
                    }
                },
            )
        }
    }
}

@Composable
private fun MetricRow(
    metric: HealthMetric,
    uiState: HealthConnectUiState,
    onClick: () -> Unit,
) {
    val value = when (uiState.viewMode) {
        HealthViewMode.DAY -> metric.valueOf(uiState.currentDay)
        HealthViewMode.WEEK -> uiState.currentWeek.mapNotNull { metric.valueOf(it) }.let {
            if (it.isEmpty()) null else it.average()
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = metric.label,
            color = GlacierOnSurface,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (value == null) "--" else formatMetricValue(value, metric),
            color = if (value == null) GlacierMuted else GlacierCyan,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun PermissionRequestCard(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(GlacierSurfaceLow).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "! PERMISSION_DENIED",
            color = GlacierAmber,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
        )
        GlacierButton(label = "REQUEST_PERMISSION", onClick = onRequest)
    }
}

@Composable
private fun WarningCard(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().background(GlacierSurfaceLow).padding(12.dp),
    ) {
        Text(text, color = ERROR_RED, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    }
}

@Composable
private fun BackfillCard(
    progress: BackfillProgress?,
    enabled: Boolean,
    onBackfill: (Int) -> Unit,
) {
    GlacierSectionCard(title = "BACKFILL") {
        if (progress != null) {
            Text(
                text = "SYNCING ${progress.done}/${progress.total}...",
                color = GlacierAmber,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(7, 30, 90).forEach { d ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (enabled) GlacierAmber else GlacierSurfaceLow)
                            .clickable(enabled = enabled) { onBackfill(d) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${d}D",
                            color = if (enabled) GlacierBg else GlacierMuted,
                            fontFamily = SpaceGroteskFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }
}

private val ERROR_RED = Color(0xFFE57373)

private fun avgLong(values: List<Long>): Long? =
    if (values.isEmpty()) null else values.sum() / values.size
