package cdglacier.mytool.ui.screen.money

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cdglacier.mytool.domain.model.AnnualService
import cdglacier.mytool.domain.model.MoneyItem
import cdglacier.mytool.domain.model.SavingsItem
import cdglacier.mytool.ui.component.GlacierConfirmDialog
import cdglacier.mytool.ui.component.GlacierSectionCard
import cdglacier.mytool.ui.component.GlacierTopBar
import cdglacier.mytool.ui.theme.GlacierAmber
import cdglacier.mytool.ui.theme.GlacierBg
import cdglacier.mytool.ui.theme.GlacierCyan
import cdglacier.mytool.ui.theme.GlacierMuted
import cdglacier.mytool.ui.theme.GlacierOnPrimary
import cdglacier.mytool.ui.theme.GlacierOnSurface
import cdglacier.mytool.ui.theme.GlacierSurface
import cdglacier.mytool.ui.theme.GlacierSurfaceLow
import cdglacier.mytool.ui.theme.GlacierTeal
import cdglacier.mytool.ui.theme.SpaceGroteskFamily
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val ERROR_RED = Color(0xFFE57373)

@Composable
fun MoneyScreen(
    viewModel: MoneyViewModel = viewModel(),
    onBack: () -> Unit,
    onNavigateChart: (section: String, group: String?) -> Unit = { _, _ -> },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MoneyContent(
        uiState = uiState,
        onBack = onBack,
        onPrevMonth = viewModel::onPrevMonth,
        onNextMonth = viewModel::onNextMonth,
        onSave = viewModel::save,
        onClearError = viewModel::clearError,
        onNavigateChart = onNavigateChart,
        viewModel = viewModel,
    )
}

@Composable
private fun MoneyContent(
    uiState: MoneyUiState,
    onBack: () -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSave: () -> Unit,
    onClearError: () -> Unit,
    onNavigateChart: (String, String?) -> Unit,
    viewModel: MoneyViewModel,
) {
    var pendingDelete by remember { mutableStateOf<PendingDelete?>(null) }
    pendingDelete?.let { pd ->
        GlacierConfirmDialog(
            title = "ARCHIVE_CONFIRM",
            body = "${pd.label} をアーカイブします。よろしいですか？",
            confirmLabel = "ARCHIVE",
            cancelLabel = "CANCEL",
            onConfirm = { pd.action(); pendingDelete = null },
            onCancel = { pendingDelete = null },
        )
    }
    val requestRemove: (String, () -> Unit) -> Unit = { label, action ->
        pendingDelete = PendingDelete(label, action)
    }

    Scaffold(
        topBar = { GlacierTopBar(title = "MONEY_BOOK", onBack = onBack) },
        containerColor = GlacierBg,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!uiState.isVaultConfigured) {
                WarningCard("Vault が未設定です。Settings から設定してください。")
            }
            uiState.saveError?.let {
                WarningCard("SAVE_ERROR: $it", onClick = onClearError)
            }

            MonthHeader(month = uiState.displayedMonth, onPrev = onPrevMonth, onNext = onNextMonth)
            SummaryCard(uiState = uiState)

            EditableMoneyItemSection(
                title = "INCOMES",
                items = uiState.currentMonth.incomes,
                onAdd = { viewModel.addIncome(it) },
                onUpdate = { i, item -> viewModel.updateIncome(i, item) },
                onRequestRemove = { i, name -> requestRemove("INCOME: $name") { viewModel.removeIncome(i) } },
                onTitleClick = { onNavigateChart("INCOMES", null) },
            )
            EditableMoneyItemSection(
                title = "CARD_2M_AGO",
                items = uiState.currentMonth.cardExpenses,
                onAdd = { viewModel.addCard(it) },
                onUpdate = { i, item -> viewModel.updateCard(i, item) },
                onRequestRemove = { i, name -> requestRemove("CARD: $name") { viewModel.removeCard(i) } },
                onTitleClick = { onNavigateChart("CARD", null) },
            )
            EditableBudgetSection(
                groups = uiState.budgetGroups,
                onAdd = { name, tag -> viewModel.addBudget(name, tag) },
                onUpdate = { i, item -> viewModel.updateBudget(i, item) },
                onRequestRemove = { i, name -> requestRemove("BUDGET: $name") { viewModel.removeBudget(i) } },
                onTitleClick = { onNavigateChart("BUDGET", null) },
                onGroupClick = { tag -> onNavigateChart("BUDGET", tag) },
            )
            EditableSavingsSection(
                groups = uiState.savingsGroups,
                onAdd = { name, category -> viewModel.addSavings(name, category) },
                onUpdate = { i, item -> viewModel.updateSavings(i, item) },
                onRequestRemove = { i, name -> requestRemove("SAVINGS: $name") { viewModel.removeSavings(i) } },
                onTitleClick = { onNavigateChart("SAVINGS", null) },
                onGroupClick = { category -> onNavigateChart("SAVINGS", category) },
            )
            ServicesSection(
                month = uiState.displayedMonth,
                services = uiState.activeServices,
                onAdd = { viewModel.addService(it) },
                onUpdate = { i, s -> viewModel.updateService(i, s) },
                onRequestRemove = { i, name -> requestRemove("SERVICE: $name") { viewModel.removeService(i) } },
                onTitleClick = { onNavigateChart("SERVICES", null) },
            )

            HistoryGraphSection(uiState = uiState)

            SaveButton(onSave = onSave)
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArrowButton("<", onPrev)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = month.format(DateTimeFormatter.ofPattern("yyyy-MM")),
            color = GlacierCyan,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            modifier = Modifier.weight(1f),
        )
        ArrowButton(">", onNext)
    }
}

