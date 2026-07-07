package cdglacier.mytool.ui.screen.household

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.domain.model.Assignee
import cdglacier.mytool.domain.model.HouseholdEntry
import cdglacier.mytool.domain.model.HouseholdPoint
import cdglacier.mytool.domain.model.HouseholdSummary
import cdglacier.mytool.domain.usecase.GetHouseholdPointsUseCase
import cdglacier.mytool.domain.usecase.GetHouseholdSummaryUseCase
import cdglacier.mytool.domain.usecase.RecordHouseholdEntryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HouseholdViewModel @Inject constructor(
    private val obsidianRepository: ObsidianRepository,
    private val getHouseholdPointsUseCase: GetHouseholdPointsUseCase,
    private val recordHouseholdEntryUseCase: RecordHouseholdEntryUseCase,
    private val getHouseholdSummaryUseCase: GetHouseholdSummaryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HouseholdUiState())
    val uiState: StateFlow<HouseholdUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val journalConfigured = obsidianRepository.journalDirUri.first() != null
            val pagesConfigured = obsidianRepository.pagesDirUri.first() != null
            val points = if (pagesConfigured) getHouseholdPointsUseCase() else emptyList()
            val date = _uiState.value.date
            val summary = if (journalConfigured) getHouseholdSummaryUseCase(date..date)
            else HouseholdUiState().summary
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isSummaryLoading = false,
                    journalConfigured = journalConfigured,
                    pagesConfigured = pagesConfigured,
                    points = points,
                    pointsSortedByUsage = sortPointsByUsage(points, summary),
                    summary = summary,
                )
            }
        }
    }

    fun onPrevDate() = shiftDate(-1)
    fun onNextDate() = shiftDate(1)

    private fun shiftDate(days: Long) {
        _uiState.update {
            it.copy(date = it.date.plusDays(days), isSummaryLoading = true, summary = HouseholdUiState().summary)
        }
        refresh()
    }

    fun onOpenRecordDialog() {
        _uiState.update {
            it.copy(
                recordDialog = RecordDialogState(
                    selectedName = it.pointsSortedByUsage.firstOrNull()?.name.orEmpty(),
                    assignee = it.lastAssignee,
                ),
            )
        }
    }

    private fun sortPointsByUsage(
        points: List<HouseholdPoint>,
        summary: HouseholdSummary,
    ): List<HouseholdPoint> {
        val usage = mutableMapOf<String, Int>()
        summary.perAssignee.values.forEach { list ->
            list.forEach { b -> usage[b.name] = (usage[b.name] ?: 0) + b.count }
        }
        return points.withIndex()
            .sortedWith(
                compareByDescending<IndexedValue<HouseholdPoint>> { usage[it.value.name] ?: 0 }
                    .thenBy { it.index }
            )
            .map { it.value }
    }

    fun onCloseRecordDialog() {
        _uiState.update { it.copy(recordDialog = null) }
    }

    fun onRecordNameChange(name: String) {
        _uiState.update { it.copy(recordDialog = it.recordDialog?.copy(selectedName = name)) }
    }

    fun onRecordAssigneeChange(assignee: Assignee) {
        _uiState.update {
            it.copy(
                recordDialog = it.recordDialog?.copy(assignee = assignee),
                lastAssignee = assignee,
            )
        }
    }

    fun onRecordCountChange(text: String) {
        _uiState.update { it.copy(recordDialog = it.recordDialog?.copy(countText = text)) }
    }

    fun onRecordAdjustmentChange(text: String) {
        _uiState.update { it.copy(recordDialog = it.recordDialog?.copy(adjustmentText = text)) }
    }

    fun onSubmitRecord() {
        val dialog = _uiState.value.recordDialog ?: return
        if (dialog.isSubmitting) return
        val name = dialog.selectedName.trim()
        val count = dialog.countText.toIntOrNull()
        val adjustment = dialog.adjustmentText.trim().let {
            if (it.isEmpty()) 0 else it.removePrefix("+").toIntOrNull()
        }
        if (name.isEmpty() || count == null || count <= 0 || adjustment == null) {
            _uiState.update { it.copy(errorMessage = "入力を確認してください") }
            return
        }
        _uiState.update { it.copy(recordDialog = it.recordDialog?.copy(isSubmitting = true)) }
        viewModelScope.launch {
            val entry = HouseholdEntry(name, dialog.assignee, count, adjustment)
            val result = recordHouseholdEntryUseCase(_uiState.value.date, entry)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(
                        recordDialog = it.recordDialog?.copy(isSubmitting = false),
                        errorMessage = result.exceptionOrNull()?.message,
                    )
                }
            } else {
                _uiState.update { state ->
                    state.copy(
                        recordDialog = null,
                        summary = applyOptimistic(state, entry),
                    )
                }
                refresh()
            }
        }
    }

    private fun applyOptimistic(state: HouseholdUiState, entry: HouseholdEntry): HouseholdSummary {
        val base = state.points.firstOrNull { it.name == entry.name }?.points ?: 0
        val delta = base * entry.count + entry.adjustment
        val summary = state.summary
        val existing = summary.perAssignee[entry.assignee].orEmpty()
        val merged = if (existing.any { it.name == entry.name }) {
            existing.map { b ->
                if (b.name == entry.name) b.copy(
                    count = b.count + entry.count,
                    adjustment = b.adjustment + entry.adjustment,
                    effectivePoints = b.effectivePoints + delta,
                ) else b
            }
        } else {
            existing + HouseholdSummary.Breakdown(entry.name, entry.count, entry.adjustment, delta)
        }.sortedByDescending { it.effectivePoints }
        val perAssignee = summary.perAssignee.toMutableMap().apply {
            this[entry.assignee] = merged
        }
        return summary.copy(
            husbandTotal = summary.husbandTotal + if (entry.assignee == Assignee.HUSBAND) delta else 0,
            wifeTotal = summary.wifeTotal + if (entry.assignee == Assignee.WIFE) delta else 0,
            perAssignee = perAssignee,
        )
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
