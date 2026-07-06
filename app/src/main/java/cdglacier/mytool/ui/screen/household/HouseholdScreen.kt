package cdglacier.mytool.ui.screen.household

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cdglacier.mytool.domain.model.Assignee
import cdglacier.mytool.domain.model.HouseholdPoint
import cdglacier.mytool.domain.model.HouseholdSummary
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
fun HouseholdRoute(
    onBack: () -> Unit,
    onNavigateToPoints: () -> Unit,
    viewModel: HouseholdViewModel = viewModel(),
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

    HouseholdScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onPrevDate = viewModel::onPrevDate,
        onNextDate = viewModel::onNextDate,
        onOpenRecord = viewModel::onOpenRecordDialog,
        onNavigateToPoints = onNavigateToPoints,
        onCloseRecord = viewModel::onCloseRecordDialog,
        onRecordNameChange = viewModel::onRecordNameChange,
        onRecordAssigneeChange = viewModel::onRecordAssigneeChange,
        onRecordCountChange = viewModel::onRecordCountChange,
        onRecordAdjustmentChange = viewModel::onRecordAdjustmentChange,
        onSubmitRecord = viewModel::onSubmitRecord,
    )
}

@Composable
fun HouseholdScreen(
    uiState: HouseholdUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onPrevDate: () -> Unit,
    onNextDate: () -> Unit,
    onOpenRecord: () -> Unit,
    onNavigateToPoints: () -> Unit,
    onCloseRecord: () -> Unit,
    onRecordNameChange: (String) -> Unit,
    onRecordAssigneeChange: (Assignee) -> Unit,
    onRecordCountChange: (String) -> Unit,
    onRecordAdjustmentChange: (String) -> Unit,
    onSubmitRecord: () -> Unit,
) {
    Scaffold(
        topBar = { HouseholdTopBar(onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = GlacierBg,
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "LOADING...",
                    color = GlacierMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                )
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            when {
                !uiState.journalConfigured -> NoticeCard(
                    "JOURNAL_DIR: NOT_SET",
                    "SYS_SETTINGS から Journal フォルダを設定してください。",
                )
                !uiState.pagesConfigured -> NoticeCard(
                    "PAGES_DIR: NOT_SET",
                    "SYS_SETTINGS から pages フォルダを設定してください。",
                )
                else -> {
                    ManagePointsLink(onClick = onNavigateToPoints)
                    Spacer(Modifier.height(16.dp))
                    DateHeader(uiState.date, onPrevDate, onNextDate)
                    Spacer(Modifier.height(12.dp))
                    if (uiState.isSummaryLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "LOADING...",
                                color = GlacierMuted,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                            )
                        }
                    } else {
                        SummaryCard(uiState.summary)
                        Spacer(Modifier.height(16.dp))
                        BreakdownSection(uiState.summary)
                        Spacer(Modifier.height(24.dp))
                        PrimaryButton("+ RECORD", GlacierAmber, Modifier.fillMaxWidth(), onClick = onOpenRecord)
                    }
                }
            }
        }
    }

    uiState.recordDialog?.let { dialog ->
        RecordDialog(
            state = dialog,
            points = uiState.points,
            onDismiss = onCloseRecord,
            onNameChange = onRecordNameChange,
            onAssigneeChange = onRecordAssigneeChange,
            onCountChange = onRecordCountChange,
            onAdjustmentChange = onRecordAdjustmentChange,
            onSubmit = onSubmitRecord,
        )
    }

}

