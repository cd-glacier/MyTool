package cdglacier.mytool.ui.screen.healthconnect.sleepstage

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cdglacier.mytool.domain.model.SleepStageType
import cdglacier.mytool.ui.component.GlacierSectionCard
import cdglacier.mytool.ui.component.GlacierTopBar
import cdglacier.mytool.ui.theme.GlacierAmber
import cdglacier.mytool.ui.theme.GlacierBg
import cdglacier.mytool.ui.theme.GlacierCyan
import cdglacier.mytool.ui.theme.GlacierIce
import cdglacier.mytool.ui.theme.GlacierMuted
import cdglacier.mytool.ui.theme.GlacierOnSurface
import cdglacier.mytool.ui.theme.GlacierOutline
import cdglacier.mytool.ui.theme.GlacierSurface
import cdglacier.mytool.ui.theme.GlacierTeal
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SleepStageChartRoute(
    dateIso: String,
    onBack: () -> Unit,
    viewModel: SleepStageChartViewModel = viewModel(),
) {
    LaunchedEffect(dateIso) {
        val d = runCatching { LocalDate.parse(dateIso) }.getOrDefault(LocalDate.now())
        viewModel.setDate(d)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SleepStageChartScreen(
        uiState = uiState,
        onBack = onBack,
        onDateChange = viewModel::onDateChange,
    )
}

@Composable
fun SleepStageChartScreen(
    uiState: SleepStageChartUiState,
    onBack: () -> Unit,
    onDateChange: (Long) -> Unit,
) {
    Scaffold(
        topBar = { GlacierTopBar(title = "SLEEP_STAGES", onBack = onBack) },
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
            DateHeader(date = uiState.date, onPrev = { onDateChange(-1) }, onNext = { onDateChange(1) })

            if (!uiState.hasData) {
                GlacierSectionCard(title = "TIMELINE") {
                    Text("(no data)", color = GlacierMuted, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            } else {
                GlacierSectionCard(title = "TIMELINE") {
                    StageTimeline(uiState = uiState)
                }
                GlacierSectionCard(title = "LEGEND") {
                    Legend()
                }
                GlacierSectionCard(title = "SUMMARY") {
                    StageSummary(uiState = uiState)
                }
            }
        }
    }
}

@Composable
private fun DateHeader(date: LocalDate, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ArrowButton("<", onPrev)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = date.toString(),
            color = GlacierCyan,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f),
        )
        ArrowButton(">", onNext)
    }
}

@Composable
private fun ArrowButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(GlacierSurface)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = GlacierAmber, fontFamily = FontFamily.Monospace, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StageTimeline(uiState: SleepStageChartUiState) {
    val start = uiState.windowStart ?: return
    val end = uiState.windowEnd ?: return
    val totalMs = (end.toEpochMilli() - start.toEpochMilli()).coerceAtLeast(1L)
    val zone = ZoneId.systemDefault()
    val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = start.atZone(zone).format(timeFmt),
                color = GlacierMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = end.atZone(zone).format(timeFmt),
                color = GlacierMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp,
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(GlacierOutline.copy(alpha = 0.2f)),
        ) {
            for (band in uiState.bands) {
                val bandStartMs = band.start.toEpochMilli() - start.toEpochMilli()
                val bandEndMs = band.end.toEpochMilli() - start.toEpochMilli()
                val x = size.width * (bandStartMs.coerceAtLeast(0).toFloat() / totalMs.toFloat())
                val w = size.width * ((bandEndMs - bandStartMs).coerceAtLeast(0).toFloat() / totalMs.toFloat())
                drawRect(
                    color = stageColor(band.type),
                    topLeft = Offset(x, 0f),
                    size = Size(w.coerceAtLeast(1f), size.height),
                )
            }
        }
        val durationMin = Duration.between(start, end).toMinutes()
        Text(
            text = "IN_BED ${formatHm(durationMin)} / SLEEP ${formatHm(uiState.totalMinutes - awakeMinutes(uiState))}",
            color = GlacierMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp,
        )
    }
}

@Composable
private fun Legend() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SleepStageChartViewModel.STAGE_DISPLAY_ORDER.forEach { type ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 16.dp, height = 10.dp)
                        .background(stageColor(type)),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = type.label,
                    color = GlacierOnSurface,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun StageSummary(uiState: SleepStageChartUiState) {
    SleepStageChartViewModel.STAGE_DISPLAY_ORDER.forEach { type ->
        val minutes = uiState.stageTotals[type] ?: return@forEach
        if (minutes <= 0L) return@forEach
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 12.dp, height = 12.dp)
                    .background(stageColor(type)),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                type.label,
                color = GlacierMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                formatHm(minutes),
                color = GlacierCyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
            )
        }
    }
}

private fun awakeMinutes(uiState: SleepStageChartUiState): Long =
    (uiState.stageTotals[SleepStageType.AWAKE] ?: 0L) +
        (uiState.stageTotals[SleepStageType.AWAKE_IN_BED] ?: 0L) +
        (uiState.stageTotals[SleepStageType.OUT_OF_BED] ?: 0L)

private fun formatHm(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (m == 0L) "${h} hour" else "${h} hour ${m} min"
}

private fun stageColor(type: SleepStageType): Color = when (type) {
    SleepStageType.DEEP -> Color(0xFF3F51B5)
    SleepStageType.REM -> GlacierAmber
    SleepStageType.LIGHT -> GlacierTeal
    SleepStageType.SLEEPING -> GlacierCyan
    SleepStageType.AWAKE -> Color(0xFFE57373)
    SleepStageType.AWAKE_IN_BED -> Color(0xFFEF9A9A)
    SleepStageType.OUT_OF_BED -> Color(0xFFFFB74D)
    SleepStageType.UNKNOWN -> GlacierIce.copy(alpha = 0.4f)
}
