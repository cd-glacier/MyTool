package cdglacier.mytool.ui.screen.qrstocker

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cdglacier.mytool.ui.component.GlacierSectionCard
import cdglacier.mytool.ui.component.GlacierTopBar
import cdglacier.mytool.ui.theme.GlacierAmber
import cdglacier.mytool.ui.theme.GlacierBg
import cdglacier.mytool.ui.theme.GlacierMuted
import cdglacier.mytool.ui.theme.GlacierOnSurface
import cdglacier.mytool.ui.theme.GlacierSurface
import cdglacier.mytool.ui.theme.SpaceGroteskFamily

@Composable
fun QrStockerRoute(
    onBack: () -> Unit,
    onEntryClick: (String) -> Unit,
    viewModel: QrStockerViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    QrStockerScreen(
        uiState = uiState,
        onBack = onBack,
        onEntryClick = onEntryClick,
        onImagePicked = viewModel::onImagePicked,
        onTitleInputChange = viewModel::onTitleInputChange,
        onCancelPending = viewModel::onCancelPending,
        onConfirmPending = viewModel::onConfirmPending,
    )
}

@Composable
fun QrStockerScreen(
    uiState: QrStockerUiState,
    onBack: () -> Unit,
    onEntryClick: (String) -> Unit,
    onImagePicked: (android.net.Uri) -> Unit,
    onTitleInputChange: (String) -> Unit,
    onCancelPending: () -> Unit,
    onConfirmPending: () -> Unit,
) {
    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) onImagePicked(uri) }

    Scaffold(
        topBar = { GlacierTopBar(title = "QR_STOCKER", onBack = onBack) },
        containerColor = GlacierBg,
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "add") {
                GlacierSectionCard(title = "ADD_QR") {
                    Text(
                        text = "[PICK_IMAGE]",
                        color = GlacierAmber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .background(GlacierSurface)
                            .clickable {
                                pickImageLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                    if (uiState.error != null && uiState.pendingDecoded == null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = uiState.error,
                            color = GlacierAmber,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                        )
                    }
                }
            }

            if (uiState.pendingDecoded != null) {
                item(key = "pending") {
                    PendingSection(
                        pending = uiState.pendingDecoded,
                        error = uiState.error,
                        onTitleChange = onTitleInputChange,
                        onCancel = onCancelPending,
                        onConfirm = onConfirmPending,
                    )
                }
            }

            when {
                uiState.isLoading && uiState.entries.isEmpty() -> {
                    item { EmptyMessage("LOADING...") }
                }
                uiState.entries.isEmpty() -> {
                    item { EmptyMessage("NO_QR_STOCKED") }
                }
                else -> {
                    items(items = uiState.entries, key = { it.title }) { entry ->
                        EntryRow(title = entry.title, onClick = { onEntryClick(entry.title) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingSection(
    pending: PendingDecodedUiModel,
    error: String?,
    onTitleChange: (String) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    GlacierSectionCard(title = "NEW_QR") {
        Text(
            text = "CONTENT: ${pending.content.take(120)}${if (pending.content.length > 120) "…" else ""}",
            color = GlacierOnSurface,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "EC: ${pending.ecLevel}   MODE: ${pending.mode}",
            color = GlacierMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        BasicTextField(
            value = pending.titleInput,
            onValueChange = onTitleChange,
            enabled = !pending.isSubmitting,
            singleLine = true,
            textStyle = TextStyle(
                color = GlacierOnSurface,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
            ),
            cursorBrush = SolidColor(GlacierAmber),
            modifier = Modifier
                .fillMaxWidth()
                .background(GlacierSurface)
                .padding(horizontal = 6.dp, vertical = 6.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Text(
                text = if (pending.isSubmitting) "[...]" else "[SAVE]",
                color = GlacierAmber,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier
                    .clickable(enabled = !pending.isSubmitting && pending.titleInput.isNotBlank()) { onConfirm() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "[CANCEL]",
                color = GlacierMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier
                    .clickable(enabled = !pending.isSubmitting) { onCancel() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        if (error != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = error,
                color = GlacierAmber,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun EntryRow(title: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlacierSurface)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                color = GlacierOnSurface,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = ">",
                color = GlacierAmber,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = text,
            color = GlacierMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
        )
    }
}
