package cdglacier.mytool.ui.screen.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val vaultFolderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.onVaultUriPicked(uri)
        }
    }

    val journalFolderPickerLauncher = rememberLauncherForActivityResult(
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

    SettingsContent(
        uiState = uiState,
        onPickVaultFolder = { vaultFolderPickerLauncher.launch(uiState.vaultUri) },
        onPickJournalFolder = { journalFolderPickerLauncher.launch(uiState.journalDirUri) },
        onFilenameFormatChange = viewModel::onFilenameFormatChange,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onPickVaultFolder: () -> Unit,
    onPickJournalFolder: () -> Unit,
    onFilenameFormatChange: (String) -> Unit,
    onBack: () -> Unit,
) {
    val filenameHelperText = remember(uiState.filenameFormat) {
        try {
            val fmt = DateTimeFormatter.ofPattern(uiState.filenameFormat)
            "${LocalDate.now().format(fmt)}.md"
        } catch (e: Exception) {
            "無効なフォーマット"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                ListItem(
                    headlineContent = { Text("Obsidian フォルダ") },
                    supportingContent = { Text(uiState.vaultUri.toDisplayString()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPickVaultFolder() }
                )
                HorizontalDivider()
            }
            item {
                ListItem(
                    headlineContent = { Text("Journal フォルダ") },
                    supportingContent = { Text(uiState.journalDirUri.toDisplayString()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPickJournalFolder() }
                )
                HorizontalDivider()
            }
            item {
                OutlinedTextField(
                    value = uiState.filenameFormat,
                    onValueChange = onFilenameFormatChange,
                    label = { Text("ファイル名フォーマット") },
                    supportingText = { Text("例: $filenameHelperText") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

private fun Uri?.toDisplayString(): String {
    if (this == null) return "未設定"
    val path = lastPathSegment
    return path?.replace("primary:", "/storage/emulated/0/") ?: toString()
}
