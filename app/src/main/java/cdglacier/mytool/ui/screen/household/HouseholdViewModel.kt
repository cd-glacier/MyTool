package cdglacier.mytool.ui.screen.household

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.domain.model.Assignee
import cdglacier.mytool.domain.model.HouseholdEntry
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
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
            _uiState.update { it.copy(isLoading = true) }
            val journalConfigured = obsidianRepository.journalDirUri.first() != null
            val pagesConfigured = obsidianRepository.pagesDirUri.first() != null
            val points = if (pagesConfigured) getHouseholdPointsUseCase() else emptyList()
            val today = LocalDate.now()
            val range = rangeFor(today, _uiState.value.period)
            val summary = if (journalConfigured) getHouseholdSummaryUseCase(range)
            else HouseholdUiState().summary
            _uiState.update {
                it.copy(
                    isLoading = false,
                    journalConfigured = journalConfigured,
                    pagesConfigured = pagesConfigured,
                    today = today,
                    points = points,
                    summary = summary,
                )
            }
        }
    }

    fun onPeriodChange(period: HouseholdPeriod) {
        _uiState.update { it.copy(period = period) }
        viewModelScope.launch {
            val range = rangeFor(_uiState.value.today, period)
            val summary = runCatching { getHouseholdSummaryUseCase(range) }
                .getOrDefault(HouseholdUiState().summary)
            _uiState.update { it.copy(summary = summary) }
        }
    }

    fun onOpenRecordDialog() {
        _uiState.update {
            it.copy(
                recordDialog = RecordDialogState(
                    selectedName = it.points.firstOrNull()?.name.orEmpty(),
                ),
            )
        }
    }

    fun onCloseRecordDialog() {
        _uiState.update { it.copy(recordDialog = null) }
    }

    fun onRecordNameChange(name: String) {
        _uiState.update { it.copy(recordDialog = it.recordDialog?.copy(selectedName = name)) }
    }

    fun onRecordAssigneeChange(assignee: Assignee) {
        _uiState.update { it.copy(recordDialog = it.recordDialog?.copy(assignee = assignee)) }
    }

    fun onRecordCountChange(text: String) {
        _uiState.update { it.copy(recordDialog = it.recordDialog?.copy(countText = text)) }
    }

    fun onRecordAdjustmentChange(text: String) {
        _uiState.update { it.copy(recordDialog = it.recordDialog?.copy(adjustmentText = text)) }
    }

    fun onSubmitRecord() {
        val dialog = _uiState.value.recordDialog ?: return
        val name = dialog.selectedName.trim()
        val count = dialog.countText.toIntOrNull()
        val adjustment = dialog.adjustmentText.trim().let {
            if (it.isEmpty()) 0 else it.removePrefix("+").toIntOrNull()
        }
        if (name.isEmpty() || count == null || count <= 0 || adjustment == null) {
            _uiState.update { it.copy(errorMessage = "入力を確認してください") }
            return
        }
        viewModelScope.launch {
            val entry = HouseholdEntry(name, dialog.assignee, count, adjustment)
            val result = recordHouseholdEntryUseCase(_uiState.value.today, entry)
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
            } else {
                _uiState.update { it.copy(recordDialog = null) }
                refresh()
            }
        }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun rangeFor(today: LocalDate, period: HouseholdPeriod): ClosedRange<LocalDate> =
        when (period) {
            HouseholdPeriod.WEEK -> {
                val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                start..start.plusDays(6)
            }
            HouseholdPeriod.MONTH -> {
                val start = today.withDayOfMonth(1)
                start..today.with(TemporalAdjusters.lastDayOfMonth())
            }
        }
}
