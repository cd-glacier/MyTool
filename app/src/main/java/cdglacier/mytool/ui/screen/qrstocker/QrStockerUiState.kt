package cdglacier.mytool.ui.screen.qrstocker

import cdglacier.mytool.domain.model.QrEntry

data class QrStockerUiState(
    val isLoading: Boolean = false,
    val entries: List<QrEntry> = emptyList(),
    val pendingDecoded: PendingDecodedUiModel? = null,
    val error: String? = null,
)

data class PendingDecodedUiModel(
    val content: String,
    val ecLevel: String,
    val mode: String,
    val titleInput: String = "",
    val isSubmitting: Boolean = false,
)
