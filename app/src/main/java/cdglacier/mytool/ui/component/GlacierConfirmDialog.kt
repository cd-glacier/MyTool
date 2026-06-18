package cdglacier.mytool.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cdglacier.mytool.ui.theme.GlacierAmber
import cdglacier.mytool.ui.theme.GlacierBg
import cdglacier.mytool.ui.theme.GlacierMuted
import cdglacier.mytool.ui.theme.GlacierOnSurface
import cdglacier.mytool.ui.theme.GlacierSurface
import cdglacier.mytool.ui.theme.GlacierSurfaceLow
import cdglacier.mytool.ui.theme.SpaceGroteskFamily

@Composable
fun GlacierConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Dialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(GlacierSurfaceLow)
                .drawBehind {
                    drawRect(
                        color = GlacierAmber,
                        topLeft = Offset.Zero,
                        size = Size(width = 4.dp.toPx(), height = size.height),
                    )
                }
                .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
        ) {
            Text(
                text = title,
                color = GlacierAmber,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = body,
                color = GlacierOnSurface,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(GlacierSurface)
                        .clickable { onCancel() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = cancelLabel,
                        color = GlacierMuted,
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(GlacierAmber)
                        .clickable { onConfirm() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = confirmLabel,
                        color = GlacierBg,
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                    )
                }
            }
        }
    }
}
