package cdglacier.mytool.ui.screen.iconpack

import androidx.annotation.DrawableRes

data class IconPackEntry(
    val appName: String,
    val packageName: String,
    @DrawableRes val drawableRes: Int,
)

data class IconPackUiState(
    val icons: List<IconPackEntry> = emptyList(),
)