@Composable
private fun HouseholdTopBar(onBack: () -> Unit) {
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
                .size(32.dp)
                .background(GlacierSurface)
                .clickable { onBack() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "<",
                color = GlacierAmber,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            "HOUSEHOLD",
            color = GlacierOnSurface,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun DateHeader(date: java.time.LocalDate, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArrowButton("<", onPrev)
        Spacer(Modifier.width(12.dp))
        Text(
            text = date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd (E)")),
            color = GlacierCyan,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
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
private fun SummaryCard(summary: HouseholdSummary) {
    val total = summary.husbandTotal + summary.wifeTotal
    val husbandPct = if (total > 0) (summary.husbandTotal * 100 / total) else 0
    val wifePct = 100 - husbandPct
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlacierSurfaceLow)
            .padding(16.dp)
    ) {
        Text(
            "SCORE",
            color = GlacierMuted,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            ScoreColumn("HUSBAND", summary.husbandTotal, husbandPct, GlacierCyan, Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            ScoreColumn("WIFE", summary.wifeTotal, wifePct, GlacierTeal, Modifier.weight(1f))
        }
        if (total > 0) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            ) {
                Box(
                    Modifier
                        .weight(husbandPct.coerceAtLeast(1).toFloat())
                        .fillMaxWidth()
                        .background(GlacierCyan)
                )
                Box(
                    Modifier
                        .weight(wifePct.coerceAtLeast(1).toFloat())
                        .fillMaxWidth()
                        .background(GlacierTeal)
                )
            }
        }
    }
}

@Composable
private fun ScoreColumn(
    label: String,
    value: Int,
    pct: Int,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            label,
            color = GlacierMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value.toString(),
            color = color,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Black,
            fontSize = 28.sp,
        )
        Text(
            "$pct%",
            color = GlacierMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun BreakdownSection(summary: HouseholdSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Assignee.entries.forEach { assignee ->
            val breakdown = summary.perAssignee[assignee].orEmpty()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GlacierSurface)
                    .padding(12.dp)
            ) {
                Text(
                    assignee.key.uppercase(),
                    color = GlacierAmber,
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                )
                Spacer(Modifier.height(6.dp))
                if (breakdown.isEmpty()) {
                    Text(
                        "-- NO_ENTRY --",
                        color = GlacierMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    )
                } else {
                    breakdown.forEach { b ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                b.name,
                                modifier = Modifier.weight(1f),
                                color = GlacierOnSurface,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                            )
                            Text(
                                "x${b.count}",
                                color = GlacierMuted,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                b.effectivePoints.toString(),
                                color = GlacierTeal,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagePointsLink(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlacierSurface)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "MANAGE_POINTS",
            modifier = Modifier.weight(1f),
            color = GlacierOnSurface,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 2.sp,
        )
        Text(
            ">",
            color = GlacierAmber,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PrimaryButton(
    label: String,
    bg: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .background(bg)
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (bg == GlacierAmber) GlacierBg else if (enabled) GlacierOnSurface else GlacierMuted,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun NoticeCard(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlacierSurface)
            .padding(16.dp)
    ) {
        Text(title, color = GlacierAmber, fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(body, color = GlacierMuted, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    }
}

@Composable
private fun RecordDialog(
    state: RecordDialogState,
    points: List<HouseholdPoint>,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onAssigneeChange: (Assignee) -> Unit,
    onCountChange: (String) -> Unit,
    onAdjustmentChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val submitting = state.isSubmitting
    Dialog(onDismissRequest = { if (!submitting) onDismiss() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(GlacierBg)
                .padding(20.dp)
        ) {
            DialogTitle("RECORD_ENTRY")
            Spacer(Modifier.height(16.dp))
            FieldLabel("TASK")
            NameSelector(points = points, selected = state.selectedName, onSelect = onNameChange)
            Spacer(Modifier.height(12.dp))
            FieldLabel("ASSIGNEE")
            AssigneeSelector(state.assignee, onAssigneeChange)
            Spacer(Modifier.height(12.dp))
            FieldLabel("COUNT")
            NumberField(state.countText, onCountChange)
            Spacer(Modifier.height(12.dp))
            FieldLabel("ADJUSTMENT")
            NumberField(state.adjustmentText, onAdjustmentChange, allowSign = true)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton(
                    label = "CANCEL",
                    bg = GlacierSurface,
                    modifier = Modifier.weight(1f),
                    enabled = !submitting,
                    onClick = onDismiss,
                )
                PrimaryButton(
                    label = if (submitting) "SAVING..." else "SAVE",
                    bg = if (submitting) GlacierSurface else GlacierAmber,
                    modifier = Modifier.weight(1f),
                    enabled = !submitting,
                    onClick = onSubmit,
                )
            }
        }
    }
}

@Composable
private fun NameSelector(points: List<HouseholdPoint>, selected: String, onSelect: (String) -> Unit) {
    if (points.isEmpty()) {
        Text(
            "-- 家事が未登録です。MANAGE POINTS から追加してください --",
            color = GlacierMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
        )
        return
    }
    var expanded by remember { mutableStateOf(false) }
    val current = points.firstOrNull { it.name == selected }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GlacierSurface)
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                current?.name ?: "SELECT",
                modifier = Modifier.weight(1f),
                color = GlacierOnSurface,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            if (current != null) {
                Text(
                    "${current.points}pt",
                    color = GlacierMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                if (expanded) "^" else "v",
                color = GlacierAmber,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        if (expanded) {
            Spacer(Modifier.height(2.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                points.forEach { p ->
                    val isSelected = p.name == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) GlacierAmber else GlacierSurface)
                            .clickable {
                                onSelect(p.name)
                                expanded = false
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            p.name,
                            modifier = Modifier.weight(1f),
                            color = if (isSelected) GlacierBg else GlacierOnSurface,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${p.points}pt",
                            color = if (isSelected) GlacierBg else GlacierMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AssigneeSelector(current: Assignee, onChange: (Assignee) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Assignee.entries.forEach { a ->
            val selected = a == current
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (selected) GlacierAmber else GlacierSurface)
                    .clickable { onChange(a) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    a.key.uppercase(),
                    color = if (selected) GlacierBg else GlacierOnSurface,
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                )
            }
        }
    }
}

@Composable
private fun DialogTitle(text: String) {
    Text(
        text,
        color = GlacierAmber,
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Black,
        fontSize = 16.sp,
        letterSpacing = 1.sp,
    )
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        color = GlacierMuted,
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 2.sp,
    )
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, allowSign: Boolean = false) {
    val keyboard = if (allowSign) KeyboardType.Text else KeyboardType.Number
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlacierSurface)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = { new ->
                val filtered = new.filter { c ->
                    c.isDigit() || (allowSign && (c == '-' || c == '+'))
                }
                onChange(filtered)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            textStyle = TextStyle(
                color = GlacierOnSurface,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both,
                ),
            ),
            cursorBrush = SolidColor(GlacierAmber),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
