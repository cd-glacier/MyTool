package cdglacier.mytool.ui.screen.home

import android.net.Uri
import java.time.LocalDate

data class HomeUiState(
    val journalDirUri: Uri? = null,
    val habitCompletionRates: Map<LocalDate, Float?> = emptyMap(),
    val todayCompletionRate: Float? = null,
    val isLoading: Boolean = false,
)
