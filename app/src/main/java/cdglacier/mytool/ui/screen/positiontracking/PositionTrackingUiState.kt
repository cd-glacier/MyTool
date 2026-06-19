package cdglacier.mytool.ui.screen.positiontracking

import android.net.Uri
import java.time.LocalDate

data class LocationPointUiModel(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val batteryLevel: Int,
    val sameLocationCount: Int,
)

data class PositionTrackingUiState(
    val date: LocalDate = LocalDate.now(),
    val points: List<LocationPointUiModel> = emptyList(),
    val trackingEnabled: Boolean = false,
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
