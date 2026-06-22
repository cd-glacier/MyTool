package cdglacier.mytool.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cdglacier.mytool.data.repository.TrackingMode
import cdglacier.mytool.ui.theme.GlacierAmber
import cdglacier.mytool.ui.theme.GlacierBg
import cdglacier.mytool.ui.theme.GlacierCyan
import cdglacier.mytool.ui.theme.GlacierMuted
import cdglacier.mytool.ui.theme.GlacierOnPrimary
import cdglacier.mytool.ui.theme.GlacierOnSurface
import cdglacier.mytool.ui.theme.GlacierSurface
import cdglacier.mytool.ui.theme.GlacierSurfaceLow
import cdglacier.mytool.ui.theme.GlacierTeal
import cdglacier.mytool.ui.theme.SpaceGroteskFamily
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onNavigateToCopyObsidianJournal: () -> Unit,
    onNavigateToHabitTracking: () -> Unit,
    onNavigateToPositionTracking: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = { TerminalTopBar() },
        containerColor = GlacierBg,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            ObsidianStatusCard(uiState = uiState)
            Spacer(modifier = Modifier.height(16.dp))
            PositionTrackingStatusCard(uiState = uiState)
            Spacer(modifier = Modifier.height(32.dp))
            ExecCommandsSection(
                onNavigateToCopyObsidianJournal = onNavigateToCopyObsidianJournal,
                onNavigateToHabitTracking = onNavigateToHabitTracking,
                onNavigateToPositionTracking = onNavigateToPositionTracking,
                onNavigateToSettings = onNavigateToSettings,
            )
        }
    }
}

@Composable
private fun TerminalTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlacierBg)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(32.dp)
                .background(GlacierCyan),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = ">_",
                color = GlacierOnPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "MY_TOOL",
            color = GlacierCyan,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            letterSpacing = (-0.5).sp,
        )
    }
}

@Composable
private fun ObsidianStatusCard(uiState: HomeUiState) {
    val yellowBorderWidth = 4.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlacierSurfaceLow)
            .drawBehind {
                drawRect(
                    color = GlacierAmber,
                    topLeft = Offset.Zero,
                    size = Size(width = yellowBorderWidth.toPx(), height = size.height),
                )
            }
            .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
    ) {
        // Section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "ACTIVITY",
                color = GlacierMuted,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.weight(1f),
            )
            val statusText = if (uiState.journalDirUri != null) "CONFIGURED" else "NOT_SET"
            val statusColor = if (uiState.journalDirUri != null) GlacierTeal else GlacierAmber
            Text(
                text = statusText,
                color = statusColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Directory status row
        ObsidianDirStatusRow(uiState = uiState)

        Spacer(modifier = Modifier.height(16.dp))

        // Activity graph
        ActivityGraph(
            activityRates = uiState.activityRates,
            isLoading = uiState.isLoading,
        )
    }
}

