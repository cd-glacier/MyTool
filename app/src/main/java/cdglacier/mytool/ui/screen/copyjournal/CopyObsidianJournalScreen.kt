package cdglacier.mytool.ui.screen.copyjournal

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val journalDirPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.onJournalDirPicked(uri)
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onSnackbarShown()
        }
    }

    CopyObsidianJournalContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onPickJournalDir = { journalDirPickerLauncher.launch(uiState.journalDirUri) },
        onFilenameFormatChange = viewModel::onFilenameFormatChange,
        onSourceDateChange = viewModel::onSourceDateChange,
        onTargetDateChange = viewModel::onTargetDateChange,
        onCopy = viewModel::onCopy,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CopyObsidianJournalContent(
    uiState: CopyObsidianJournalUiState,
    snackbarHostState: SnackbarHostState,
    onPickJournalDir: () -> Unit,
    onFilenameFormatChange: (String) -> Unit,
    onSourceDateChange: (LocalDate) -> Unit,
    onTargetDateChange: (LocalDate) -> Unit,
    onCopy: () -> Unit,
    onBack: () -> Unit,
) {
    var showSourceDatePicker by remember { mutableStateOf(false) }
    var showTargetDatePicker by remember { mutableStateOf(false) }

    val displayFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
    val helperText = remember(uiState.filenameFormat) {
        try {
            val fmt = DateTimeFormatter.ofPattern(uiState.filenameFormat)
            "${LocalDate.now().format(fmt)}.md"
        } catch (e: Exception) {
            "無効なフォーマット"
        }
    }

    val dirDisplayName = uiState.journalDirUri?.lastPathSegment
        ?.replace("primary:", "/storage/emulated/0/")
        ?: "未選択"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Journal コピー") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "設定",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Journal フォルダ") },
                    supportingContent = { Text(dirDisplayName) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(0.dp),
                    trailingContent = {
                        TextButton(onClick = onPickJournalDir) {
                            Text("選択")
                        }
                    }
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.filenameFormat,
                    onValueChange = onFilenameFormatChange,
                    label = { Text("ファイル名フォーマット") },
                    supportingText = { Text("例: $helperText") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "コピー操作",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("コピー元", style = MaterialTheme.typography.labelMedium)
                        Text(uiState.sourceDate.format(displayFormatter))
                    }
                    IconButton(onClick = { showSourceDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "コピー元の日付を選択")
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("コピー先", style = MaterialTheme.typography.labelMedium)
                        Text(uiState.targetDate.format(displayFormatter))
                    }
                    IconButton(onClick = { showTargetDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "コピー先の日付を選択")
                    }
                }
            }

            item {
                Button(
                    onClick = onCopy,
                    enabled = uiState.journalDirUri != null && !uiState.isCopying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    if (uiState.isCopying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("コピー")
                    }
                }
            }
        }
    }

    if (showSourceDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.sourceDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showSourceDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onSourceDateChange(Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate())
                    }
                    showSourceDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showSourceDatePicker = false }) { Text("キャンセル") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTargetDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.targetDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showTargetDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onTargetDateChange(Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate())
                    }
                    showTargetDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTargetDatePicker = false }) { Text("キャンセル") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
