package cdglacier.mytool.domain.model

import java.time.Instant
import java.time.LocalDate

enum class SleepStageType(val key: String, val label: String) {
    AWAKE("AWAKE", "AWAKE"),
    AWAKE_IN_BED("AWAKE_IN_BED", "AWAKE_IN_BED"),
    OUT_OF_BED("OUT_OF_BED", "OUT_OF_BED"),
    LIGHT("LIGHT", "LIGHT"),
    DEEP("DEEP", "DEEP"),
    REM("REM", "REM"),
    SLEEPING("SLEEPING", "SLEEPING"),
    UNKNOWN("UNKNOWN", "UNKNOWN");

    companion object {
        fun fromKey(key: String): SleepStageType =
            values().firstOrNull { it.key.equals(key, ignoreCase = true) } ?: UNKNOWN
    }
}

data class SleepStage(
    val type: SleepStageType,
    val start: Instant,
    val end: Instant,
)

data class SleepSession(
    val start: Instant,
    val end: Instant,
    val stages: List<SleepStage>,
)

data class SleepDay(
    val date: LocalDate,
    val sessions: List<SleepSession> = emptyList(),
) {
    fun hasAny(): Boolean = sessions.isNotEmpty()
}

data class SleepBook(
    val days: Map<LocalDate, SleepDay> = emptyMap(),
) {
    fun dayOrEmpty(date: LocalDate): SleepDay = days[date] ?: SleepDay(date)
    fun with(day: SleepDay): SleepBook = copy(days = days + (day.date to day))
}