@Composable
private fun ArrowButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(32.dp)
            .height(32.dp)
            .background(GlacierSurface)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = GlacierAmber, fontFamily = FontFamily.Monospace, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SummaryCard(uiState: MoneyUiState) {
    GlacierSectionCard(title = "SUMMARY") {
        SummaryRow("INCOME", uiState.incomeTotal)
        SummaryRow("CARD(2M_AGO)", uiState.cardTotal)
        SummaryRow("BUDGET", uiState.budgetTotal)
        SummaryRow("SAVINGS", uiState.savingsTotal)
        SummaryRow("SERVICES", uiState.servicesMonthlyTotal)
        Spacer(modifier = Modifier.height(8.dp))
        SummaryRow(
            label = "DIFFERENCE",
            amount = uiState.difference,
            highlight = if (uiState.difference < 0) ERROR_RED else GlacierTeal,
        )
        Spacer(modifier = Modifier.height(4.dp))
        SummaryRow(label = "TRANSFER_TO_LIFE_ACCOUNT", amount = uiState.lifeAccountTransfer, highlight = GlacierAmber)
    }
}

@Composable
private fun SummaryRow(label: String, amount: Long, highlight: Color? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = GlacierMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatYen(amount),
            color = highlight ?: GlacierOnSurface,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (highlight != null) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun EditableMoneyItemSection(
    title: String,
    items: List<MoneyItem>,
    onAdd: (String) -> Unit,
    onUpdate: (Int, MoneyItem) -> Unit,
    onRequestRemove: (Int, String) -> Unit,
    onTitleClick: (() -> Unit)? = null,
) {
    GlacierSectionCard(
        title = title,
        trailing = { SectionSubtotal(items.sumOf { it.amount }) },
        onTitleClick = onTitleClick,
    ) {
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.name,
                    color = GlacierOnSurface,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                )
                AmountInput(
                    value = item.amount,
                    onChange = { onUpdate(index, item.copy(amount = it)) },
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "[X]",
                    color = GlacierAmber,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onRequestRemove(index, item.name) },
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        AddItemRow(onAdd = onAdd)
    }
}

@Composable
private fun EditableBudgetSection(
    groups: List<Pair<String, List<IndexedValue<MoneyItem>>>>,
    onAdd: (String, String) -> Unit,
    onUpdate: (Int, MoneyItem) -> Unit,
    onRequestRemove: (Int, String) -> Unit,
    onTitleClick: (() -> Unit)? = null,
    onGroupClick: ((String) -> Unit)? = null,
) {
    GlacierSectionCard(
        title = "BUDGET_ENVELOPES",
        trailing = { SectionSubtotal(groups.sumOf { (_, items) -> items.sumOf { it.value.amount } }) },
        onTitleClick = onTitleClick,
    ) {
        if (groups.isEmpty()) {
            Text(
                text = "(no tags yet)",
                color = GlacierMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
        }
        groups.forEach { (tag, indexed) ->
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (onGroupClick != null) Modifier.clickable { onGroupClick(tag) } else Modifier),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "# $tag",
                    color = GlacierCyan,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatYen(indexed.sumOf { it.value.amount }),
                    color = GlacierCyan,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }
            indexed.forEach { (index, item) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.name,
                        color = GlacierOnSurface,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f),
                    )
                    AmountInput(
                        value = item.amount,
                        onChange = { onUpdate(index, item.copy(amount = it)) },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "[X]",
                        color = GlacierAmber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { onRequestRemove(index, item.name) },
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        AddTaggedItemRow(tagLabel = "TAG:", onAdd = onAdd)
    }
}

