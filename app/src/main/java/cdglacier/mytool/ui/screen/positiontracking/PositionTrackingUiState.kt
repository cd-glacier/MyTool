package cdglacier.mytool.ui.screen.positiontracking

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
) {
    val permissionsReady: Boolean
        get() = foregroundLocationGranted && backgroundLocationGranted
}
