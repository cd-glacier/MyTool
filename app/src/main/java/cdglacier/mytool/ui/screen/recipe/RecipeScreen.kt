package cdglacier.mytool.ui.screen.recipe

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cdglacier.mytool.data.ai.GeminiNanoAvailability
import cdglacier.mytool.ui.component.GlacierSectionCard
import cdglacier.mytool.ui.component.GlacierTopBar
import cdglacier.mytool.ui.component.RecipeItem
import cdglacier.mytool.ui.theme.GlacierAmber
import cdglacier.mytool.ui.theme.GlacierBg
import cdglacier.mytool.ui.theme.GlacierMuted
import cdglacier.mytool.ui.theme.GlacierOnSurface
import cdglacier.mytool.ui.theme.GlacierSurface
import cdglacier.mytool.ui.theme.GlacierSurfaceLow
import cdglacier.mytool.ui.theme.SpaceGroteskFamily
import java.time.format.DateTimeFormatter

@Composable
fun RecipeRoute(
    onBack: () -> Unit,
    prefilledUrl: String? = null,
    viewModel: RecipeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        viewModel.refreshAiAvailability()
        onPauseOrDispose { }
    }
    LaunchedEffect(prefilledUrl) {
        if (!prefilledUrl.isNullOrBlank()) viewModel.setPrefilledUrl(prefilledUrl)
    }
    RecipeScreen(
        uiState = uiState,
        onRecipeClick = { url ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        },
        onAddUrlChange = viewModel::onAddUrlChange,
        onAddTitleChange = viewModel::onAddTitleChange,
        onFetchTitle = viewModel::fetchAddTitle,
        onSubmitAdd = viewModel::submitAdd,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onBack = onBack,
    )
}

@Composable
fun RecipeScreen(
    uiState: RecipeUiState,
    onRecipeClick: (String) -> Unit,
    onAddUrlChange: (String) -> Unit,
    onAddTitleChange: (String) -> Unit,
    onFetchTitle: () -> Unit,
    onSubmitAdd: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = { GlacierTopBar(title = "RECIPES", onBack = onBack) },
        containerColor = GlacierBg,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp,
                vertical = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "add-form") {
                AddRecipeSection(
                    form = uiState.addForm,
                    onUrlChange = onAddUrlChange,
                    onTitleChange = onAddTitleChange,
                    onFetchTitle = onFetchTitle,
                    onSubmit = onSubmitAdd,
                )
            }
            item(key = "search") {
                SearchSection(
                    query = uiState.searchQuery,
                    isSearching = uiState.isSearching,
                    availability = uiState.aiAvailability,
                    onQueryChange = onSearchQueryChange,
                )
            }
            val isSearchMode = uiState.searchQuery.isNotBlank()
            when {
                isSearchMode -> {
                    when {
                        uiState.isSearching && uiState.searchResults.isEmpty() -> {
                            item { EmptyMessage(text = "SEARCHING...") }
                        }
                        uiState.searchResults.isEmpty() -> {
                            item { EmptyMessage(text = "NO_MATCH") }
                        }
                        else -> {
                            items(items = uiState.searchResults, key = { "search-${it.url}" }) { item ->
                                RecipeItem(uiModel = item, onClick = onRecipeClick)
                            }
                        }
                    }
                }
                uiState.isLoading && uiState.sections.isEmpty() -> {
                    item { EmptyMessage(text = "LOADING...") }
                }
                uiState.sections.isEmpty() -> {
                    item { EmptyMessage(text = "NO_RECIPES_FOUND") }
                }
                else -> {
                    uiState.sections.forEach { section ->
                        item(key = "date-${section.date}") {
                            DateSectionHeader(label = formatDate(section.date))
                        }
                        items(items = section.items, key = { "${section.date}-${it.url}" }) { item ->
                            RecipeItem(uiModel = item, onClick = onRecipeClick)
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
private fun AddRecipeSection(
    form: AddRecipeFormUiModel,
    onUrlChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onFetchTitle: () -> Unit,
    onSubmit: () -> Unit,
) {
    GlacierSectionCard(title = "ADD_RECIPE") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextInput(
                value = form.url,
                onChange = onUrlChange,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (form.isFetching) "[...]" else "[FETCH]",
                color = GlacierAmber,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier.clickable(enabled = !form.isFetching && form.url.isNotBlank()) {
                    onFetchTitle()
                },
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextInput(
                value = form.title.orEmpty(),
                onChange = onTitleChange,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (form.isSubmitting) "[...]" else "[+ADD]",
                color = GlacierAmber,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier.clickable(
                    enabled = !form.isSubmitting && form.url.isNotBlank() && !form.title.isNullOrBlank(),
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
private fun SearchSection(
    query: String,
    isSearching: Boolean,
    availability: GeminiNanoAvailability,
    onQueryChange: (String) -> Unit,
) {
    val isEnabled = availability == GeminiNanoAvailability.AVAILABLE
    val statusLabel = when (availability) {
        GeminiNanoAvailability.AVAILABLE -> if (isSearching) "[...]" else "[AI:READY]"
        GeminiNanoAvailability.DOWNLOADING -> "[AI:DL...]"
        GeminiNanoAvailability.DOWNLOADABLE -> "[NO_MODEL]"
        GeminiNanoAvailability.UNAVAILABLE -> "[AI:N/A]"
        GeminiNanoAvailability.UNKNOWN -> "[AI:?]"
    }
    GlacierSectionCard(title = "SEARCH") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextInput(
                value = query,
                onChange = onQueryChange,
                enabled = isEnabled,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusLabel,
                color = if (isEnabled) GlacierMuted else GlacierAmber,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun TextInput(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        singleLine = true,
        textStyle = TextStyle(
            color = if (enabled) GlacierOnSurface else GlacierMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
        ),
        cursorBrush = SolidColor(GlacierAmber),
        modifier = modifier
            .background(GlacierSurface)
            .padding(horizontal = 6.dp, vertical = 4.dp),
    )
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
private fun formatDate(date: java.time.LocalDate): String = date.format(DATE_FORMATTER)
