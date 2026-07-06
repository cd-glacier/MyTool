package cdglacier.mytool.ui.screen.householdpoints

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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cdglacier.mytool.ui.theme.GlacierAmber
import cdglacier.mytool.ui.theme.GlacierBg
import cdglacier.mytool.ui.theme.GlacierMuted
import cdglacier.mytool.ui.theme.GlacierOnSurface
import cdglacier.mytool.ui.theme.GlacierSurface
import cdglacier.mytool.ui.theme.GlacierSurfaceLow
import cdglacier.mytool.ui.theme.GlacierTeal
import cdglacier.mytool.ui.theme.SpaceGroteskFamily

@Composable
fun HouseholdPointsRoute(
    onBack: () -> Unit,
    viewModel: HouseholdPointsViewModel = viewModel(),
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

    HouseholdPointsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onNewNameChange = viewModel::onNewNameChange,
        onNewValueChange = viewModel::onNewValueChange,
        onAdd = viewModel::onAddPoint,
        onRequestDelete = viewModel::onRequestDelete,
        onCancelDelete = viewModel::onCancelDelete,
        onConfirmDelete = viewModel::onConfirmDelete,
        onRequestEdit = viewModel::onRequestEdit,
        onEditValueChange = viewModel::onEditValueChange,
        onCancelEdit = viewModel::onCancelEdit,
        onConfirmEdit = viewModel::onConfirmEdit,
    )
}

@Composable
fun HouseholdPointsScreen(
    uiState: HouseholdPointsUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onNewNameChange: (String) -> Unit,
    onNewValueChange: (String) -> Unit,
    onAdd: () -> Unit,
    onRequestDelete: (String) -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onRequestEdit: (String) -> Unit,
    onEditValueChange: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onConfirmEdit: () -> Unit,
) {
    Scaffold(
        topBar = { TopBar(onBack) },
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
            if (!uiState.pagesConfigured) {
                NoticeCard(
                    "PAGES_DIR: NOT_SET",
                    "SYS_SETTINGS から pages フォルダを設定してください。",
                )
                return@Column
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GlacierSurfaceLow)
                    .padding(16.dp),
            ) {
                SectionLabel("NEW_TASK")
                Spacer(Modifier.height(8.dp))
                FieldLabel("NAME")
                TextInput(uiState.newName, onNewNameChange, placeholder = "家事名")
                Spacer(Modifier.height(12.dp))
                FieldLabel("POINTS")
                NumberInput(uiState.newPointsText, onNewValueChange)
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GlacierAmber)
                        .clickable { onAdd() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "+ ADD",
                        color = GlacierBg,
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            SectionLabel("REGISTERED_TASKS")
            Spacer(Modifier.height(8.dp))
            if (uiState.points.isEmpty()) {
                Text(
                    "-- 家事が未登録です --",
                    color = GlacierMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    uiState.points.forEach { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GlacierSurface)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                p.name,
                                modifier = Modifier.weight(1f),
                                color = GlacierOnSurface,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "${p.points}pt",
                                color = GlacierTeal,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .background(GlacierBg)
                                    .clickable { onRequestEdit(p.name) }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    "EDIT",
                                    color = GlacierTeal,
                                    fontFamily = SpaceGroteskFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(GlacierBg)
                                    .clickable { onRequestDelete(p.name) }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    "×",
                                    color = GlacierAmber,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    if (uiState.pendingDeleteName != null) {
        ConfirmDeleteDialog(
            name = uiState.pendingDeleteName,
            onConfirm = onConfirmDelete,
            onDismiss = onCancelDelete,
        )
    }
    if (uiState.editingName != null) {
        EditPointDialog(
            name = uiState.editingName,
            pointsText = uiState.editingPointsText,
            onValueChange = onEditValueChange,
            onConfirm = onConfirmEdit,
            onDismiss = onCancelEdit,
        )
    }
}

@Composable
private fun ConfirmDeleteDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(GlacierBg)
                .padding(20.dp)
        ) {
            Text(
                "DELETE_TASK",
                color = GlacierAmber,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "「$name」を削除しますか？",
                color = GlacierOnSurface,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DialogButton("CANCEL", GlacierSurface, GlacierOnSurface, Modifier.weight(1f), onDismiss)
                DialogButton("DELETE", GlacierAmber, GlacierBg, Modifier.weight(1f), onConfirm)
            }
        }
    }
}

@Composable
private fun EditPointDialog(
    name: String,
    pointsText: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(GlacierBg)
                .padding(20.dp)
        ) {
            Text(
                "EDIT_POINTS",
                color = GlacierTeal,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                name,
                color = GlacierOnSurface,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            FieldLabel("POINTS")
            NumberInput(pointsText, onValueChange)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DialogButton("CANCEL", GlacierSurface, GlacierOnSurface, Modifier.weight(1f), onDismiss)
                DialogButton("SAVE", GlacierAmber, GlacierBg, Modifier.weight(1f), onConfirm)
            }
        }
    }
}

@Composable
private fun DialogButton(
    label: String,
    bg: androidx.compose.ui.graphics.Color,
    fg: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = fg,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
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
            "HOUSEHOLD_POINTS",
            color = GlacierOnSurface,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = GlacierMuted,
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 2.sp,
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
private fun TextInput(value: String, onChange: (String) -> Unit, placeholder: String = "") {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlacierSurface)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                placeholder,
                color = GlacierMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.Both,
                    ),
                ),
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
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

@Composable
private fun NumberInput(value: String, onChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlacierSurface)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = { new -> onChange(new.filter { it.isDigit() }) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

@Composable
private fun NoticeCard(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlacierSurface)
            .padding(16.dp),
    ) {
        Text(title, color = GlacierAmber, fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(body, color = GlacierMuted, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    }
}
