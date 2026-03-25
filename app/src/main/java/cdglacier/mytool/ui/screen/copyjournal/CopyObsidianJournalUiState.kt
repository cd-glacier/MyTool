package cdglacier.mytool.ui.screen.copyjournal

import android.net.Uri
import java.time.LocalDate

data class CopyObsidianJournalUiState(
    val journalDirUri: Uri? = null,
    val filenameFormat: String = "yyyy-MM-dd",
    val sourceDate: LocalDate = LocalDate.now().minusDays(1),
    val targetDate: LocalDate = LocalDate.now(),
    val isCopying: Boolean = false,
    val snackbarMessage: String? = null,
)
