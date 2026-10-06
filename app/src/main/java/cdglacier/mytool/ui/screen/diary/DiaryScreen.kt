package cdglacier.mytool.ui.screen.diary

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cdglacier.mytool.ui.component.DiaryItem
import cdglacier.mytool.ui.component.GlacierSectionCard
import cdglacier.mytool.ui.component.GlacierTopBar
import cdglacier.mytool.ui.theme.GlacierAmber
import cdglacier.mytool.ui.theme.GlacierBg
import cdglacier.mytool.ui.theme.GlacierMuted
import cdglacier.mytool.ui.theme.GlacierOnSurface
import cdglacier.mytool.ui.theme.GlacierSurface
import cdglacier.mytool.ui.theme.GlacierSurfaceLow
import cdglacier.mytool.ui.theme.SpaceGroteskFamily
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DiaryRoute(
    onBack: () -> Unit,
    viewModel: DiaryViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    DiaryScreen(
        uiState = uiState,
        onAddContentChange = viewModel::onAddContentChange,
        onSubmitAdd = viewModel::submitAdd,
        onBack = onBack,
    )
}

@Composable
fun DiaryScreen(
    uiState: DiaryUiState,
    onAddContentChange: (String) -> Unit,
    onSubmitAdd: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = { GlacierTopBar(title = "DIARY", onBack = onBack) },
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
            item(key = "add-form") {
                AddDiarySection(
                    form = uiState.addForm,
                    onContentChange = onAddContentChange,
                    onSubmit = onSubmitAdd,
                )
            }
            when {
                uiState.isLoading && uiState.sections.isEmpty() -> {
                    item { EmptyMessage(text = "LOADING...") }
                }
                uiState.sections.isEmpty() -> {
                    item { EmptyMessage(text = "NO_DIARIES_FOUND") }
                }
                else -> {
                    uiState.sections.forEach { section ->
                        item(key = "date-${section.date}") {
                            DateSectionHeader(label = formatDate(section.date))
                        }
                        items(items = section.items, key = { "${section.date}-${it.timestamp}-${it.content.hashCode()}" }) { item ->
                            DiaryItem(uiModel = item)
                        }
                        item(key = "spacer-${section.date}") {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddDiarySection(
    form: AddDiaryFormUiModel,
    onContentChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    GlacierSectionCard(title = "ADD_DIARY") {
        BasicTextField(
            value = form.content,
            onValueChange = onContentChange,
            textStyle = TextStyle(
                color = GlacierOnSurface,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
            ),
            cursorBrush = SolidColor(GlacierAmber),
            modifier = Modifier
                .fillMaxWidth()
                .background(GlacierSurface)
                .heightIn(min = 96.dp)
                .padding(horizontal = 6.dp, vertical = 4.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (form.isSubmitting) "[...]" else "[+ADD]",
                color = GlacierAmber,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier.clickable(
                    enabled = !form.isSubmitting && form.content.isNotBlank(),
                ) { onSubmit() },
            )
        }
        if (form.error != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = form.error,
                color = GlacierAmber,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun DateSectionHeader(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlacierSurfaceLow)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            color = GlacierAmber,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 2.sp,
        )
    }
}

@Composable
private fun EmptyMessage(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
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

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd (EEE)")
private fun formatDate(date: LocalDate): String = date.format(DATE_FORMATTER)
