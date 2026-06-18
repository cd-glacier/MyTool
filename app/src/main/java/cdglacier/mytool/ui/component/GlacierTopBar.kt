package cdglacier.mytool.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cdglacier.mytool.ui.theme.GlacierAmber
import cdglacier.mytool.ui.theme.GlacierBg
import cdglacier.mytool.ui.theme.GlacierOnSurface
import cdglacier.mytool.ui.theme.GlacierSurface
import cdglacier.mytool.ui.theme.SpaceGroteskFamily

@Composable
fun GlacierTopBar(title: String, onBack: () -> Unit) {
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
                text = "<",
                color = GlacierAmber,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = GlacierOnSurface,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            letterSpacing = 1.sp,
        )
    }
}
