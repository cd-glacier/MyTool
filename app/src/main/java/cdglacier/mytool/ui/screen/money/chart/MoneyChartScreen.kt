package cdglacier.mytool.ui.screen.money.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cdglacier.mytool.ui.component.GlacierSectionCard
import cdglacier.mytool.ui.component.GlacierTopBar
import cdglacier.mytool.ui.theme.GlacierAmber
import cdglacier.mytool.ui.theme.GlacierBg
import cdglacier.mytool.ui.theme.GlacierCyan
import cdglacier.mytool.ui.theme.GlacierIce
import cdglacier.mytool.ui.theme.GlacierMuted
import cdglacier.mytool.ui.theme.GlacierOnSurface
import cdglacier.mytool.ui.theme.GlacierOutline
import cdglacier.mytool.ui.theme.GlacierTeal

private val SERIES_COLORS = listOf(
    GlacierCyan, GlacierAmber, GlacierTeal, GlacierIce, GlacierOnSurface,
)

private const val INITIAL_VISIBLE_MONTHS = 6

@Composable
fun MoneyChartScreen(
    section: String,
    group: String?,
    onBack: () -> Unit,
    viewModel: MoneyChartViewModel = viewModel(),
) {
    LaunchedEffect(section, group) {
        viewModel.setTarget(MoneySection.fromKey(section), group?.takeIf { it.isNotEmpty() })
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MoneyChartContent(uiState = uiState, onBack = onBack)
}

@Composable
private fun MoneyChartContent(
    uiState: MoneyChartUiState,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = { GlacierTopBar(title = uiState.title, onBack = onBack) },
        containerColor = GlacierBg,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GlacierSectionCard(title = "CHART_ALL_MONTHS") {
                if (uiState.series.isEmpty() || uiState.months.isEmpty()) {
                    Text(
                        text = "(no data)",
                        color = GlacierMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    )
                } else {
                    LineChart(months = uiState.months, series = uiState.series)
                    Spacer(modifier = Modifier.height(12.dp))
                    Legend(series = uiState.series)
                }
            }
        }
    }
}

@Composable
private fun LineChart(
    months: List<java.time.YearMonth>,
    series: List<ChartSeries>,
) {
    val chartHeight = 220.dp
    val axisWidth = 56.dp

    Row(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
        // visible-range computed from scroll position
        val listState = rememberLazyListState()
        val visibleMax by remember(months, series) {
            derivedStateOf {
                val firstIndex = listState.firstVisibleItemIndex
                val visibleCount = INITIAL_VISIBLE_MONTHS
                val end = (firstIndex + visibleCount).coerceAtMost(months.size)
                var max = 0L
                for (s in series) {
                    for (i in firstIndex until end) {
                        val v = s.values.getOrNull(i) ?: 0L
                        if (v > max) max = v
                    }
                }
                max.coerceAtLeast(1L)
            }
        }

        LaunchedEffect(months.size) {
            val start = (months.size - INITIAL_VISIBLE_MONTHS).coerceAtLeast(0)
            if (start > 0) listState.scrollToItem(start)
        }

        // Y-axis labels
        Column(
            modifier = Modifier.width(axisWidth).fillMaxSize(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            AxisLabel(formatYenShort(visibleMax))
            AxisLabel(formatYenShort(visibleMax / 2))
            AxisLabel("¥0")
        }

        Spacer(modifier = Modifier.width(6.dp))

        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            // Render all months in one Canvas-backed item per month for cell-based painting
            items(months.size) { index ->
                MonthCell(
                    index = index,
                    months = months,
                    series = series,
                    visibleMax = visibleMax,
                    cellWidthDp = 54.dp,
                )
            }
        }
    }
}

@Composable
private fun MonthCell(
    index: Int,
    months: List<java.time.YearMonth>,
    series: List<ChartSeries>,
    visibleMax: Long,
    cellWidthDp: androidx.compose.ui.unit.Dp,
) {
    Column(
        modifier = Modifier.width(cellWidthDp).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
        ) {
            // grid line at right edge
            drawLine(
                color = GlacierOutline.copy(alpha = 0.3f),
                start = Offset(size.width, 0f),
                end = Offset(size.width, size.height),
                strokeWidth = 1f,
            )
            // baseline
            drawLine(
                color = GlacierOutline.copy(alpha = 0.5f),
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 1f,
            )

            series.forEachIndexed { sIdx, s ->
                val color = SERIES_COLORS[sIdx % SERIES_COLORS.size]
                val cur = s.values.getOrNull(index) ?: 0L
                val next = s.values.getOrNull(index + 1)
                val yCur = pointY(cur, visibleMax, size.height)
                val xCur = 0f
                val xNext = size.width
                // line to next
                if (next != null) {
                    val yNext = pointY(next, visibleMax, size.height)
                    drawLine(
                        color = color,
                        start = Offset(xCur, yCur),
                        end = Offset(xNext, yNext),
                        strokeWidth = 3f,
                    )
                }
                // point
                drawCircle(
                    color = color,
                    radius = 3.5f,
                    center = Offset(xCur, yCur),
                )
            }
        }
        Text(
            text = months[index].toString().takeLast(7), // yyyy-MM
            color = GlacierMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private fun pointY(value: Long, max: Long, height: Float): Float {
    val ratio = (value.toFloat() / max.toFloat()).coerceIn(0f, 1f)
    return height - height * ratio
}

@Composable
private fun AxisLabel(text: String) {
    Text(
        text = text,
        color = GlacierMuted,
        fontFamily = FontFamily.Monospace,
        fontSize = 9.sp,
    )
}

@Composable
private fun Legend(series: List<ChartSeries>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        series.forEachIndexed { idx, s ->
            val color = SERIES_COLORS[idx % SERIES_COLORS.size]
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .height(10.dp)
                        .background(color),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = s.name,
                    color = GlacierOnSurface,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

private fun formatYenShort(amount: Long): String = when {
    amount >= 1_000_000 -> "¥%.1fM".format(amount / 1_000_000.0)
    amount >= 1_000 -> "¥%dk".format(amount / 1_000)
    else -> "¥$amount"
}