@Composable
private fun EditableSavingsSection(
    groups: List<Pair<String, List<IndexedValue<SavingsItem>>>>,
    onAdd: (String, String) -> Unit,
    onUpdate: (Int, SavingsItem) -> Unit,
    onRequestRemove: (Int, String) -> Unit,
    onTitleClick: (() -> Unit)? = null,
    onGroupClick: ((String) -> Unit)? = null,
) {
    GlacierSectionCard(
        title = "SAVINGS",
        trailing = { SectionSubtotal(groups.sumOf { (_, items) -> items.sumOf { it.value.amount } }) },
        onTitleClick = onTitleClick,
    ) {
        if (groups.isEmpty()) {
            Text(
                text = "(no categories yet)",
                color = GlacierMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
        }
        groups.forEach { (category, indexed) ->
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (onGroupClick != null) Modifier.clickable { onGroupClick(category) } else Modifier),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "# $category",
                    color = GlacierCyan,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatYen(indexed.sumOf { it.value.amount }),
                    color = GlacierCyan,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }
            indexed.forEach { (index, item) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.name,
                        color = GlacierOnSurface,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f),
                    )
                    val flagLabel = if (item.toLifeAccount) "[L]" else "[ ]"
                    Text(
                        text = flagLabel,
                        color = if (item.toLifeAccount) GlacierTeal else GlacierMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable {
                                onUpdate(index, item.copy(toLifeAccount = !item.toLifeAccount))
                            },
                    )
                    AmountInput(
                        value = item.amount,
                        onChange = { onUpdate(index, item.copy(amount = it)) },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "[X]",
                        color = GlacierAmber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { onRequestRemove(index, item.name) },
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        AddTaggedItemRow(tagLabel = "CAT:", onAdd = onAdd)
    }
}

