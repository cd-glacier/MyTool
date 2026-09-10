package cdglacier.mytool.ui.screen.qrstocker.detail

import android.graphics.Bitmap
import cdglacier.mytool.domain.model.QrEntry

data class QrStockerDetailUiState(
    val isLoading: Boolean = true,
    val entry: QrEntry? = null,
    val bitmap: Bitmap? = null,
    val error: String? = null,
)
