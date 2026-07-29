package cdglacier.mytool.ui.screen.healthconnect

import cdglacier.mytool.domain.model.DailyHealth
import java.time.LocalDate

enum class HealthViewMode { DAY, WEEK }

data class HealthConnectUiState(
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val healthConnectAvailable: Boolean = false,
    val permissionsGranted: Boolean = false,
    val isPagesDirConfigured: Boolean = false,
    val viewMode: HealthViewMode = HealthViewMode.DAY,
    val anchorDate: LocalDate = LocalDate.now().minusDays(1),
    val currentDay: DailyHealth = DailyHealth(LocalDate.now().minusDays(1)),
    val currentWeek: List<DailyHealth> = emptyList(),
    val weekLabel: String = "",
    val snackbarMessage: String? = null,
)
