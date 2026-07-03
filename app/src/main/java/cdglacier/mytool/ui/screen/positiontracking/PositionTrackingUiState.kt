package cdglacier.mytool.ui.screen.positiontracking

import android.net.Uri
import cdglacier.mytool.data.repository.TrackingMode
import cdglacier.mytool.ui.component.LocationPointUiModel
import java.time.LocalDate

data class PositionTrackingUiState(
    val date: LocalDate = LocalDate.now(),
    val points: List<LocationPointUiModel> = emptyList(),
    val trackingEnabled: Boolean = false,
    val trackingMode: TrackingMode = TrackingMode.STATIONARY,
    val foregroundLocationGranted: Boolean = false,
    val backgroundLocationGranted: Boolean = false,
    val journalDirUri: Uri? = null,
    val filenameFormat: String = "yyyy-MM-dd",
    val isExporting: Boolean = false,
    val snackbarMessage: String? = null,
) {
    val permissionsReady: Boolean
        get() = foregroundLocationGranted && backgroundLocationGranted

    val canExport: Boolean
        get() = journalDirUri != null && !isExporting
}
