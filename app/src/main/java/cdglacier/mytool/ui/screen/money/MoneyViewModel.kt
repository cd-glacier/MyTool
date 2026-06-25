package cdglacier.mytool.ui.screen.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cdglacier.mytool.data.repository.MoneyRepository
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.domain.model.AnnualService
import cdglacier.mytool.domain.model.MoneyBook
import cdglacier.mytool.domain.model.MoneyCategory
import cdglacier.mytool.domain.model.MoneyItem
import cdglacier.mytool.domain.model.MonthlyMoney
import cdglacier.mytool.domain.model.SavingsItem
import cdglacier.mytool.domain.usecase.MoneyCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class MoneyViewModel @Inject constructor(
    private val moneyRepository: MoneyRepository,
    private val obsidianRepository: ObsidianRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoneyUiState())
    val uiState: StateFlow<MoneyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            obsidianRepository.vaultUri.collect { uri ->
                _uiState.update { it.copy(isVaultConfigured = uri != null) }
            }
        }
        viewModelScope.launch {
            moneyRepository.observeBook().collect { book ->
                _uiState.update { state ->
                    val ensured = ensureMonthExists(book, state.displayedMonth)
                    state.copy(book = ensured, isLoading = false)
                }
            }
        }
    }

    fun onPrevMonth() = changeMonth(_uiState.value.displayedMonth.minusMonths(1))
    fun onNextMonth() = changeMonth(_uiState.value.displayedMonth.plusMonths(1))

    private fun changeMonth(month: YearMonth) {
        _uiState.update { state ->
            state.copy(
                book = ensureMonthExists(state.book, month),
                displayedMonth = month,
            )
        }
    }

    private fun ensureMonthExists(book: MoneyBook, month: YearMonth): MoneyBook {
        if (book.months.containsKey(month)) return book
        val carried = MoneyCalculator.carryForwardNames(book, month)
        return book.copy(months = book.months + (month to carried))
    }

    // === 編集 ===

    fun updateIncome(index: Int, item: MoneyItem) = updateList { it.copy(incomes = it.incomes.replaceAt(index, item)) }
    fun addIncome(name: String) = updateList { it.copy(incomes = it.incomes + MoneyItem(name, 0L)) }
    fun removeIncome(index: Int) = archiveAt(MoneyCategory.INCOME, index) { it.incomes }

    fun updateCard(index: Int, item: MoneyItem) = updateList { it.copy(cardExpenses = it.cardExpenses.replaceAt(index, item)) }
    fun addCard(name: String) = updateList { it.copy(cardExpenses = it.cardExpenses + MoneyItem(name, 0L)) }
    fun removeCard(index: Int) = archiveAt(MoneyCategory.CARD, index) { it.cardExpenses }

    fun updateBudget(index: Int, item: MoneyItem) = updateList { it.copy(budgets = it.budgets.replaceAt(index, item)) }
    fun addBudget(name: String) = updateList { it.copy(budgets = it.budgets + MoneyItem(name, 0L)) }
    fun removeBudget(index: Int) = archiveAt(MoneyCategory.BUDGET, index) { it.budgets }

    fun updateSavings(index: Int, item: SavingsItem) = updateList { it.copy(savings = it.savings.replaceAt(index, item)) }
    fun addSavings(name: String) = updateList {
        it.copy(savings = it.savings + SavingsItem(name, 0L, "未分類", false))
    }
    fun removeSavings(index: Int) {
        val state = _uiState.value
        val name = state.currentMonth.savings.getOrNull(index)?.name ?: return
        _uiState.update { s ->
            val archived = s.book.archived.toMutableMap()
            archived[MoneyCategory.SAVINGS] = (archived[MoneyCategory.SAVINGS] ?: emptySet()) + name
            val newMonth = s.currentMonth.copy(savings = s.currentMonth.savings.filterIndexed { i, _ -> i != index })
            s.copy(book = s.book.copy(
                months = s.book.months + (s.displayedMonth to newMonth),
                archived = archived,
            ))
        }
    }

    fun addService(service: AnnualService) {
        _uiState.update { it.copy(book = it.book.copy(services = it.book.services + service)) }
    }
    fun updateService(index: Int, service: AnnualService) {
        _uiState.update {
            it.copy(book = it.book.copy(services = it.book.services.replaceAt(index, service)))
        }
    }
    fun removeService(index: Int) {
        _uiState.update {
            it.copy(book = it.book.copy(services = it.book.services.filterIndexed { i, _ -> i != index }))
        }
    }

    private inline fun updateList(transform: (MonthlyMoney) -> MonthlyMoney) {
        _uiState.update { state ->
            val newMonth = transform(state.currentMonth)
            state.copy(book = state.book.copy(months = state.book.months + (state.displayedMonth to newMonth)))
        }
    }

    private inline fun archiveAt(category: MoneyCategory, index: Int, selector: (MonthlyMoney) -> List<MoneyItem>) {
        val state = _uiState.value
        val name = selector(state.currentMonth).getOrNull(index)?.name ?: return
        _uiState.update { s ->
            val archived = s.book.archived.toMutableMap()
            archived[category] = (archived[category] ?: emptySet()) + name
            val newMonth = when (category) {
                MoneyCategory.INCOME -> s.currentMonth.copy(incomes = s.currentMonth.incomes.filterIndexed { i, _ -> i != index })
                MoneyCategory.CARD -> s.currentMonth.copy(cardExpenses = s.currentMonth.cardExpenses.filterIndexed { i, _ -> i != index })
                MoneyCategory.BUDGET -> s.currentMonth.copy(budgets = s.currentMonth.budgets.filterIndexed { i, _ -> i != index })
                MoneyCategory.SAVINGS -> s.currentMonth
            }
            s.copy(book = s.book.copy(
                months = s.book.months + (s.displayedMonth to newMonth),
                archived = archived,
            ))
        }
    }

    fun save() {
        viewModelScope.launch {
            val book = _uiState.value.book
            val result = moneyRepository.save(book)
            result.exceptionOrNull()?.let { e ->
                _uiState.update { it.copy(saveError = e.message ?: "保存に失敗しました") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(saveError = null) }
    }
}

private fun <T> List<T>.replaceAt(index: Int, item: T): List<T> =
    mapIndexed { i, v -> if (i == index) item else v }
