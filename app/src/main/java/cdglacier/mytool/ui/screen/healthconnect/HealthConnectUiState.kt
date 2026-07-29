package cdglacier.mytool.ui.screen.healthconnect

import cdglacier.mytool.domain.model.DailyHealth
import cdglacier.mytool.domain.model.HealthBook
import java.time.LocalDate

enum class HealthViewMode { DAY, WEEK }

data class HealthConnectUiState(
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val healthConnectAvailable: Boolean = false,
    val permissionsGranted: Boolean = false,
    val isPagesDirConfigured: Boolean = false,
    val book: HealthBook = HealthBook(),
    val viewMode: HealthViewMode = HealthViewMode.DAY,
    val anchorDate: LocalDate = LocalDate.now().minusDays(1),
    val snackbarMessage: String? = null,
) {
    val currentDay: DailyHealth get() = book.dayOrEmpty(anchorDate)

    val currentWeek: List<DailyHealth>
        get() = weekDates(anchorDate).map { book.dayOrEmpty(it) }

    val weekLabel: String
        get() {
            val dates = weekDates(anchorDate)
            return "${dates.first()} ~ ${dates.last()}"
        }

    companion object {
        fun weekDates(anchor: LocalDate): List<LocalDate> {
            val mondayOffset = ((anchor.dayOfWeek.value + 6) % 7).toLong()
            val monday = anchor.minusDays(mondayOffset)
            return (0L..6L).map { monday.plusDays(it) }
        }
    }
}
