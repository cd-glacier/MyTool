package cdglacier.mytool.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GlacierColorScheme = darkColorScheme(
    primary = GlacierCyan,
    onPrimary = GlacierOnPrimary,
    primaryContainer = GlacierCyan,
    onPrimaryContainer = GlacierOnPrimary,
    secondary = GlacierIce,
    onSecondary = GlacierOnSecondary,
    secondaryContainer = GlacierIce,
    onSecondaryContainer = GlacierOnSecondary,
    tertiary = GlacierTeal,
    onTertiary = GlacierBg,
    background = GlacierBg,
    onBackground = GlacierOnSurface,
    surface = GlacierBg,
    onSurface = GlacierOnSurface,
    surfaceVariant = GlacierSurface,
    onSurfaceVariant = GlacierTeal,
    outline = GlacierOutline,
    outlineVariant = GlacierSurfaceHighest,
)

@Composable
fun MyToolTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GlacierColorScheme,
        typography = Typography,
        content = content
    )
}
