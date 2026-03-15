package cdglacier.mytool.screen

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopyObsidianJournalScreen(
    journalDirUri: Uri?,
    filenameFormat: String,
    onPickJournalDir: () -> Unit,
    onFilenameFormatChange: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var sourceDate by remember { mutableStateOf(LocalDate.now().minusDays(1)) }
    var targetDate by remember { mutableStateOf(LocalDate.now()) }

    var showSourceDatePicker by remember { mutableStateOf(false) }
    var showTargetDatePicker by remember { mutableStateOf(false) }
    var isCopying by remember { mutableStateOf(false) }

    val displayFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
    val helperText = remember(filenameFormat) {
        try {
            val fmt = DateTimeFormatter.ofPattern(filenameFormat)
            "${LocalDate.now().format(fmt)}.md"
        } catch (e: Exception) {
            "無効なフォーマット"
        }
    }

    val dirDisplayName = journalDirUri?.lastPathSegment
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
                    modifier = Modifier.fillMaxWidth()
                        .padding(0.dp)
                        .run {
                            this
                        },
                    trailingContent = {
                        TextButton(onClick = onPickJournalDir) {
                            Text("選択")
                        }
                    }
                )
            }

            item {
                OutlinedTextField(
                    value = filenameFormat,
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
                        Text(sourceDate.format(displayFormatter))
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
                        Text(targetDate.format(displayFormatter))
                    }
                    IconButton(onClick = { showTargetDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "コピー先の日付を選択")
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        val dirUri = journalDirUri ?: return@Button
                        scope.launch {
                            isCopying = true
                            val startTime = System.currentTimeMillis()
                            val result = copyJournalFile(
                                context = context,
                                journalDirUri = dirUri,
                                sourceDate = sourceDate,
                                targetDate = targetDate,
                                filenameFormat = filenameFormat
                            )
                            val elapsed = System.currentTimeMillis() - startTime
                            if (elapsed < 2000) delay(2000- elapsed)
                            isCopying = false
                            result.fold(
                                onSuccess = { snackbarHostState.showSnackbar("コピーしました") },
                                onFailure = { e -> snackbarHostState.showSnackbar("エラー: ${e.message}") }
                            )
                        }
                    },
                    enabled = journalDirUri != null && !isCopying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    if (isCopying) {
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
            initialSelectedDateMillis = sourceDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showSourceDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        sourceDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
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
            initialSelectedDateMillis = targetDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showTargetDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        targetDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
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

private suspend fun copyJournalFile(
    context: Context,
    journalDirUri: Uri,
    sourceDate: LocalDate,
    targetDate: LocalDate,
    filenameFormat: String
): Result<Unit> = runCatching {
    val formatter = DateTimeFormatter.ofPattern(filenameFormat)
    val srcName = "${sourceDate.format(formatter)}.md"
    val dstName = "${targetDate.format(formatter)}.md"

    val dir = DocumentFile.fromTreeUri(context, journalDirUri)
        ?: error("フォルダを開けません")

    val srcFile = dir.findFile(srcName)
        ?: error("コピー元ファイルが見つかりません: $srcName")

    val srcBytes = context.contentResolver.openInputStream(srcFile.uri)
        ?.use { it.readBytes() }
        ?: error("コピー元ファイルを読み込めません")

    val dstFile = dir.findFile(dstName)
        ?: dir.createFile("text/markdown", dstName)
        ?: error("コピー先ファイルを作成できません")

    context.contentResolver.openOutputStream(dstFile.uri, "wt")
        ?.use { it.write(srcBytes) }
        ?: error("コピー先ファイルに書き込めません")
}
