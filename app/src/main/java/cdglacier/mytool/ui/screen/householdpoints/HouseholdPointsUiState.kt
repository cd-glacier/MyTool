package cdglacier.mytool.ui.screen.householdpoints

import cdglacier.mytool.domain.model.HouseholdPoint

data class HouseholdPointsUiState(
    val isLoading: Boolean = true,
    val pagesConfigured: Boolean = true,
    val points: List<HouseholdPoint> = emptyList(),
    val newName: String = "",
    val newPointsText: String = "",
    val pendingDeleteName: String? = null,
    val editingName: String? = null,
    val editingPointsText: String = "",
    val errorMessage: String? = null,
)
