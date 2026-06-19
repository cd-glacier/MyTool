package cdglacier.mytool.ui.screen.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cdglacier.mytool.ui.component.GlacierSectionCard
import cdglacier.mytool.ui.component.GlacierTopBar
import cdglacier.mytool.ui.theme.GlacierAmber
import cdglacier.mytool.ui.theme.GlacierBg
import cdglacier.mytool.ui.theme.GlacierMuted
import cdglacier.mytool.ui.theme.GlacierOnSurface
import cdglacier.mytool.ui.theme.GlacierSurface
import cdglacier.mytool.ui.theme.GlacierTeal
import cdglacier.mytool.ui.theme.SpaceGroteskFamily
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

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onCalendarPermissionResult(granted)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onLocationPermissionResult(granted)
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onBackgroundLocationPermissionResult(granted)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SettingsContent(
        uiState = uiState,
        onPickVaultFolder = { vaultFolderPickerLauncher.launch(uiState.vaultUri) },
        onPickJournalFolder = { journalFolderPickerLauncher.launch(uiState.journalDirUri) },
        onFilenameFormatChange = viewModel::onFilenameFormatChange,
        onRequestCalendarPermission = {
            calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        },
        onRequestLocationPermission = {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        },
        onRequestBackgroundLocationPermission = {
            if (uiState.fineLocationGranted) {
                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                // FINE_LOCATIONが先に必要 → アプリ設定画面に飛ばす
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        },
        onBack = onBack,
    )
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onPickVaultFolder: () -> Unit,
    onPickJournalFolder: () -> Unit,
    onFilenameFormatChange: (String) -> Unit,
    onRequestCalendarPermission: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onRequestBackgroundLocationPermission: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = { GlacierTopBar(title = "SYS_SETTINGS", onBack = onBack) },
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
            GlacierSectionCard(title = "OBSIDIAN_DIRS") {
                SettingRow(
                    label = "VAULT_DIR",
                    value = uiState.vaultUri.toDisplayString(),
                    onClick = onPickVaultFolder,
                )
                Spacer(modifier = Modifier.height(4.dp))
                SettingRow(
                    label = "JOURNAL_DIR",
                    value = uiState.journalDirUri.toDisplayString(),
                    onClick = onPickJournalFolder,
                )
            }

            GlacierSectionCard(title = "JOURNAL_FMT") {
                FilenameFormatRow(
                    value = uiState.filenameFormat,
                    onValueChange = onFilenameFormatChange,
                )
            }

            GlacierSectionCard(title = "PERMISSIONS") {
                val cal = uiState.calendarPermissionGranted
                SettingRow(
                    label = "CALENDAR_ACCESS",
                    value = if (cal) "[GRANTED]" else "[DENIED]",
                    valueColor = if (cal) GlacierTeal else GlacierAmber,
                    onClick = if (cal) null else onRequestCalendarPermission,
                )
                Spacer(modifier = Modifier.height(4.dp))
                val fine = uiState.fineLocationGranted
                SettingRow(
                    label = "LOCATION_ACCESS",
                    value = if (fine) "[GRANTED]" else "[DENIED]",
                    valueColor = if (fine) GlacierTeal else GlacierAmber,
                    onClick = if (fine) null else onRequestLocationPermission,
                )
                Spacer(modifier = Modifier.height(4.dp))
                val bg = uiState.backgroundLocationGranted
                SettingRow(
                    label = "BACKGROUND_LOCATION",
                    value = if (bg) "[GRANTED]" else "[DENIED]",
                    valueColor = if (bg) GlacierTeal else GlacierAmber,
                    onClick = if (bg) null else onRequestBackgroundLocationPermission,
                )
            }
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    value: String,
    onClick: (() -> Unit)?,
    valueColor: Color = GlacierOnSurface,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlacierSurface)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
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
                text = value,
                color = valueColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
            )
        }
        if (onClick != null) {
            Text(
                text = ">",
                color = GlacierAmber,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun FilenameFormatRow(
    value: String,
    onValueChange: (String) -> Unit,
) {
    val example = remember(value) {
        try {
            "${LocalDate.now().format(DateTimeFormatter.ofPattern(value))}.md"
        } catch (e: Exception) {
            "INVALID_FORMAT"
        }
    }
    val isValid = example != "INVALID_FORMAT"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlacierSurface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = "FILENAME_FMT",
            color = GlacierMuted,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
        )
        Spacer(modifier = Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = GlacierOnSurface,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
            ),
            cursorBrush = SolidColor(GlacierAmber),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isValid) "EXAMPLE: $example" else "EXAMPLE: INVALID_FORMAT",
            color = if (isValid) GlacierMuted else GlacierAmber,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
        )
    }
}

private fun Uri?.toDisplayString(): String {
    if (this == null) return "[NOT_SET]"
    val path = lastPathSegment
    return path?.replace("primary:", "/storage/emulated/0/") ?: toString()
}
