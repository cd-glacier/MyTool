package cdglacier.mytool.ui.screen.healthconnect.sleepstage

import cdglacier.mytool.domain.model.SleepStageType
import java.time.Instant
import java.time.LocalDate

data class SleepStageBand(
    val type: SleepStageType,
    val start: Instant,
    val end: Instant,
)

data class SleepStageChartUiState(
    val date: LocalDate = LocalDate.now(),
    val bands: List<SleepStageBand> = emptyList(),
    val windowStart: Instant? = null,
    val windowEnd: Instant? = null,
    val stageTotals: Map<SleepStageType, Long> = emptyMap(),
    val totalMinutes: Long = 0,
) {
    val hasData: Boolean get() = bands.isNotEmpty() && windowStart != null && windowEnd != null
}