@Composable
private fun AddTaggedItemRow(tagLabel: String, onAdd: (String, String) -> Unit) {
    var newName by remember { mutableStateOf("") }
    var newTag by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextInput(value = newName, onChange = { newName = it }, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            val enabled = newName.isNotBlank() && newTag.isNotBlank()
            Text(
                text = "[+ADD]",
                color = if (enabled) GlacierAmber else GlacierMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier.clickable(enabled = enabled) {
                    onAdd(newName.trim(), newTag.trim())
                    newName = ""; newTag = ""
                },
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tagLabel, color = GlacierMuted, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            Spacer(modifier = Modifier.width(4.dp))
            TextInput(value = newTag, onChange = { newTag = it }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ServicesSection(
    month: YearMonth,
    services: List<IndexedValue<AnnualService>>,
    onAdd: (AnnualService) -> Unit,
    onUpdate: (Int, AnnualService) -> Unit,
    onRequestRemove: (Int, String) -> Unit,
    onTitleClick: (() -> Unit)? = null,
) {
    GlacierSectionCard(
        title = "ANNUAL_SERVICES",
        trailing = { SectionSubtotal(services.sumOf { it.value.monthlyAmountFor(month) }) },
        onTitleClick = onTitleClick,
    ) {
        services.forEach { (index, s) ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextInput(
                        value = s.name,
                        onChange = { onUpdate(index, s.copy(name = it)) },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AmountInput(value = s.annualAmount, onChange = { onUpdate(index, s.copy(annualAmount = it)) })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("[X]", color = GlacierAmber, fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                        modifier = Modifier.clickable { onRequestRemove(index, s.name) })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("FROM:", color = GlacierMuted, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    DateInput(value = s.startDate, onChange = { onUpdate(index, s.copy(startDate = it)) })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TO:", color = GlacierMuted, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    DateInput(value = s.endDate, onChange = { onUpdate(index, s.copy(endDate = it)) })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "= ${formatYen(s.monthlyAmountFor(month))}/m",
                        color = GlacierTeal, fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        var newName by remember { mutableStateOf("") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextInput(value = newName, onChange = { newName = it }, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "[+ADD]",
                color = GlacierAmber, fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                modifier = Modifier.clickable(enabled = newName.isNotBlank()) {
                    val today = LocalDate.now()
                    onAdd(AnnualService(newName.trim(), 0L, today, today.plusYears(1)))
                    newName = ""
                },
            )
        }
    }
}

@Composable
private fun AddItemRow(onAdd: (String) -> Unit) {
    var newName by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextInput(value = newName, onChange = { newName = it }, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "[+ADD]",
            color = GlacierAmber, fontFamily = FontFamily.Monospace, fontSize = 12.sp,
            modifier = Modifier.clickable(enabled = newName.isNotBlank()) {
                onAdd(newName.trim()); newName = ""
            },
        )
    }
}

@Composable
private fun AmountInput(value: Long, onChange: (Long) -> Unit) {
    var text by remember(value) { mutableStateOf(if (value == 0L) "" else value.toString()) }
    BasicTextField(
        value = text,
        onValueChange = { new ->
            text = new.filter { it.isDigit() }
            onChange(text.toLongOrNull() ?: 0L)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = TextStyle(color = GlacierOnSurface, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
        cursorBrush = SolidColor(GlacierAmber),
        modifier = Modifier
            .width(100.dp)
            .background(GlacierSurface)
            .padding(horizontal = 6.dp, vertical = 4.dp),
    )
}

@Composable
private fun TextInput(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = TextStyle(color = GlacierOnSurface, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
        cursorBrush = SolidColor(GlacierAmber),
        modifier = modifier
            .background(GlacierSurface)
            .padding(horizontal = 6.dp, vertical = 4.dp),
    )
}

@Composable
private fun DateInput(value: LocalDate, onChange: (LocalDate) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    BasicTextField(
        value = text,
        onValueChange = { new ->
            text = new
            runCatching { LocalDate.parse(new) }.getOrNull()?.let(onChange)
        },
        singleLine = true,
        textStyle = TextStyle(color = GlacierOnSurface, fontFamily = FontFamily.Monospace, fontSize = 11.sp),
        cursorBrush = SolidColor(GlacierAmber),
        modifier = Modifier
            .width(96.dp)
            .background(GlacierSurface)
            .padding(horizontal = 6.dp, vertical = 4.dp),
    )
}

@Composable
private fun HistoryGraphSection(uiState: MoneyUiState) {
    GlacierSectionCard(title = "HISTORY_12M") {
        val months = (0L until 12L).map { uiState.displayedMonth.minusMonths(11 - it) }
        val values = months.map { ym ->
            val m = uiState.book.monthOrEmpty(ym)
            BarValue(
                month = ym,
                income = m.incomeTotal,
                card = m.cardTotal,
                budget = m.budgetTotal,
                savings = m.savingsTotal,
            )
        }
        val max = values.flatMap { listOf(it.income, it.card, it.budget, it.savings) }.maxOrNull()?.coerceAtLeast(1) ?: 1L
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            val barGroupWidth = maxWidth / 12
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween) {
                values.forEach { v ->
                    Column(
                        modifier = Modifier.width(barGroupWidth),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(1.dp),
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                        ) {
                            Bar(v.income, max, GlacierTeal)
                            Bar(v.card, max, GlacierAmber)
                            Bar(v.budget, max, GlacierCyan)
                            Bar(v.savings, max, GlacierOnSurface)
                        }
                        Text(
                            text = v.month.monthValue.toString(),
                            color = GlacierMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LegendDot("INC", GlacierTeal)
            LegendDot("CARD", GlacierAmber)
            LegendDot("BUDGET", GlacierCyan)
            LegendDot("SAV", GlacierOnSurface)
        }
    }
}

@Composable
private fun RowScope.Bar(value: Long, max: Long, color: Color) {
    val ratio = (value.toFloat() / max.toFloat()).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((150 * ratio).dp.coerceAtLeast(1.dp))
                .background(color),
        )
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(8.dp).height(8.dp).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = GlacierMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
    }
}

@Composable
private fun WarningCard(text: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlacierSurfaceLow)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(12.dp),
    ) {
        Text(text, color = ERROR_RED, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    }
}

@Composable
private fun SaveButton(onSave: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlacierAmber)
            .clickable { onSave() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "SAVE >",
            color = GlacierOnPrimary,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            letterSpacing = 2.sp,
        )
    }
}

private data class PendingDelete(val label: String, val action: () -> Unit)

private data class BarValue(
    val month: YearMonth,
    val income: Long,
    val card: Long,
    val budget: Long,
    val savings: Long,
)

@Composable
private fun SectionSubtotal(amount: Long) {
    Text(
        text = formatYen(amount),
        color = GlacierCyan,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
    )
}

private fun formatYen(amount: Long): String {
    val sign = if (amount < 0) "-" else ""
    val abs = kotlin.math.abs(amount)
    return "${sign}¥${"%,d".format(abs)}"
}
