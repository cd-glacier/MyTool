package cdglacier.mytool.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cdglacier.mytool.ui.theme.GlacierAmber
import cdglacier.mytool.ui.theme.GlacierMuted
import cdglacier.mytool.ui.theme.GlacierSurface

@Composable
fun NoticeCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(GlacierSurface)
            .padding(16.dp)
    ) {
        Text(
            text = title,
            color = GlacierAmber,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = body,
            color = GlacierMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
        )
    }
}
