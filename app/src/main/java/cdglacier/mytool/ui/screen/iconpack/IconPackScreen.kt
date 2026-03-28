package cdglacier.mytool.ui.screen.iconpack

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cdglacier.mytool.ui.theme.GruvboxBg
import cdglacier.mytool.ui.theme.GruvboxMuted
import cdglacier.mytool.ui.theme.GruvboxOnSurface
import cdglacier.mytool.ui.theme.GruvboxRed
import cdglacier.mytool.ui.theme.GruvboxSurfaceHigh
import cdglacier.mytool.ui.theme.GruvboxSurfaceLow
import cdglacier.mytool.ui.theme.GruvboxYellow
import cdglacier.mytool.ui.theme.SpaceGroteskFamily

@Composable
fun IconPackScreen(
    onBack: () -> Unit,
    viewModel: IconPackViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { IconPackTopBar(onBack = onBack) },
        containerColor = GruvboxBg,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            InfoBanner()
            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader(text = "PACK_CONTENTS")
            Spacer(modifier = Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(0.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.icons) { entry ->
                    IconPackItem(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun IconPackTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GruvboxBg)
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "戻る",
                tint = GruvboxOnSurface,
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "ICON_PACK",
            color = GruvboxRed,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            letterSpacing = (-0.5).sp,
        )
    }
}

@Composable
private fun InfoBanner() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GruvboxSurfaceLow)
            .padding(16.dp)
    ) {
        Text(
            text = "PACK_STATUS: ACTIVE",
            color = GruvboxYellow,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "対応ランチャー（Nova, Apex 等）の設定からこのアプリをアイコンパックとして選択することでアイコンを適用できます。",
            color = GruvboxMuted,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 20.sp,
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(1.dp)
                .background(GruvboxSurfaceHigh)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = GruvboxMuted,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 2.sp,
        )
    }
}

@Composable
private fun IconPackItem(entry: IconPackEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GruvboxSurfaceLow)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(GruvboxSurfaceHigh)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = entry.drawableRes),
                contentDescription = entry.appName,
                modifier = Modifier
                    .size(52.dp)
                    .padding(4.dp),
                colorFilter = ColorFilter.tint(GruvboxOnSurface),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = entry.appName,
            color = GruvboxOnSurface,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
        Text(
            text = entry.packageName,
            color = GruvboxMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
        )
    }
}
