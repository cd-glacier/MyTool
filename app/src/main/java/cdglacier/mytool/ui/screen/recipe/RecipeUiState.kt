package cdglacier.mytool.ui.screen.recipe

import cdglacier.mytool.ui.component.RecipeItemUiModel
import java.time.LocalDate

data class RecipeUiState(
    val isLoading: Boolean = false,
    val sections: List<RecipeSectionUiModel> = emptyList(),
)

data class RecipeSectionUiModel(
    val date: LocalDate,
    val items: List<RecipeItemUiModel>,
)
