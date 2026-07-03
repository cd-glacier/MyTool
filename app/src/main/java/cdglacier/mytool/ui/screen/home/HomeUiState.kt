package cdglacier.mytool.ui.screen.home

import android.net.Uri
import cdglacier.mytool.domain.usecase.DailyActivity
import java.time.LocalDate

data class HomeUiState(
    val journalDirUri: Uri? = null,
    val dailyActivities: Map<LocalDate, DailyActivity> = emptyMap(),
    val todayCompletionRate: Float? = null,
    val selectedDate: LocalDate = LocalDate.now(),
    val isLoading: Boolean = false,
)
