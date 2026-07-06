package cdglacier.mytool.ui.screen.household

import cdglacier.mytool.domain.model.Assignee
import cdglacier.mytool.domain.model.HouseholdPoint
import cdglacier.mytool.domain.model.HouseholdSummary
import java.time.LocalDate

enum class HouseholdPeriod { WEEK, MONTH }

data class HouseholdUiState(
    val period: HouseholdPeriod = HouseholdPeriod.WEEK,
    val today: LocalDate = LocalDate.now(),
    val isLoading: Boolean = false,
    val journalConfigured: Boolean = true,
    val pagesConfigured: Boolean = true,
    val points: List<HouseholdPoint> = emptyList(),
    val summary: HouseholdSummary = HouseholdSummary(0, 0, emptyMap()),
    val recordDialog: RecordDialogState? = null,
    val pointsDialog: PointsDialogState? = null,
    val errorMessage: String? = null,
)

data class RecordDialogState(
    val selectedName: String = "",
    val assignee: Assignee = Assignee.HUSBAND,
    val countText: String = "1",
    val adjustmentText: String = "0",
)

data class PointsDialogState(
    val points: List<HouseholdPoint> = emptyList(),
    val newName: String = "",
    val newPointsText: String = "",
)
