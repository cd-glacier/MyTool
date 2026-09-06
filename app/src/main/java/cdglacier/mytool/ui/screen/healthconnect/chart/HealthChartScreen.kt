package cdglacier.mytool.ui.screen.healthconnect.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cdglacier.mytool.ui.component.GlacierSectionCard
import cdglacier.mytool.ui.component.GlacierTopBar
import cdglacier.mytool.domain.model.HealthMetric
import cdglacier.mytool.ui.screen.healthconnect.formatMetricValue
import cdglacier.mytool.ui.theme.GlacierAmber
import cdglacier.mytool.ui.theme.GlacierBg
import cdglacier.mytool.ui.theme.GlacierCyan
import cdglacier.mytool.ui.theme.GlacierMuted
import cdglacier.mytool.ui.theme.GlacierOnSurface
import cdglacier.mytool.ui.theme.GlacierOutline

private const val INITIAL_VISIBLE_DAYS = 30
private const val INITIAL_VISIBLE_WEEKS = 12

@Composable
fun HealthChartRoute(
    metricKey: String,
    onBack: () -> Unit,
    viewModel: HealthChartViewModel = viewModel(),
) {
    LaunchedEffect(metricKey) { viewModel.setMetric(metricKey) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HealthChartScreen(uiState = uiState, onBack = onBack, onModeChange = viewModel::onModeChange)
}

@Composable
fun HealthChartScreen(
    uiState: HealthChartUiState,
    onBack: () -> Unit,
    onModeChange: (HealthChartMode) -> Unit,
) {
    Scaffold(
        topBar = { GlacierTopBar(title = uiState.title, onBack = onBack) },
        containerColor = GlacierBg,
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ModeSelector(mode = uiState.mode, onChange = onModeChange)

            if (uiState.points.isEmpty() || uiState.points.all { it.value == null }) {
                GlacierSectionCard(title = "CHART") {
                    Text("(no data)", color = GlacierMuted, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            } else {
                GlacierSectionCard(title = "CHART") {
                    BarChart(points = uiState.points, metric = uiState.metric, mode = uiState.mode)
                }
                GlacierSectionCard(title = "STATS") {
                    val values = uiState.points.mapNotNull { it.value }
                    StatRow("MIN", values.min(), uiState.metric)
                    StatRow("MAX", values.max(), uiState.metric)
                    StatRow("AVG", values.average(), uiState.metric)
                }
            }
        }
    }
}

@Composable
private fun ModeSelector(mode: HealthChartMode, onChange: (HealthChartMode) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().background(GlacierOutline.copy(alpha = 0.2f))) {
        HealthChartMode.values().forEach { m ->
            val selected = m == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (selected) GlacierAmber else GlacierBg)
                    .padding(vertical = 8.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = m.name,
                    color = if (selected) GlacierBg else GlacierMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: Double, metric: HealthMetric?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = GlacierMuted, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(formatMetricValue(value, metric), color = GlacierCyan, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
    }
}

@Composable
private fun BarChart(points: List<HealthChartPoint>, metric: HealthMetric?, mode: HealthChartMode) {
    val rawMax = points.mapNotNull { it.value }.max().coerceAtLeast(1.0)
    val axisStep = niceAxisStep(rawMax / 2, metric)
    val axisMax = axisStep * 2
    val cellWidth: Dp = 22.dp
    val chartHeight = 200.dp

    Row(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
        Column(
            modifier = Modifier.width(56.dp).fillMaxSize(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            AxisLabel(formatMetricValue(axisMax, metric))
            AxisLabel(formatMetricValue(axisStep, metric))
            AxisLabel("0")
        }
        Spacer(modifier = Modifier.width(6.dp))

        val listState = rememberLazyListState()
        LaunchedEffect(points.size) {
            val initial = if (mode == HealthChartMode.DAY) INITIAL_VISIBLE_DAYS else INITIAL_VISIBLE_WEEKS
            val start = (points.size - initial).coerceAtLeast(0)
            if (start > 0) listState.scrollToItem(start)
        }
        LazyRow(state = listState, modifier = Modifier.fillMaxSize()) {
            items(points) { point ->
                BarCell(point = point, visibleMax = axisMax, cellWidth = cellWidth)
            }
        }
    }
}

private fun niceAxisStep(rawStep: Double, metric: HealthMetric?): Double {
    if (rawStep <= 0.0) return 1.0
    if (metric == HealthMetric.SLEEP_MINUTES) {
        val hourCandidates = doubleArrayOf(0.5, 1.0, 2.0, 3.0, 4.0, 6.0, 8.0, 12.0)
        val hours = rawStep / 60.0
        val pickHours = hourCandidates.firstOrNull { it >= hours }
            ?: (kotlin.math.ceil(hours / 12.0) * 12.0)
        return pickHours * 60.0
    }
    val exp = kotlin.math.floor(kotlin.math.log10(rawStep))
    val pow = Math.pow(10.0, exp)
    val f = rawStep / pow
    val nf = when {
        f <= 1.0 -> 1.0
        f <= 2.0 -> 2.0
        f <= 2.5 -> 2.5
        f <= 5.0 -> 5.0
        else -> 10.0
    }
    return nf * pow
}

@Composable
private fun BarCell(point: HealthChartPoint, visibleMax: Double, cellWidth: Dp) {
    Column(
        modifier = Modifier.width(cellWidth).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(170.dp)) {
            drawLine(
                color = GlacierOutline.copy(alpha = 0.3f),
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 1f,
            )
            val v = point.value ?: return@Canvas
            val ratio = (v / visibleMax).toFloat().coerceIn(0f, 1f)
            val barHeight = size.height * ratio
            val barWidth = size.width * 0.7f
            val left = (size.width - barWidth) / 2f
            drawRect(
                color = GlacierCyan,
                topLeft = Offset(left, size.height - barHeight),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
            )
        }
        Text(
            text = point.label,
            color = GlacierMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            lineHeight = 10.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun AxisLabel(text: String) {
    Text(text = text, color = GlacierOnSurface, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
}

