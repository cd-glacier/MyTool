package cdglacier.mytool.ui.screen.positiontracking

import cdglacier.mytool.data.db.LocationRecordEntity
import java.time.LocalDate

data class PositionTrackingUiState(
    val date: LocalDate = LocalDate.now(),
    val records: List<LocationRecordEntity> = emptyList(),
    val trackingEnabled: Boolean = false,
    val foregroundLocationGranted: Boolean = false,
    val backgroundLocationGranted: Boolean = false,
) {
    val permissionsReady: Boolean
        get() = foregroundLocationGranted && backgroundLocationGranted
}
