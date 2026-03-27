package cdglacier.mytool.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GruvboxColorScheme = darkColorScheme(
    primary = GruvboxRed,
    onPrimary = GruvboxOnPrimary,
    primaryContainer = GruvboxRed,
    onPrimaryContainer = GruvboxOnPrimary,
    secondary = GruvboxYellow,
    onSecondary = GruvboxOnSecondary,
    secondaryContainer = GruvboxYellow,
    onSecondaryContainer = GruvboxOnSecondary,
    tertiary = GruvboxGreen,
    onTertiary = GruvboxBg,
    background = GruvboxBg,
    onBackground = GruvboxOnSurface,
    surface = GruvboxBg,
    onSurface = GruvboxOnSurface,
    surfaceVariant = GruvboxSurface,
    onSurfaceVariant = GruvboxGreen,
    outline = GruvboxOutline,
    outlineVariant = GruvboxSurfaceHighest,
)

@Composable
fun MyToolTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GruvboxColorScheme,
        typography = Typography,
        content = content
    )
}
