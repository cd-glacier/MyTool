package cdglacier.mytool.ui.screen.recipe

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cdglacier.mytool.ui.component.GlacierTopBar
import cdglacier.mytool.ui.component.RecipeItem
import cdglacier.mytool.ui.theme.GlacierAmber
import cdglacier.mytool.ui.theme.GlacierBg
import cdglacier.mytool.ui.theme.GlacierMuted
import cdglacier.mytool.ui.theme.GlacierSurfaceLow
import cdglacier.mytool.ui.theme.SpaceGroteskFamily
import java.time.format.DateTimeFormatter

@Composable
fun RecipeRoute(
    onBack: () -> Unit,
    viewModel: RecipeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    RecipeScreen(
        uiState = uiState,
        onRecipeClick = { url ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        },
        onBack = onBack,
    )
}

@Composable
fun RecipeScreen(
    uiState: RecipeUiState,
    onRecipeClick: (String) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = { GlacierTopBar(title = "RECIPES", onBack = onBack) },
        containerColor = GlacierBg,
    ) { innerPadding ->
        when {
            uiState.isLoading && uiState.sections.isEmpty() -> {
                EmptyMessage(text = "LOADING...", modifier = Modifier.padding(innerPadding))
            }
            uiState.sections.isEmpty() -> {
                EmptyMessage(text = "NO_RECIPES_FOUND", modifier = Modifier.padding(innerPadding))
            }
            else -> {
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
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
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
