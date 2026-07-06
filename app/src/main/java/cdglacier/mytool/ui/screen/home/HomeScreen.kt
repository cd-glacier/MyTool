package cdglacier.mytool.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cdglacier.mytool.domain.usecase.DailyActivity
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
fun HomeRoute(
    onNavigateToCopyObsidianJournal: () -> Unit,
    onNavigateToHabitTracking: () -> Unit,
    onNavigateToPositionTracking: () -> Unit,
    onNavigateToMoney: () -> Unit,
    onNavigateToRecipe: () -> Unit,
    onNavigateToHousehold: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    HomeScreen(
        uiState = uiState,
        onSelectDate = viewModel::onSelectDate,
        onNavigateToCopyObsidianJournal = onNavigateToCopyObsidianJournal,
        onNavigateToHabitTracking = onNavigateToHabitTracking,
        onNavigateToPositionTracking = onNavigateToPositionTracking,
        onNavigateToMoney = onNavigateToMoney,
        onNavigateToRecipe = onNavigateToRecipe,
        onNavigateToHousehold = onNavigateToHousehold,
        onNavigateToSettings = onNavigateToSettings,
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onSelectDate: (LocalDate) -> Unit,
    onNavigateToCopyObsidianJournal: () -> Unit,
    onNavigateToHabitTracking: () -> Unit,
    onNavigateToPositionTracking: () -> Unit,
    onNavigateToMoney: () -> Unit,
    onNavigateToRecipe: () -> Unit,
    onNavigateToHousehold: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    Scaffold(
        topBar = { TerminalTopBar() },
        containerColor = GlacierBg,
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            ObsidianStatusCard(uiState = uiState, onSelectDate = onSelectDate)
            Spacer(modifier = Modifier.height(32.dp))
            ExecCommandsSection(
                onNavigateToCopyObsidianJournal = onNavigateToCopyObsidianJournal,
                onNavigateToHabitTracking = onNavigateToHabitTracking,
                onNavigateToPositionTracking = onNavigateToPositionTracking,
                onNavigateToMoney = onNavigateToMoney,
                onNavigateToRecipe = onNavigateToRecipe,
                onNavigateToHousehold = onNavigateToHousehold,
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
private fun ObsidianStatusCard(uiState: HomeUiState, onSelectDate: (LocalDate) -> Unit) {
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
            if (uiState.isLoading) {
                Text(
                    text = "LOADING...",
                    color = GlacierMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        ActivityGraph(
            dailyActivities = uiState.dailyActivities,
            selectedDate = uiState.selectedDate,
            isLoading = uiState.isLoading,
            onSelectDate = onSelectDate,
        )

        Spacer(modifier = Modifier.height(12.dp))

        ActivityBreakdown(
            date = uiState.selectedDate,
            activity = uiState.dailyActivities[uiState.selectedDate],
        )
    }
}

@Composable
private fun ActivityBreakdown(
    date: LocalDate,
    activity: DailyActivity?,
) {
    val habitPercent = ((activity?.habitRate ?: 0f) * 100).toInt()
    val distanceKm = (activity?.distanceMeters ?: 0.0) / 1000.0
    val activityPercent = ((activity?.activityRate ?: 0f) * 100).toInt()
    Column {
        Text(
            text = "DATE: $date",
            color = GlacierMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "HABIT:    $habitPercent%",
            color = GlacierOnSurface,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
        )
        Text(
            text = "DIST:     %.2f km".format(distanceKm),
            color = GlacierOnSurface,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
        )
        Text(
            text = "ACTIVITY: $activityPercent%",
            color = GlacierTeal,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
        )
    }
}

private fun activityRateToColor(rate: Float?, isLoading: Boolean): Color {
    val dim = if (isLoading) 0.3f else 1f
    if (rate == null) return GlacierSurface.copy(alpha = dim)
    val clamped = rate.coerceIn(0f, 1f)
    val alpha = (0.15f + 0.85f * clamped) * dim
    return GlacierTeal.copy(alpha = alpha)
}

@Composable
private fun ActivityGraph(
    dailyActivities: Map<LocalDate, DailyActivity>,
    selectedDate: LocalDate,
    isLoading: Boolean,
    onSelectDate: (LocalDate) -> Unit,
) {
    val today = LocalDate.now()
    val cellSize = 12.dp
    val cellGap = 2.dp

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val weekCount = ((maxWidth + cellGap) / (cellSize + cellGap)).toInt()
        val thisSunday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val startSunday = thisSunday.minusWeeks((weekCount - 1).toLong())

        Column(verticalArrangement = Arrangement.spacedBy(cellGap)) {
            for (dayOfWeekIndex in 0 until 7) {
                Row(horizontalArrangement = Arrangement.spacedBy(cellGap)) {
                    for (weekIndex in 0 until weekCount) {
                        val date = startSunday
                            .plusWeeks(weekIndex.toLong())
                            .plusDays(dayOfWeekIndex.toLong())
                        val isFuture = date.isAfter(today)
                        val color = if (isFuture) {
                            Color.Transparent
                        } else {
                            activityRateToColor(dailyActivities[date]?.activityRate, isLoading)
                        }
                        val isSelected = !isFuture && date == selectedDate
                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .background(color)
                                .then(
                                    if (isSelected) Modifier.border(1.dp, GlacierAmber)
                                    else Modifier
                                )
                                .then(
                                    if (!isFuture) Modifier.clickable { onSelectDate(date) }
                                    else Modifier
                                )
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
    onNavigateToMoney: () -> Unit,
    onNavigateToRecipe: () -> Unit,
    onNavigateToHousehold: () -> Unit,
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
        label = "MONEY_BOOK",
        onClick = onNavigateToMoney,
    )
    Spacer(modifier = Modifier.height(2.dp))
    CommandMenuItem(
        number = "05.",
        label = "RECIPES",
        onClick = onNavigateToRecipe,
    )
    Spacer(modifier = Modifier.height(2.dp))
    CommandMenuItem(
        number = "06.",
        label = "HOUSEHOLD",
        onClick = onNavigateToHousehold,
    )
    Spacer(modifier = Modifier.height(2.dp))
    CommandMenuItem(
        number = "07.",
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
