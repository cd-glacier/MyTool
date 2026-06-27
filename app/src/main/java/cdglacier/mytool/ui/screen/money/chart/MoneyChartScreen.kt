package cdglacier.mytool.ui.screen.money.chart

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
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

private const val INITIAL_VISIBLE_MONTHS = 12

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
                .verticalScroll(rememberScrollState())
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
                    var selectedIndex by remember(uiState.series) { mutableStateOf<Int?>(null) }
                    LineChart(
                        months = uiState.months,
                        series = uiState.series,
                        selectedIndex = selectedIndex,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Legend(
                        series = uiState.series,
                        selectedIndex = selectedIndex,
                        onToggle = { idx ->
                            selectedIndex = if (selectedIndex == idx) null else idx
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LineChart(
    months: List<java.time.YearMonth>,
    series: List<ChartSeries>,
    selectedIndex: Int?,
) {
    val chartHeight = 220.dp
    val axisWidth = 56.dp

    val visibleSeries = remember(series, selectedIndex) {
        if (selectedIndex == null) series.withIndex().toList()
        else series.getOrNull(selectedIndex)?.let { listOf(IndexedValue(selectedIndex, it)) }
            ?: emptyList()
    }
    val globalMax = remember(visibleSeries) {
        var max = 0L
        for ((_, s) in visibleSeries) for (v in s.values) if (v > max) max = v
        max.coerceAtLeast(1L)
    }

    Row(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
        val listState = rememberLazyListState()

        LaunchedEffect(months.size) {
            val start = (months.size - INITIAL_VISIBLE_MONTHS).coerceAtLeast(0)
            if (start > 0) listState.scrollToItem(start)
        }

        Column(
            modifier = Modifier.width(axisWidth).fillMaxSize(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            AxisLabel(formatYenShort(globalMax))
            AxisLabel(formatYenShort(globalMax / 2))
            AxisLabel("¥0")
        }

        Spacer(modifier = Modifier.width(6.dp))

        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            items(months.size) { index ->
                MonthCell(
                    index = index,
                    months = months,
                    series = visibleSeries,
                    visibleMax = globalMax,
                    showValueLabel = selectedIndex != null,
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
    series: List<IndexedValue<ChartSeries>>,
    visibleMax: Long,
    showValueLabel: Boolean,
    cellWidthDp: androidx.compose.ui.unit.Dp,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = remember {
        TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
    }

    Column(
        modifier = Modifier.width(cellWidthDp).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
        ) {
            drawLine(
                color = GlacierOutline.copy(alpha = 0.3f),
                start = Offset(size.width, 0f),
                end = Offset(size.width, size.height),
                strokeWidth = 1f,
            )
            drawLine(
                color = GlacierOutline.copy(alpha = 0.5f),
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 1f,
            )

            series.forEach { (originalIdx, s) ->
                val color = SERIES_COLORS[originalIdx % SERIES_COLORS.size]
                val cur = s.values.getOrNull(index) ?: 0L
                val next = s.values.getOrNull(index + 1)
                val yCur = pointY(cur, visibleMax, size.height)
                val xCur = 0f
                val xNext = size.width
                if (next != null) {
                    val yNext = pointY(next, visibleMax, size.height)
                    drawLine(
                        color = color,
                        start = Offset(xCur, yCur),
                        end = Offset(xNext, yNext),
                        strokeWidth = 3f,
                    )
                }
                drawCircle(
                    color = color,
                    radius = 3.5f,
                    center = Offset(xCur, yCur),
                )
                if (showValueLabel) {
                    val label = formatYenShort(cur)
                    val layout = textMeasurer.measure(label, labelStyle)
                    val tx = (xCur - layout.size.width / 2f).coerceAtLeast(0f)
                    val ty = (yCur - layout.size.height - 4f).coerceAtLeast(0f)
                    drawText(layout, topLeft = Offset(tx, ty))
                }
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
private fun Legend(
    series: List<ChartSeries>,
    selectedIndex: Int?,
    onToggle: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        series.forEachIndexed { idx, s ->
            val color = SERIES_COLORS[idx % SERIES_COLORS.size]
            val isSelected = selectedIndex == idx
            val dimmed = selectedIndex != null && !isSelected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(idx) }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .height(10.dp)
                        .background(if (dimmed) color.copy(alpha = 0.3f) else color),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = s.name,
                    color = when {
                        isSelected -> GlacierAmber
                        dimmed -> GlacierMuted
                        else -> GlacierOnSurface
                    },
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

