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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cdglacier.mytool.ui.theme.GruvboxBg
import cdglacier.mytool.ui.theme.GruvboxGreen
import cdglacier.mytool.ui.theme.GruvboxMuted
import cdglacier.mytool.ui.theme.GruvboxOnPrimary
import cdglacier.mytool.ui.theme.GruvboxOnSurface
import cdglacier.mytool.ui.theme.GruvboxRed
import cdglacier.mytool.ui.theme.GruvboxSurface
import cdglacier.mytool.ui.theme.GruvboxSurfaceLow
import cdglacier.mytool.ui.theme.GruvboxYellow
import cdglacier.mytool.ui.theme.SpaceGroteskFamily
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onNavigateToCopyObsidianJournal: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TerminalTopBar() },
        containerColor = GruvboxBg,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            ObsidianStatusCard(uiState = uiState)
            Spacer(modifier = Modifier.height(32.dp))
            ExecCommandsSection(
                onNavigateToCopyObsidianJournal = onNavigateToCopyObsidianJournal,
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
            .background(GruvboxBg)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(32.dp)
                .background(GruvboxRed),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = ">_",
                color = GruvboxOnPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "MY_TOOL",
            color = GruvboxRed,
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
            .background(GruvboxSurfaceLow)
            .drawBehind {
                drawRect(
                    color = GruvboxYellow,
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
                text = "JOURNAL_ACTIVITY",
                color = GruvboxMuted,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.weight(1f),
            )
            val statusText = if (uiState.journalDirUri != null) "CONFIGURED" else "NOT_SET"
            val statusColor = if (uiState.journalDirUri != null) GruvboxGreen else GruvboxRed
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

        // Contribution graph
        JournalContributionGraph(
            lineCounts = uiState.journalLineCounts,
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
                color = GruvboxRed,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Column {
                Text(
                    text = "OBSIDIAN_DIR: NOT_SET",
                    color = GruvboxRed,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "設定が必要です。SYS_SETTINGS から設定してください。",
                    color = GruvboxMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                )
            }
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "✓ ",
                color = GruvboxGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Column {
                Text(
                    text = "OBSIDIAN_DIR: CONFIGURED",
                    color = GruvboxGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = uiState.journalDirUri.lastPathSegment ?: uiState.journalDirUri.toString(),
                    color = GruvboxMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun lineCountToColor(count: Int, isLoading: Boolean): Color {
    val alpha = if (isLoading) 0.3f else 1f
    return when {
        count == 0 -> GruvboxSurface.copy(alpha = alpha)
        count <= 25 -> GruvboxGreen.copy(alpha = 0.25f * alpha)
        count <= 50 -> GruvboxGreen.copy(alpha = 0.5f * alpha)
        count <= 100 -> GruvboxGreen.copy(alpha = 0.75f * alpha)
        else -> GruvboxGreen.copy(alpha = alpha)
    }
}

@Composable
private fun JournalContributionGraph(
    lineCounts: Map<LocalDate, Int>,
    isLoading: Boolean,
) {
    val today = LocalDate.now()
    val cellSize = 12.dp
    val cellGap = 2.dp

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // 利用可能な幅からセルがいくつ並ぶか（= 週数）を計算
        val weekCount = ((maxWidth + cellGap) / (cellSize + cellGap)).toInt()
        val startMonday = today
            .minusWeeks((weekCount - 1).toLong())
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        Column(verticalArrangement = Arrangement.spacedBy(cellGap)) {
            for (dayOfWeekIndex in 0 until 7) {
                Row(horizontalArrangement = Arrangement.spacedBy(cellGap)) {
                    for (weekIndex in 0 until weekCount) {
                        val date = startMonday
                            .plusWeeks(weekIndex.toLong())
                            .plusDays(dayOfWeekIndex.toLong())
                        val count = if (date.isAfter(today)) -1 else lineCounts[date] ?: 0
                        val color = if (count < 0) {
                            Color.Transparent
                        } else {
                            lineCountToColor(count, isLoading)
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
                .background(GruvboxSurface)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "EXEC_COMMANDS",
            color = GruvboxMuted,
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
            .background(if (isPressed) GruvboxYellow else GruvboxSurface)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(56.dp)
                .align(Alignment.CenterStart)
                .background(GruvboxYellow.copy(alpha = if (isPressed) 1f else 0f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = number,
                color = if (isPressed) GruvboxOnPrimary else GruvboxYellow,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                color = if (isPressed) GruvboxOnPrimary else GruvboxOnSurface,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = ">",
                color = if (isPressed) GruvboxOnPrimary else GruvboxYellow,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
            )
        }
    }
}
