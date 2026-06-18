package cdglacier.mytool.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cdglacier.mytool.ui.theme.GlacierAmber
import cdglacier.mytool.ui.theme.GlacierMuted
import cdglacier.mytool.ui.theme.GlacierSurfaceLow
import cdglacier.mytool.ui.theme.SpaceGroteskFamily

@Composable
fun GlacierSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(GlacierSurfaceLow)
            .drawBehind {
                drawRect(
                    color = GlacierAmber,
                    topLeft = Offset.Zero,
                    size = Size(width = 4.dp.toPx(), height = size.height),
                )
            }
            .padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)
    ) {
        Text(
            text = title,
            color = GlacierMuted,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 2.sp,
        )
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}
