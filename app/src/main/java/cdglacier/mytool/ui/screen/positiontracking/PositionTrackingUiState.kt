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

    val precisionLabel: String
        get() = when {
            !trackingEnabled -> "OFF"
            trackingMode == TrackingMode.MOVING -> "HIGH"
            else -> "BALANCED"
        }

    val precisionDetail: String
        get() = when {
            !trackingEnabled -> "記録は停止しています"
            trackingMode == TrackingMode.MOVING -> "GPS高精度 / 5秒間隔"
            else -> "省電力 / 30秒間隔"
        }
}
