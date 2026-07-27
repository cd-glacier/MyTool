package cdglacier.mytool.ui.screen.healthconnect

import android.net.Uri
import java.time.Duration
import java.time.LocalDate

data class HealthConnectUiState(
    val sourceDate: LocalDate = LocalDate.now().minusDays(1),
    val targetDate: LocalDate = LocalDate.now(),
    val steps: Long? = null,
    val sleep: Duration? = null,
    val isLoading: Boolean = false,
    val isWriting: Boolean = false,
    val healthConnectAvailable: Boolean = false,
    val permissionsGranted: Boolean = false,
    val journalDirUri: Uri? = null,
    val filenameFormat: String = "yyyy-MM-dd",
    val snackbarMessage: String? = null,
) {
    val hasAnyData: Boolean
        get() = sleep != null || (steps != null && steps > 0)

    val canWrite: Boolean
        get() = hasAnyData && journalDirUri != null && !isWriting
}
