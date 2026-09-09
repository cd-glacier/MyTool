package cdglacier.mytool.ui.screen.qrstocker.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cdglacier.mytool.ui.component.GlacierTopBar
import cdglacier.mytool.ui.theme.GlacierAmber
import cdglacier.mytool.ui.theme.GlacierBg
import cdglacier.mytool.ui.theme.GlacierMuted
import cdglacier.mytool.ui.theme.GlacierOnSurface
import cdglacier.mytool.ui.theme.SpaceGroteskFamily
import java.time.format.DateTimeFormatter

@Composable
fun QrStockerDetailRoute(
    title: String,
    onBack: () -> Unit,
    viewModel: QrStockerDetailViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(title) { viewModel.load(title) }
    QrStockerDetailScreen(uiState = uiState, onBack = onBack)
}

@Composable
fun QrStockerDetailScreen(
    uiState: QrStockerDetailUiState,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = { GlacierTopBar(title = uiState.entry?.title ?: "QR_DETAIL", onBack = onBack) },
        containerColor = GlacierBg,
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(horizontal = 16.dp, vertical = 16.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                uiState.isLoading -> {
                    Text("LOADING...", color = GlacierMuted, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                }
                uiState.error != null -> {
                    Text(uiState.error, color = GlacierAmber, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                }
                uiState.entry != null && uiState.bitmap != null -> {
                    Image(
                        bitmap = uiState.bitmap.asImageBitmap(),
                        contentDescription = uiState.entry.title,
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .aspectRatio(1f)
                            .background(Color.White)
                            .padding(8.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    MetaRow(label = "TITLE", value = uiState.entry.title)
                    MetaRow(label = "EC_LEVEL", value = uiState.entry.ecLevel.name)
                    MetaRow(label = "MODE", value = uiState.entry.mode)
                    uiState.entry.version?.let { MetaRow(label = "VERSION", value = it.toString()) }
                    MetaRow(
                        label = "CREATED_AT",
                        value = uiState.entry.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "CONTENT",
                        color = GlacierMuted,
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 2.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = uiState.entry.content,
                        color = GlacierOnSurface,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "$label: $value",
            color = GlacierOnSurface,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
        )
    }
}
