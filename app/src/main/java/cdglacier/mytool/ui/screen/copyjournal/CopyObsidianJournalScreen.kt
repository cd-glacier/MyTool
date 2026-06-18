package cdglacier.mytool.ui.screen.copyjournal

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cdglacier.mytool.ui.component.GlacierButton
import cdglacier.mytool.ui.component.GlacierConfirmDialog
import cdglacier.mytool.ui.component.GlacierSectionCard
import cdglacier.mytool.ui.component.GlacierSwitch
import cdglacier.mytool.ui.component.GlacierTopBar
import cdglacier.mytool.ui.component.NoticeCard
import cdglacier.mytool.ui.theme.GlacierAmber
import cdglacier.mytool.ui.theme.GlacierBg
import cdglacier.mytool.ui.theme.GlacierMuted
import cdglacier.mytool.ui.theme.GlacierOnSurface
import cdglacier.mytool.ui.theme.GlacierSurface
import cdglacier.mytool.ui.theme.SpaceGroteskFamily
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CopyObsidianJournalScreen(
    viewModel: CopyObsidianJournalViewModel = viewModel(),
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onSnackbarShown()
        }
    }

    CopyObsidianJournalContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onSourceDateChange = viewModel::onSourceDateChange,
        onTargetDateChange = viewModel::onTargetDateChange,
        onCopy = viewModel::onCopy,
        onOverwriteConfirmed = viewModel::onOverwriteConfirmed,
        onOverwriteCancelled = viewModel::onOverwriteCancelled,
        onAutoCopyToggle = viewModel::onAutoCopyToggle,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CopyObsidianJournalContent(
    uiState: CopyObsidianJournalUiState,
    snackbarHostState: SnackbarHostState,
    onSourceDateChange: (LocalDate) -> Unit,
    onTargetDateChange: (LocalDate) -> Unit,
    onCopy: () -> Unit,
    onOverwriteConfirmed: () -> Unit,
    onOverwriteCancelled: () -> Unit,
    onAutoCopyToggle: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    var showSourceDatePicker by remember { mutableStateOf(false) }
    var showTargetDatePicker by remember { mutableStateOf(false) }

    val displayFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

    Scaffold(
        topBar = { GlacierTopBar(title = "COPY_JOURNAL", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = GlacierBg,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (uiState.journalDirUri == null) {
                NoticeCard(
                    title = "JOURNAL_DIR: NOT_SET",
                    body = "SYS_SETTINGS から Journal フォルダを設定してください。",
                )
            }

            GlacierSectionCard(title = "COPY_OPS") {
                DateRow(
                    label = "SOURCE_DATE",
                    date = uiState.sourceDate.format(displayFormatter),
                    onClick = { showSourceDatePicker = true },
                )
                Spacer(modifier = Modifier.height(4.dp))
                DateRow(
                    label = "TARGET_DATE",
                    date = uiState.targetDate.format(displayFormatter),
                    onClick = { showTargetDatePicker = true },
                )
                Spacer(modifier = Modifier.height(12.dp))
                GlacierButton(
                    label = "COPY",
                    loadingLabel = "COPYING...",
                    onClick = onCopy,
                    enabled = uiState.journalDirUri != null,
                    loading = uiState.isCopying,
                )
            }

            GlacierSectionCard(title = "AUTO_COPY") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "今日のJournalを自動作成",
                            color = GlacierOnSurface,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "1時間ごとに前日Journalから今日分をコピー",
                            color = GlacierMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                        )
                    }
                    Spacer(modifier = Modifier.padding(start = 8.dp))
                    GlacierSwitch(
                        checked = uiState.autoCopyEnabled,
                        onCheckedChange = onAutoCopyToggle,
                        enabled = uiState.journalDirUri != null,
                    )
                }
            }
        }
    }

    if (uiState.showOverwriteConfirmation) {
        GlacierConfirmDialog(
            title = "OVERWRITE_CONFIRM",
            body = "コピー先にすでに内容があります。上書きしますか？",
            confirmLabel = "OVERWRITE",
            cancelLabel = "CANCEL",
            onConfirm = onOverwriteConfirmed,
            onCancel = onOverwriteCancelled,
        )
    }

    if (showSourceDatePicker) {
        DateDialog(
            initialDate = uiState.sourceDate,
            onConfirm = {
                onSourceDateChange(it)
                showSourceDatePicker = false
            },
            onDismiss = { showSourceDatePicker = false },
        )
    }
    if (showTargetDatePicker) {
        DateDialog(
            initialDate = uiState.targetDate,
            onConfirm = {
                onTargetDateChange(it)
                showTargetDatePicker = false
            },
            onDismiss = { showTargetDatePicker = false },
        )
    }
}

@Composable
private fun DateRow(label: String, date: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlacierSurface)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = GlacierMuted,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = date,
                color = GlacierOnSurface,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
            )
        }
        Box {
            Text(
                text = "EDIT >",
                color = GlacierAmber,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(
    initialDate: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate())
                }
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
