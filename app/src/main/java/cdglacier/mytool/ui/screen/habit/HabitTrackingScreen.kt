package cdglacier.mytool.ui.screen.habit

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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cdglacier.mytool.domain.model.Habit
import cdglacier.mytool.domain.model.HabitFrequency
import cdglacier.mytool.ui.theme.GruvboxBg
import cdglacier.mytool.ui.theme.GruvboxGreen
import cdglacier.mytool.ui.theme.GruvboxMuted
import cdglacier.mytool.ui.theme.GruvboxOnSurface
import cdglacier.mytool.ui.theme.GruvboxRed
import cdglacier.mytool.ui.theme.GruvboxSurface
import cdglacier.mytool.ui.theme.GruvboxSurfaceLow
import cdglacier.mytool.ui.theme.GruvboxYellow
import cdglacier.mytool.ui.theme.SpaceGroteskFamily
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HabitTrackingScreen(
    viewModel: HabitTrackingViewModel = viewModel(),
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onErrorShown()
        }
    }

    Scaffold(
        topBar = { HabitTopBar(onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = GruvboxBg,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            DateHeader(uiState)
            Spacer(modifier = Modifier.height(16.dp))
            when {
                !uiState.journalConfigured -> NoticeCard(
                    "OBSIDIAN_DIR: NOT_SET",
                    "SYS_SETTINGS から Journal フォルダを設定してください。",
                )
                uiState.isLoading -> NoticeCard("LOADING", "...")
                uiState.habits.isEmpty() -> NoticeCard(
                    "NO_HABITS",
                    "今日のJournalに該当する習慣がありません。\n`# Habit` セクションを追加してください。",
                )
                else -> HabitList(uiState.habits, viewModel::onHabitToggle)
            }
            Spacer(modifier = Modifier.height(24.dp))
            SyncHistorySection(
                uiState = uiState,
                onSync = viewModel::onSyncHistory,
            )
        }
    }
}

@Composable
private fun SyncHistorySection(
    uiState: HabitTrackingUiState,
    onSync: () -> Unit,
) {
    val syncedAtText = uiState.lastSyncedAtEpochMillis?.let { millis ->
        val fmt = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime().format(fmt)
    } ?: "NEVER"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GruvboxSurfaceLow)
            .padding(16.dp)
    ) {
        Text(
            text = "HISTORY_CACHE",
            color = GruvboxMuted,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 2.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "LAST_SYNC: $syncedAtText",
            color = GruvboxOnSurface,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
        )
        Text(
            text = "CACHED_DAYS: ${uiState.historyDayCount}",
            color = GruvboxOnSurface,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (uiState.isSyncingHistory) GruvboxSurface else GruvboxYellow
                )
                .clickable(enabled = !uiState.isSyncingHistory && uiState.journalConfigured) {
                    onSync()
                }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (uiState.isSyncingHistory) "SYNCING..." else "SYNC_HISTORY",
                color = if (uiState.isSyncingHistory) GruvboxMuted else GruvboxBg,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                letterSpacing = 1.sp,
            )
        }
    }
}

@Composable
private fun HabitTopBar(onBack: () -> Unit) {
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
                .size(32.dp)
                .background(GruvboxSurface)
                .clickable { onBack() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "<",
                color = GruvboxYellow,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "HABIT_TRACKING",
            color = GruvboxOnSurface,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun DateHeader(uiState: HabitTrackingUiState) {
    val displayFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd (E)")
    val total = uiState.habits.size
    val done = uiState.habits.count { it.isCompleted }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GruvboxSurfaceLow)
            .drawBehind {
                drawRect(
                    color = GruvboxYellow,
                    topLeft = Offset.Zero,
                    size = Size(width = 4.dp.toPx(), height = size.height),
                )
            }
            .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
    ) {
        Text(
            text = "TODAY",
            color = GruvboxMuted,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 2.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = uiState.date.format(displayFormatter),
            color = GruvboxOnSurface,
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
        )
        if (total > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$done / $total COMPLETED",
                color = GruvboxGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun NoticeCard(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GruvboxSurface)
            .padding(16.dp)
    ) {
        Text(
            text = title,
            color = GruvboxRed,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = body,
            color = GruvboxMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun HabitList(habits: List<Habit>, onToggle: (Habit) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        habits.forEach { habit ->
            HabitRow(habit = habit, onToggle = { onToggle(habit) })
        }
    }
}

@Composable
private fun HabitRow(habit: Habit, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GruvboxSurface)
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(if (habit.isCompleted) GruvboxGreen else GruvboxBg),
            contentAlignment = Alignment.Center,
        ) {
            if (habit.isCompleted) {
                Text(
                    text = "x",
                    color = GruvboxBg,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = habit.name,
                color = GruvboxOnSurface,
                fontFamily = SpaceGroteskFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = habit.frequency.label(),
                color = GruvboxMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            )
        }
    }
}

private fun HabitFrequency.label(): String = when (this) {
    is HabitFrequency.Daily -> "DAILY"
    is HabitFrequency.Weekly -> "EVERY ${dayOfWeek.name}"
}
