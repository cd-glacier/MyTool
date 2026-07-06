package cdglacier.mytool.ui.screen.habit

import cdglacier.mytool.domain.model.Habit
import java.time.LocalDate

data class HabitTrackingUiState(
    val date: LocalDate = LocalDate.now(),
    val habits: List<Habit> = emptyList(),
    val isLoading: Boolean = false,
    val journalConfigured: Boolean = true,
    val journalExists: Boolean = true,
    val errorMessage: String? = null,
)
