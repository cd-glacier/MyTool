package cdglacier.mytool.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cdglacier.mytool.ui.theme.GlacierAmber
import cdglacier.mytool.ui.theme.GlacierBg
import cdglacier.mytool.ui.theme.GlacierMuted
import cdglacier.mytool.ui.theme.GlacierSurface
import cdglacier.mytool.ui.theme.SpaceGroteskFamily

@Composable
fun GlacierButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    loadingLabel: String = "...",
) {
    val isInteractable = enabled && !loading
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isInteractable) GlacierAmber else GlacierSurface)
            .clickable(enabled = isInteractable) { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (loading) loadingLabel else label,
            color = if (isInteractable) GlacierBg else GlacierMuted,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            letterSpacing = 1.sp,
        )
    }
}
