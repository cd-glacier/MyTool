package cdglacier.mytool.ui.screen.home

import android.net.Uri
import java.time.LocalDate

data class HomeUiState(
    val journalDirUri: Uri? = null,
    val journalLineCounts: Map<LocalDate, Int> = emptyMap(),
    val isLoading: Boolean = false,
)