@Composable
private fun ObsidianDirStatusRow(uiState: HomeUiState) {
    if (uiState.journalDirUri == null) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = "! ",
                color = GlacierAmber,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Column {
                Text(
                    text = "OBSIDIAN_DIR: NOT_SET",
                    color = GlacierAmber,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "設定が必要です。SYS_SETTINGS から設定してください。",
                    color = GlacierMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                )
            }
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "✓ ",
                color = GlacierTeal,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Column {
                Text(
                    text = "OBSIDIAN_DIR: CONFIGURED",
                    color = GlacierTeal,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = uiState.journalDirUri.lastPathSegment ?: uiState.journalDirUri.toString(),
                    color = GlacierMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PositionTrackingStatusCard(uiState: HomeUiState) {
    val borderWidth = 4.dp
    val accent = if (uiState.trackingEnabled) GlacierTeal else GlacierMuted
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlacierSurfaceLow)
            .drawBehind {
                drawRect(
                    color = accent,
                    topLeft = Offset.Zero,
                    size = Size(width = borderWidth.toPx(), height = size.height),
                )
            }
            .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "POSITION_TRACKING",
                color = GlacierMuted,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (uiState.trackingEnabled) "RECORDING" else "STOPPED",
                color = if (uiState.trackingEnabled) GlacierTeal else GlacierAmber,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        val (precisionLabel, precisionDetail) = when {
            !uiState.trackingEnabled -> "OFF" to "記録は停止しています"
            uiState.trackingMode == TrackingMode.MOVING -> "HIGH" to "GPS高精度 / 5秒間隔"
            else -> "BALANCED" to "省電力 / 30秒間隔"
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "PRECISION: ",
                color = GlacierMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
            )
            Text(
                text = precisionLabel,
                color = accent,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = precisionDetail,
            color = GlacierMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
        )
    }
}

private fun activityRateToColor(rate: Float?, isLoading: Boolean): Color {
    val alpha = if (isLoading) 0.3f else 1f
    if (rate == null) return GlacierSurface.copy(alpha = alpha)
    return when {
        rate <= 0f -> GlacierTeal.copy(alpha = 0.15f * alpha)
        rate < 0.5f -> GlacierTeal.copy(alpha = 0.4f * alpha)
        rate < 1f -> GlacierTeal.copy(alpha = 0.7f * alpha)
        else -> GlacierTeal.copy(alpha = alpha)
    }
}

@Composable
private fun ActivityGraph(
    activityRates: Map<LocalDate, Float?>,
    isLoading: Boolean,
) {
    val today = LocalDate.now()
    val cellSize = 12.dp
    val cellGap = 2.dp

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val weekCount = ((maxWidth + cellGap) / (cellSize + cellGap)).toInt()
        // 今週の日曜日を起点に、過去 weekCount 週分を表示する（日曜始まり）
        val thisSunday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val startSunday = thisSunday.minusWeeks((weekCount - 1).toLong())

        Column(verticalArrangement = Arrangement.spacedBy(cellGap)) {
            for (dayOfWeekIndex in 0 until 7) {
                Row(horizontalArrangement = Arrangement.spacedBy(cellGap)) {
                    for (weekIndex in 0 until weekCount) {
                        val date = startSunday
                            .plusWeeks(weekIndex.toLong())
                            .plusDays(dayOfWeekIndex.toLong())
                        val color = if (date.isAfter(today)) {
                            Color.Transparent
                        } else {
                            activityRateToColor(activityRates[date], isLoading)
                        }
                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .background(color)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExecCommandsSection(
    onNavigateToCopyObsidianJournal: () -> Unit,
    onNavigateToHabitTracking: () -> Unit,
    onNavigateToPositionTracking: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(1.dp)
                .background(GlacierSurface)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "EXEC_COMMANDS",
            color = GlacierMuted,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 2.sp,
        )
    }

    CommandMenuItem(
        number = "01.",
        label = "COPY_JOURNAL",
        onClick = onNavigateToCopyObsidianJournal,
    )
    Spacer(modifier = Modifier.height(2.dp))
    CommandMenuItem(
        number = "02.",
        label = "HABIT_TRACKING",
        onClick = onNavigateToHabitTracking,
    )
    Spacer(modifier = Modifier.height(2.dp))
    CommandMenuItem(
        number = "03.",
        label = "POSITION_TRACKING",
        onClick = onNavigateToPositionTracking,
    )
    Spacer(modifier = Modifier.height(2.dp))
    CommandMenuItem(
        number = "04.",
        label = "SYS_SETTINGS",
        onClick = onNavigateToSettings,
    )
}

@Composable
private fun CommandMenuItem(
    number: String,
    label: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isPressed) GlacierAmber else GlacierSurface)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(56.dp)
                .align(Alignment.CenterStart)
                .background(GlacierAmber.copy(alpha = if (isPressed) 1f else 0f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = number,
                color = if (isPressed) GlacierOnPrimary else GlacierAmber,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                color = if (isPressed) GlacierOnPrimary else GlacierOnSurface,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = ">",
                color = if (isPressed) GlacierOnPrimary else GlacierAmber,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
            )
        }
    }
}
