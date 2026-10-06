package cdglacier.mytool.ui.screen.diary

import cdglacier.mytool.ui.component.DiaryItemUiModel
import java.time.LocalDate

data class DiaryUiState(
    val isLoading: Boolean = false,
    val sections: List<DiarySectionUiModel> = emptyList(),
    val addForm: AddDiaryFormUiModel = AddDiaryFormUiModel(),
)

data class DiarySectionUiModel(
    val date: LocalDate,
    val items: List<DiaryItemUiModel>,
)

data class AddDiaryFormUiModel(
    val content: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
)
